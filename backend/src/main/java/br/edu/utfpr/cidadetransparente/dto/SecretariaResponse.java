package br.edu.utfpr.cidadetransparente.dto;

import br.edu.utfpr.cidadetransparente.domain.Secretaria;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "secretarias", itemRelation = "secretaria")
public record SecretariaResponse(Long id, String nome, String sigla, String email, boolean ativa) {

    public static SecretariaResponse de(Secretaria s) {
        return new SecretariaResponse(s.getId(), s.getNome(), s.getSigla(), s.getEmail(), s.isAtiva());
    }
}
