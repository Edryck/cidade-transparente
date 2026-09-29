package br.edu.utfpr.cidadetransparente.dto;

import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;

/** {@code data} é a ocorrência no ano consultado (para feriado anual, o mesmo dia e mês naquele ano). */
@Relation(collectionRelation = "feriados", itemRelation = "feriado")
public record FeriadoResponse(Long id, LocalDate data, String descricao, boolean anual, Ambito ambito) {

    public enum Ambito { NACIONAL, MUNICIPAL }
}
