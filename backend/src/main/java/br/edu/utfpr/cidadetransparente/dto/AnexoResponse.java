package br.edu.utfpr.cidadetransparente.dto;

import org.springframework.hateoas.server.core.Relation;

import java.time.OffsetDateTime;

@Relation(collectionRelation = "anexos", itemRelation = "anexo")
public record AnexoResponse(Long id, String nomeArquivo, String contentType, long tamanho, OffsetDateTime enviadoEm) {
}
