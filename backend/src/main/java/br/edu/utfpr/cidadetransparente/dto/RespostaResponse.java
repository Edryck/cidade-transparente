package br.edu.utfpr.cidadetransparente.dto;

import br.edu.utfpr.cidadetransparente.domain.ResultadoLai;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Resposta oficial. Em pedido LAI com acesso negado, traz quando e a quem recorrer: a lei manda informar
 * a possibilidade de recurso, o prazo e a autoridade competente (Lei 12.527, art. 11, § 4º).
 */
@Relation(collectionRelation = "respostas", itemRelation = "resposta")
public record RespostaResponse(Long id, String texto, ResultadoLai resultadoLai, String secretaria,
                               OffsetDateTime respondidaEm, boolean recursoCabivel, LocalDate prazoRecurso,
                               String instanciaRecursal) {
}
