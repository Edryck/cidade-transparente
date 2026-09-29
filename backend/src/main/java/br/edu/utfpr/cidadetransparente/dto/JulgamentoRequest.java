package br.edu.utfpr.cidadetransparente.dto;

import br.edu.utfpr.cidadetransparente.domain.StatusRecurso;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Julgamento do recurso (Lei 12.527, art. 15, parágrafo único). Deferido, a autoridade fixa até quando a
 * secretaria deve entregar a nova resposta ({@code prazoCumprimento}, obrigatório nesse caso).
 */
public record JulgamentoRequest(
        @NotNull StatusRecurso resultado,
        @NotBlank @Size(max = 5000) String decisao,
        LocalDate prazoCumprimento) {
}
