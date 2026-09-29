package br.edu.utfpr.cidadetransparente.service;

import br.edu.utfpr.cidadetransparente.config.UsuarioAutenticado;
import br.edu.utfpr.cidadetransparente.domain.PrazoLegal;
import br.edu.utfpr.cidadetransparente.domain.TipoManifestacao;
import br.edu.utfpr.cidadetransparente.dto.PrazoRequest;
import br.edu.utfpr.cidadetransparente.dto.TipoManifestacaoResponse;
import br.edu.utfpr.cidadetransparente.exception.ApiException;
import br.edu.utfpr.cidadetransparente.repository.MunicipioRepository;
import br.edu.utfpr.cidadetransparente.repository.PrazoLegalRepository;
import br.edu.utfpr.cidadetransparente.repository.TipoManifestacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tipos e regras de prazo. A regra federal (municipio nulo) vale para todos; o município pode ter uma regra
 * própria por tipo (art. 45 da LAI), desde que respeite as normas gerais: pode ser mais protetiva ao
 * cidadão, nunca menos.
 */
@Service
@RequiredArgsConstructor
public class TipoManifestacaoService {

    private final TipoManifestacaoRepository tipoRepository;
    private final PrazoLegalRepository prazoRepository;
    private final MunicipioRepository municipioRepository;

    @Transactional(readOnly = true)
    public List<TipoManifestacaoResponse> listar() {
        Long municipioId = UsuarioAutenticado.atual().municipioId();
        Map<Long, PrazoLegal> federais = porTipo(prazoRepository.findByMunicipioIsNull());
        Map<Long, PrazoLegal> municipais = porTipo(prazoRepository.findByMunicipioId(municipioId));
        return tipoRepository.findAllByOrderByIdAsc().stream()
                .map(t -> TipoManifestacaoResponse.de(t, municipais.getOrDefault(t.getId(), federais.get(t.getId()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public TipoManifestacaoResponse buscar(Long id) {
        TipoManifestacao tipo = buscarTipo(id);
        return TipoManifestacaoResponse.de(tipo, prazoVigente(id, UsuarioAutenticado.atual().municipioId()));
    }

    /** Regra que vale para o município: a própria, se houver; senão, a federal. */
    @Transactional(readOnly = true)
    public PrazoLegal prazoVigente(Long tipoId, Long municipioId) {
        return prazoRepository.findByTipoManifestacaoIdAndMunicipioId(tipoId, municipioId)
                .or(() -> prazoRepository.findByTipoManifestacaoIdAndMunicipioIsNull(tipoId))
                .orElseThrow(() -> new IllegalStateException("Tipo " + tipoId + " sem regra federal de prazo"));
    }

    /** Cria ou substitui a regra municipal do tipo. Manifestações já abertas mantêm a data limite calculada. */
    @Transactional
    public TipoManifestacaoResponse definirPrazoMunicipal(Long tipoId, PrazoRequest request) {
        Long municipioId = UsuarioAutenticado.atual().municipioId();
        TipoManifestacao tipo = buscarTipo(tipoId);
        PrazoLegal federal = prazoRepository.findByTipoManifestacaoIdAndMunicipioIsNull(tipoId)
                .orElseThrow(() -> new IllegalStateException("Tipo " + tipoId + " sem regra federal de prazo"));
        validarContraRegraFederal(tipo, federal, request);

        PrazoLegal municipal = prazoRepository.findByTipoManifestacaoIdAndMunicipioId(tipoId, municipioId)
                .orElseGet(() -> {
                    PrazoLegal novo = new PrazoLegal();
                    novo.setTipoManifestacao(tipo);
                    novo.setMunicipio(municipioRepository.getReferenceById(municipioId));
                    return novo;
                });
        municipal.setDiasResposta(request.diasResposta());
        municipal.setPermiteProrrogacao(request.permiteProrrogacao());
        municipal.setDiasProrrogacao(request.permiteProrrogacao() ? request.diasProrrogacao() : null);
        municipal.setDiasInterposicaoRecurso(request.diasInterposicaoRecurso());
        municipal.setDiasJulgamentoRecurso(request.diasJulgamentoRecurso());
        return TipoManifestacaoResponse.de(tipo, prazoRepository.save(municipal));
    }

    /** Volta o município para a regra federal. Idempotente: sem regra municipal, não faz nada. */
    @Transactional
    public void removerPrazoMunicipal(Long tipoId) {
        buscarTipo(tipoId);
        prazoRepository.findByTipoManifestacaoIdAndMunicipioId(tipoId, UsuarioAutenticado.atual().municipioId())
                .ifPresent(prazoRepository::delete);
    }

    /**
     * Prazos que a administração cumpre (resposta, prorrogação, julgamento) só podem ser iguais ou menores que
     * os federais; o prazo que o cidadão tem para recorrer só pode ser igual ou maior.
     */
    private static void validarContraRegraFederal(TipoManifestacao tipo, PrazoLegal federal, PrazoRequest r) {
        if (r.diasResposta() > federal.getDiasResposta()) {
            throw ApiException.requisicaoInvalida(
                    "Prazo de resposta não pode passar do federal (" + federal.getDiasResposta() + " dias)");
        }
        if (r.permiteProrrogacao()) {
            if (!federal.isPermiteProrrogacao()) {
                throw ApiException.requisicaoInvalida("A lei federal não permite prorrogação para este tipo");
            }
            if (r.diasProrrogacao() == null) {
                throw ApiException.requisicaoInvalida("Informe diasProrrogacao quando permiteProrrogacao for true");
            }
            if (r.diasProrrogacao() > federal.getDiasProrrogacao()) {
                throw ApiException.requisicaoInvalida(
                        "Prorrogação não pode passar da federal (" + federal.getDiasProrrogacao() + " dias)");
            }
        } else if (r.diasProrrogacao() != null) {
            throw ApiException.requisicaoInvalida("diasProrrogacao deve ser nulo quando permiteProrrogacao for false");
        }

        boolean informouRecurso = r.diasInterposicaoRecurso() != null || r.diasJulgamentoRecurso() != null;
        if (!tipo.isPermiteRecurso()) {
            if (informouRecurso) {
                throw ApiException.requisicaoInvalida("Este tipo não tem fase recursal: não informe prazos de recurso");
            }
            return;
        }
        // O recurso é direito garantido pela LAI: o município não pode suprimir a fase recursal
        if (r.diasInterposicaoRecurso() == null || r.diasJulgamentoRecurso() == null) {
            throw ApiException.requisicaoInvalida("Informe diasInterposicaoRecurso e diasJulgamentoRecurso");
        }
        if (r.diasInterposicaoRecurso() < federal.getDiasInterposicaoRecurso()) {
            throw ApiException.requisicaoInvalida("Prazo para o cidadão recorrer não pode ser menor que o federal ("
                    + federal.getDiasInterposicaoRecurso() + " dias)");
        }
        if (r.diasJulgamentoRecurso() > federal.getDiasJulgamentoRecurso()) {
            throw ApiException.requisicaoInvalida("Prazo de julgamento do recurso não pode passar do federal ("
                    + federal.getDiasJulgamentoRecurso() + " dias)");
        }
    }

    private TipoManifestacao buscarTipo(Long id) {
        return tipoRepository.findById(id)
                .orElseThrow(() -> ApiException.naoEncontrado("Tipo de manifestação não encontrado"));
    }

    private static Map<Long, PrazoLegal> porTipo(List<PrazoLegal> prazos) {
        return prazos.stream().collect(Collectors.toMap(p -> p.getTipoManifestacao().getId(), Function.identity()));
    }
}
