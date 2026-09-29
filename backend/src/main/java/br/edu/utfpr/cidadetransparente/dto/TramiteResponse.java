package br.edu.utfpr.cidadetransparente.dto;

import br.edu.utfpr.cidadetransparente.domain.StatusManifestacao;
import org.springframework.hateoas.server.core.Relation;

import java.time.OffsetDateTime;

/**
 * Linha do histórico. Ação do próprio cidadão aparece como "Manifestante", nunca pelo nome: o histórico
 * também é visto pela secretaria, que não pode saber quem se manifestou.
 */
@Relation(collectionRelation = "tramites", itemRelation = "tramite")
public record TramiteResponse(StatusManifestacao statusAnterior, StatusManifestacao statusNovo, String descricao,
                              String secretariaDestino, String responsavel, OffsetDateTime registradoEm) {
}
