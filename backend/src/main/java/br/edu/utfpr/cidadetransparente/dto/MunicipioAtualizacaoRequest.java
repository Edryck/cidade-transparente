package br.edu.utfpr.cidadetransparente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** O código IBGE fica de fora: ele compõe os protocolos já emitidos e não pode mudar. */
public record MunicipioAtualizacaoRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "deve ser a sigla da UF em maiúsculas") String uf,
        @NotNull Boolean ativo) {
}
