package br.edu.utfpr.cidadetransparente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Abertura de manifestação. {@code municipioId} só é lido na manifestação anônima (sem token): logado, o
 * município vem sempre do token e o valor enviado é ignorado.
 */
public record ManifestacaoRequest(
        Long municipioId,
        @NotNull Long tipoId,
        @NotBlank @Size(max = 200) String assunto,
        @NotBlank @Size(max = 5000) String descricao) {
}
