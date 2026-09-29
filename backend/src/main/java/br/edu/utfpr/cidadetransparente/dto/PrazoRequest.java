package br.edu.utfpr.cidadetransparente.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Regra municipal de prazo. A coerência com a regra federal é conferida no TipoManifestacaoService. */
public record PrazoRequest(
        @NotNull @Positive Integer diasResposta,
        @NotNull Boolean permiteProrrogacao,
        @Positive Integer diasProrrogacao,
        @Positive Integer diasInterposicaoRecurso,
        @Positive Integer diasJulgamentoRecurso) {
}
