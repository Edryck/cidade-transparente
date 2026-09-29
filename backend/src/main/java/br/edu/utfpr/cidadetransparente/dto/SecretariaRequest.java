package br.edu.utfpr.cidadetransparente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code ativa} é opcional: nulo cria ativa e, na atualização, mantém como está. */
public record SecretariaRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Size(max = 20) String sigla,
        @Email @Size(max = 150) String email,
        Boolean ativa) {
}
