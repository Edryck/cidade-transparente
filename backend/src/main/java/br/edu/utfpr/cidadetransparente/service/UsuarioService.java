package br.edu.utfpr.cidadetransparente.service;

import br.edu.utfpr.cidadetransparente.config.UsuarioAutenticado;
import br.edu.utfpr.cidadetransparente.domain.Municipio;
import br.edu.utfpr.cidadetransparente.domain.Perfil;
import br.edu.utfpr.cidadetransparente.domain.Secretaria;
import br.edu.utfpr.cidadetransparente.domain.Usuario;
import br.edu.utfpr.cidadetransparente.dto.UsuarioRequest;
import br.edu.utfpr.cidadetransparente.dto.UsuarioResponse;
import br.edu.utfpr.cidadetransparente.exception.ApiException;
import br.edu.utfpr.cidadetransparente.repository.MunicipioRepository;
import br.edu.utfpr.cidadetransparente.repository.PerfilRepository;
import br.edu.utfpr.cidadetransparente.repository.SecretariaRepository;
import br.edu.utfpr.cidadetransparente.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    /** O ADMIN só gerencia a equipe da prefeitura; conta de cidadão é criada e mantida pelo próprio cidadão. */
    private static final Set<String> PERFIS_DA_EQUIPE = Set.of(Perfil.ADMIN, Perfil.OUVIDOR, Perfil.SERVIDOR);

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final SecretariaRepository secretariaRepository;
    private final MunicipioRepository municipioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Ponto único de criação de conta (registro de cidadão, primeiro ADMIN do município e CRUD de usuários):
     * normaliza o e-mail, garante que ele é único na plataforma e grava a senha só como hash BCrypt.
     */
    @Transactional
    public Usuario criarConta(String nome, String email, String senha, String perfil,
                              Municipio municipio, Secretaria secretaria) {
        String emailNormalizado = normalizarEmail(email);
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            throw ApiException.conflito("Já existe uma conta com este e-mail");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(nome.trim());
        usuario.setEmail(emailNormalizado);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setPerfil(buscarPerfil(perfil));
        usuario.setMunicipio(municipio);
        usuario.setSecretaria(secretaria);
        return usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(String perfil) {
        Long municipioId = UsuarioAutenticado.atual().municipioId();
        List<Usuario> usuarios = perfil == null
                ? usuarioRepository.findByMunicipioIdOrderByNomeAsc(municipioId)
                : usuarioRepository.findByMunicipioIdAndPerfilNomeOrderByNomeAsc(municipioId, perfil.toUpperCase(Locale.ROOT));
        return usuarios.stream().map(UsuarioResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        return UsuarioResponse.de(buscarNoMunicipio(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request) {
        if (request.senha() == null) {
            throw ApiException.requisicaoInvalida("Senha é obrigatória na criação do usuário");
        }
        Long municipioId = UsuarioAutenticado.atual().municipioId();
        String perfil = validarPerfilDaEquipe(request.perfil());
        Secretaria secretaria = resolverSecretaria(perfil, request.secretariaId(), municipioId);

        Usuario usuario = criarConta(request.nome(), request.email(), request.senha(), perfil,
                municipioRepository.getReferenceById(municipioId), secretaria);
        if (Boolean.FALSE.equals(request.ativo())) {
            usuario.setAtivo(false);
        }
        return UsuarioResponse.de(usuario);
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioRequest request) {
        UsuarioAutenticado logado = UsuarioAutenticado.atual();
        Usuario usuario = buscarDaEquipe(id);
        String perfil = validarPerfilDaEquipe(request.perfil());
        boolean ativo = request.ativo() == null ? usuario.isAtivo() : request.ativo();

        // Impede o ADMIN de se trancar para fora: sem isso, o município poderia ficar sem nenhum gestor
        if (usuario.getId().equals(logado.id()) && (!Perfil.ADMIN.equals(perfil) || !ativo)) {
            throw ApiException.conflito("Você não pode remover o próprio perfil ADMIN nem desativar a própria conta");
        }

        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw ApiException.conflito("Já existe uma conta com este e-mail");
        }

        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setPerfil(buscarPerfil(perfil));
        usuario.setSecretaria(resolverSecretaria(perfil, request.secretariaId(), logado.municipioId()));
        usuario.setAtivo(ativo);
        if (request.senha() != null) {
            usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        }
        return UsuarioResponse.de(usuario);
    }

    /** DELETE desativa em vez de apagar: o usuário continua referenciado em trâmites e respostas. */
    @Transactional
    public void desativar(Long id) {
        Usuario usuario = buscarDaEquipe(id);
        if (usuario.getId().equals(UsuarioAutenticado.atual().id())) {
            throw ApiException.conflito("Você não pode desativar a própria conta");
        }
        usuario.setAtivo(false);
    }

    private Usuario buscarNoMunicipio(Long id) {
        // Usuário de outro município responde 404, igual a inexistente: não confirma que o id existe
        return usuarioRepository.findByIdAndMunicipioId(id, UsuarioAutenticado.atual().municipioId())
                .orElseThrow(() -> ApiException.naoEncontrado("Usuário não encontrado"));
    }

    private Usuario buscarDaEquipe(Long id) {
        Usuario usuario = buscarNoMunicipio(id);
        if (!PERFIS_DA_EQUIPE.contains(usuario.getPerfil().getNome())) {
            throw ApiException.proibido("Conta de cidadão só pode ser alterada pelo próprio cidadão");
        }
        return usuario;
    }

    private String validarPerfilDaEquipe(String perfil) {
        String normalizado = perfil.trim().toUpperCase(Locale.ROOT);
        if (!PERFIS_DA_EQUIPE.contains(normalizado)) {
            throw ApiException.requisicaoInvalida("Perfil deve ser ADMIN, OUVIDOR ou SERVIDOR");
        }
        return normalizado;
    }

    /** SERVIDOR precisa de uma secretaria ativa do próprio município; os demais perfis não têm secretaria. */
    private Secretaria resolverSecretaria(String perfil, Long secretariaId, Long municipioId) {
        if (!Perfil.SERVIDOR.equals(perfil)) {
            if (secretariaId != null) {
                throw ApiException.requisicaoInvalida("Só usuários SERVIDOR são vinculados a uma secretaria");
            }
            return null;
        }
        if (secretariaId == null) {
            throw ApiException.requisicaoInvalida("Usuário SERVIDOR precisa de secretariaId");
        }
        return secretariaRepository.findByIdAndMunicipioId(secretariaId, municipioId)
                .filter(Secretaria::isAtiva)
                .orElseThrow(() -> ApiException.requisicaoInvalida("Secretaria inexistente ou desativada"));
    }

    private Perfil buscarPerfil(String nome) {
        return perfilRepository.findByNome(nome)
                .orElseThrow(() -> new IllegalStateException("Perfil " + nome + " ausente: migration V5 não aplicada"));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
