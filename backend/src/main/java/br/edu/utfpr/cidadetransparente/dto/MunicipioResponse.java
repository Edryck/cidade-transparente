package br.edu.utfpr.cidadetransparente.dto;

import br.edu.utfpr.cidadetransparente.domain.Municipio;
import org.springframework.hateoas.server.core.Relation;

import java.time.OffsetDateTime;

@Relation(collectionRelation = "municipios", itemRelation = "municipio")
public record MunicipioResponse(Long id, String nome, String uf, String codigoIbge, boolean ativo,
                                OffsetDateTime criadoEm) {

    public static MunicipioResponse de(Municipio m) {
        return new MunicipioResponse(m.getId(), m.getNome(), m.getUf(), m.getCodigoIbge(), m.isAtivo(), m.getCriadoEm());
    }
}
