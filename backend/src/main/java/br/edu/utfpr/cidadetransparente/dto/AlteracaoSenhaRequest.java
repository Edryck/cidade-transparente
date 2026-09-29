package br.edu.utfpr.cidadetransparente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A senha atual é exigida: um token esquecido aberto não basta para tomar a conta. */
public record AlteracaoSenhaRequest(
        @NotBlank String senhaAtual,
        // BCrypt só considera os primeiros 72 bytes
        @NotBlank @Size(min = 8, max = 72) String novaSenha) {
}
