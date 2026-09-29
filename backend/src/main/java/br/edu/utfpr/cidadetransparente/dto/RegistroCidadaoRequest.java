package br.edu.utfpr.cidadetransparente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

/**
 * Único ponto em que o municipioId vem do cliente: quem se registra ainda não tem token
 * e não lê dado de ninguém, só escolhe a qual prefeitura vai se manifestar.
 */
public record RegistroCidadaoRequest(
        @NotNull Long municipioId,
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 150) String email,
        // BCrypt só considera os primeiros 72 bytes
        @NotBlank @Size(min = 8, max = 72) String senha,
        // Opcional no cadastro; aceita com ou sem pontuação e valida os dígitos verificadores
        @CPF String cpf,
        @Size(max = 20) String telefone) {
}
