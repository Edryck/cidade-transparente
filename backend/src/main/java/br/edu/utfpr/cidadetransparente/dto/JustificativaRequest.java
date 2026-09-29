package br.edu.utfpr.cidadetransparente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Prorrogação, arquivamento e recurso: todos exigem justificativa escrita (Lei 13.460, art. 16;
 * Lei 12.527, art. 11, § 2º).
 */
public record JustificativaRequest(
        @NotBlank @Size(max = 2000) String justificativa) {
}
