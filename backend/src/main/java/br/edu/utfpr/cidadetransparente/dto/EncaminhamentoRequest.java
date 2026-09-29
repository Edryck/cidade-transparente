package br.edu.utfpr.cidadetransparente.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EncaminhamentoRequest(
        @NotNull Long secretariaId,
        @Size(max = 1000) String observacao) {
}
