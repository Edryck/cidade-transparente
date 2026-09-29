package br.edu.utfpr.cidadetransparente.dto;

import java.time.OffsetDateTime;

/**
 * Confirmação de tratamento e acesso aos próprios dados (LGPD art. 18, I e II).
 * {@code cpf} e {@code telefone} só existem para contas de cidadão.
 */
public record MinhaContaResponse(Long id, String nome, String email, String perfil, Long municipioId,
                                 String municipioNome, boolean ativo, OffsetDateTime criadoEm,
                                 String cpf, String telefone) {
}
