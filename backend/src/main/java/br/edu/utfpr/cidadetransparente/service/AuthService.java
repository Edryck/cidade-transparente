package br.edu.utfpr.cidadetransparente.service;

import br.edu.utfpr.cidadetransparente.config.JwtService;
import br.edu.utfpr.cidadetransparente.domain.Cidadao;
import br.edu.utfpr.cidadetransparente.domain.Municipio;
import br.edu.utfpr.cidadetransparente.domain.Perfil;
import br.edu.utfpr.cidadetransparente.domain.Usuario;
import br.edu.utfpr.cidadetransparente.dto.LoginRequest;
import br.edu.utfpr.cidadetransparente.dto.LoginResponse;
import br.edu.utfpr.cidadetransparente.dto.RegistroCidadaoRequest;
import br.edu.utfpr.cidadetransparente.exception.ApiException;
import br.edu.utfpr.cidadetransparente.repository.CidadaoRepository;
import br.edu.utfpr.cidadetransparente.repository.MunicipioRepository;
import br.edu.utfpr.cidadetransparente.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final MunicipioRepository municipioRepository;
    private final CidadaoRepository cidadaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Mesma mensagem para e-mail inexistente e senha errada: não revela quais e-mails têm conta
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(u -> passwordEncoder.matches(request.senha(), u.getSenhaHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos"));

        boolean municipioInativo = usuario.getMunicipio() != null && !usuario.getMunicipio().isAtivo();
        if (!usuario.isAtivo() || municipioInativo) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Usuário ou município desativado");
        }
        return gerarResposta(usuario);
    }

    @Transactional
    public LoginResponse registrarCidadao(RegistroCidadaoRequest request) {
        Municipio municipio = municipioRepository.findById(request.municipioId())
                .filter(Municipio::isAtivo)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Município inexistente ou desativado"));

        String cpf = request.cpf() == null ? null : request.cpf().replaceAll("\\D", "");
        if (cpf != null && cidadaoRepository.existsByMunicipioIdAndCpf(municipio.getId(), cpf)) {
            throw ApiException.conflito("Já existe um cidadão com este CPF neste município");
        }

        Usuario usuario = usuarioService.criarConta(request.nome(), request.email(), request.senha(),
                Perfil.CIDADAO, municipio, null);

        Cidadao cidadao = new Cidadao();
        cidadao.setMunicipio(municipio);
        cidadao.setUsuario(usuario);
        cidadao.setNome(usuario.getNome());
        cidadao.setEmail(usuario.getEmail());
        cidadao.setCpf(cpf);
        cidadao.setTelefone(request.telefone());
        cidadaoRepository.save(cidadao);

        return gerarResposta(usuario);
    }

    private LoginResponse gerarResposta(Usuario usuario) {
        return new LoginResponse(
                jwtService.gerarToken(usuario),
                "Bearer",
                jwtService.getExpiracaoMs() / 1000,
                usuario.getId(),
                usuario.getNome(),
                usuario.getPerfil().getNome(),
                usuario.getMunicipio() == null ? null : usuario.getMunicipio().getId());
    }
}
