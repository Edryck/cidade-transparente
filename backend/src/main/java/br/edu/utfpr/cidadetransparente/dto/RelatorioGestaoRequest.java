package br.edu.utfpr.cidadetransparente.dto;

import jakarta.validation.constraints.Size;

/** Parte escrita pela ouvidoria (Lei 13.460, art. 15, III e IV). Obrigatória só na publicação. */
public record RelatorioGestaoRequest(
        @Size(max = 20000) String analisePontosRecorrentes,
        @Size(max = 20000) String providenciasAdotadas) {
}
