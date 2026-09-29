package br.edu.utfpr.cidadetransparente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Feriado estadual, municipal ou ponto facultativo sem expediente. {@code anual} nulo vale false: feriados
 * móveis (Sexta-Feira da Paixão, Corpus Christi) são cadastrados ano a ano.
 */
public record FeriadoRequest(
        @NotNull LocalDate data,
        @NotBlank @Size(max = 100) String descricao,
        Boolean anual) {
}
