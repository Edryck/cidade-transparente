package br.edu.utfpr.cidadetransparente.dto;

import java.util.List;

/**
 * Resposta ao pedido de encerramento de conta. Quando os dados não podem ser eliminados, a LGPD exige
 * informar as razões de fato e de direito (art. 18, § 4º, II).
 */
public record EncerramentoContaResponse(String mensagem, List<String> fundamentacao) {
}
