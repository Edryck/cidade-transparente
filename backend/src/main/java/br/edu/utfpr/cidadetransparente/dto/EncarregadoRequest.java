package br.edu.utfpr.cidadetransparente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Encarregado pelo tratamento de dados pessoais (LGPD art. 41). O e-mail é o canal dos titulares. */
public record EncarregadoRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 150) String email) {
}
