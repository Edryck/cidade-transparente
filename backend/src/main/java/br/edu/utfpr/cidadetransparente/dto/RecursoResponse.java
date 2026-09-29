package br.edu.utfpr.cidadetransparente.dto;

import br.edu.utfpr.cidadetransparente.domain.Recurso;
import br.edu.utfpr.cidadetransparente.domain.StatusRecurso;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RecursoResponse(Long id, Long respostaId, String justificativa, StatusRecurso status,
                              OffsetDateTime interpostoEm, LocalDate dataLimiteJulgamento, String decisao,
                              OffsetDateTime julgadoEm) {

    public static RecursoResponse de(Recurso r) {
        return new RecursoResponse(r.getId(), r.getResposta().getId(), r.getJustificativa(), r.getStatus(),
                r.getInterpostoEm(), r.getDataLimiteJulgamento(), r.getDecisao(), r.getJulgadoEm());
    }
}
