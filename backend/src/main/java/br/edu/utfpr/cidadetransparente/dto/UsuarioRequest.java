package br.edu.utfpr.cidadetransparente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Criação e atualização de servidores da prefeitura. {@code senha} é obrigatória só na criação (conferido
 * no service); na atualização, nula mantém a atual. {@code ativo} nulo cria ativo e, na atualização, mantém.
 */
public record UsuarioRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(min = 8, max = 72) String senha,
        @NotBlank String perfil,
        Long secretariaId,
        Boolean ativo) {
}
