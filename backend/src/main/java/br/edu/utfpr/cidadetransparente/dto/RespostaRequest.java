package br.edu.utfpr.cidadetransparente.dto;

import br.edu.utfpr.cidadetransparente.domain.ResultadoLai;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code resultado} é obrigatório em pedido LAI e proibido nos tipos de ouvidoria (conferido no service). */
public record RespostaRequest(
        @NotBlank @Size(max = 10000) String texto,
        ResultadoLai resultado) {
}
