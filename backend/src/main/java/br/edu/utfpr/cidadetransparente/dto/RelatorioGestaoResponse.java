package br.edu.utfpr.cidadetransparente.dto;

import br.edu.utfpr.cidadetransparente.domain.EstatisticasGestao;
import org.springframework.hateoas.server.core.Relation;

import java.time.OffsetDateTime;

/** Em RASCUNHO, os números são calculados na hora; PUBLICADO, são os congelados na publicação. */
public record RelatorioGestaoResponse(String municipio, int ano, Situacao situacao, EstatisticasGestao estatisticas,
                                      String analisePontosRecorrentes, String providenciasAdotadas,
                                      OffsetDateTime publicadoEm) {

    public enum Situacao { RASCUNHO, PUBLICADO }

    @Relation(collectionRelation = "relatorios", itemRelation = "relatorio")
    public record Publicado(int ano, OffsetDateTime publicadoEm) {
    }
}
