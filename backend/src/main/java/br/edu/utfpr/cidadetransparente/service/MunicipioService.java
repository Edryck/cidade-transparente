package br.edu.utfpr.cidadetransparente.service;

import br.edu.utfpr.cidadetransparente.domain.Municipio;
import br.edu.utfpr.cidadetransparente.domain.Perfil;
import br.edu.utfpr.cidadetransparente.dto.MunicipioAtualizacaoRequest;
import br.edu.utfpr.cidadetransparente.dto.MunicipioRequest;
import br.edu.utfpr.cidadetransparente.dto.MunicipioResponse;
import br.edu.utfpr.cidadetransparente.dto.MunicipioResumo;
import br.edu.utfpr.cidadetransparente.exception.ApiException;
import br.edu.utfpr.cidadetransparente.repository.MunicipioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MunicipioService {

    private final MunicipioRepository municipioRepository;
    private final UsuarioService usuarioService;

    @Transactional(readOnly = true)
    public List<MunicipioResumo> listarAtivos() {
        return municipioRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(MunicipioResumo::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MunicipioResponse> listar() {
        return municipioRepository.findAllByOrderByNomeAsc().stream()
                .map(MunicipioResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public MunicipioResponse buscar(Long id) {
        return MunicipioResponse.de(buscarEntidade(id));
    }

    /** Município e primeiro ADMIN na mesma transação: se o e-mail do admin já existir, nada é criado. */
    @Transactional
    public MunicipioResponse criar(MunicipioRequest request) {
        if (municipioRepository.existsByCodigoIbge(request.codigoIbge())) {
            throw ApiException.conflito("Já existe um município com este código IBGE");
        }
        Municipio municipio = new Municipio();
        municipio.setNome(request.nome().trim());
        municipio.setUf(request.uf());
        municipio.setCodigoIbge(request.codigoIbge());
        municipio.setEncarregadoNome(request.encarregado().nome().trim());
        municipio.setEncarregadoEmail(request.encarregado().email().trim().toLowerCase(Locale.ROOT));
        municipioRepository.save(municipio);

        var admin = request.admin();
        usuarioService.criarConta(admin.nome(), admin.email(), admin.senha(), Perfil.ADMIN, municipio, null);
        return MunicipioResponse.de(municipio);
    }

    @Transactional
    public MunicipioResponse atualizar(Long id, MunicipioAtualizacaoRequest request) {
        Municipio municipio = buscarEntidade(id);
        municipio.setNome(request.nome().trim());
        municipio.setUf(request.uf());
        // Desativar bloqueia novos logins de todos os usuários do município (conferido no AuthService)
        municipio.setAtivo(request.ativo());
        return MunicipioResponse.de(municipio);
    }

    private Municipio buscarEntidade(Long id) {
        return municipioRepository.findById(id)
                .orElseThrow(() -> ApiException.naoEncontrado("Município não encontrado"));
    }
}
