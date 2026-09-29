package br.edu.utfpr.cidadetransparente.dto;

import br.edu.utfpr.cidadetransparente.domain.StatusManifestacao;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Item da listagem. Nunca traz a identidade do manifestante. */
@Relation(collectionRelation = "manifestacoes", itemRelation = "manifestacao")
public record ManifestacaoResumo(Long id, String protocolo, String tipo, String assunto, StatusManifestacao status,
                                 OffsetDateTime dataAbertura, LocalDate dataLimite, boolean prorrogada,
                                 String secretariaSigla, Long diasRestantes, boolean vencida) {
}
