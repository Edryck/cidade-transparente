package br.edu.utfpr.cidadetransparente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

/** Correção dos próprios dados (LGPD art. 18, III). CPF e telefone só se aplicam a contas de cidadão. */
public record MinhaContaRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 150) String email,
        @CPF String cpf,
        @Size(max = 20) String telefone) {
}
