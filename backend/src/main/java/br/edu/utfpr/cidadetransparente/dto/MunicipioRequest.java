package br.edu.utfpr.cidadetransparente.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Cadastro de prefeitura cliente: nasce junto com o primeiro ADMIN, então nunca existe município sem gestor. */
public record MunicipioRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Pattern(regexp = "[A-Z]{2}", message = "deve ser a sigla da UF em maiúsculas") String uf,
        @NotBlank @Pattern(regexp = "\\d{7}", message = "deve ter os 7 dígitos do código IBGE") String codigoIbge,
        @NotNull @Valid AdminInicial admin) {

    public record AdminInicial(
            @NotBlank @Size(max = 150) String nome,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72) String senha) {
    }
}
