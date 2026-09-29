package br.edu.utfpr.cidadetransparente.service;

import br.edu.utfpr.cidadetransparente.config.UsuarioAutenticado;
import br.edu.utfpr.cidadetransparente.domain.Secretaria;
import br.edu.utfpr.cidadetransparente.dto.SecretariaRequest;
import br.edu.utfpr.cidadetransparente.dto.SecretariaResponse;
import br.edu.utfpr.cidadetransparente.exception.ApiException;
import br.edu.utfpr.cidadetransparente.repository.MunicipioRepository;
import br.edu.utfpr.cidadetransparente.repository.SecretariaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SecretariaService {

    private final SecretariaRepository secretariaRepository;
    private final MunicipioRepository municipioRepository;

    @Transactional(readOnly = true)
    public List<SecretariaResponse> listar() {
        return secretariaRepository.findByMunicipioIdOrderByNomeAsc(UsuarioAutenticado.atual().municipioId()).stream()
                .map(SecretariaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public SecretariaResponse buscar(Long id) {
        return SecretariaResponse.de(buscarNoMunicipio(id));
    }

    @Transactional
    public SecretariaResponse criar(SecretariaRequest request) {
        Long municipioId = UsuarioAutenticado.atual().municipioId();
        String sigla = normalizarSigla(request.sigla());
        if (secretariaRepository.existsByMunicipioIdAndSigla(municipioId, sigla)) {
            throw ApiException.conflito("Já existe uma secretaria com a sigla " + sigla + " neste município");
        }
        Secretaria secretaria = new Secretaria();
        secretaria.setMunicipio(municipioRepository.getReferenceById(municipioId));
        preencher(secretaria, request, sigla);
        return SecretariaResponse.de(secretariaRepository.save(secretaria));
    }

    @Transactional
    public SecretariaResponse atualizar(Long id, SecretariaRequest request) {
        Secretaria secretaria = buscarNoMunicipio(id);
        String sigla = normalizarSigla(request.sigla());
        if (secretariaRepository.existsByMunicipioIdAndSiglaAndIdNot(secretaria.getMunicipio().getId(), sigla, id)) {
            throw ApiException.conflito("Já existe uma secretaria com a sigla " + sigla + " neste município");
        }
        preencher(secretaria, request, sigla);
        return SecretariaResponse.de(secretaria);
    }

    /** DELETE desativa em vez de apagar: manifestações e respostas antigas continuam apontando para ela. */
    @Transactional
    public void desativar(Long id) {
        buscarNoMunicipio(id).setAtiva(false);
    }

    private Secretaria buscarNoMunicipio(Long id) {
        // Secretaria de outro município responde 404, igual a inexistente: não confirma que o id existe
        return secretariaRepository.findByIdAndMunicipioId(id, UsuarioAutenticado.atual().municipioId())
                .orElseThrow(() -> ApiException.naoEncontrado("Secretaria não encontrada"));
    }

    private static void preencher(Secretaria secretaria, SecretariaRequest request, String sigla) {
        secretaria.setNome(request.nome().trim());
        secretaria.setSigla(sigla);
        secretaria.setEmail(request.email());
        if (request.ativa() != null) {
            secretaria.setAtiva(request.ativa());
        }
    }

    private static String normalizarSigla(String sigla) {
        return sigla.trim().toUpperCase(Locale.ROOT);
    }
}
