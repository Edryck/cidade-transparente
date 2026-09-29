package br.edu.utfpr.cidadetransparente.dto;

import br.edu.utfpr.cidadetransparente.domain.CategoriaManifestacao;
import br.edu.utfpr.cidadetransparente.domain.StatusManifestacao;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Detalhe da manifestação. {@code anonima} e {@code manifestante} são nulos para quem não pode ver a identidade
 * (SERVIDOR): a identificação é informação restrita (Lei 13.460, art. 10, § 7º).
 * {@code diasRestantes} e {@code vencida} só fazem sentido enquanto a administração deve uma resposta.
 */
public record ManifestacaoResponse(Long id, String protocolo, Tipo tipo, String assunto, String descricao,
                                   StatusManifestacao status, OffsetDateTime dataAbertura, LocalDate dataLimite,
                                   boolean prorrogada, Long diasRestantes, Boolean vencida,
                                   OffsetDateTime dataEncerramento, Secretaria secretaria, Boolean anonima,
                                   Manifestante manifestante, RecursoResponse recurso) {

    public record Tipo(Long id, String codigo, String nome, CategoriaManifestacao categoria) {
    }

    public record Secretaria(Long id, String sigla, String nome) {
    }

    public record Manifestante(String nome, String email, String cpf, String telefone) {
    }
}
