package br.edu.utfpr.cidadetransparente.dto;

import br.edu.utfpr.cidadetransparente.domain.CategoriaManifestacao;
import br.edu.utfpr.cidadetransparente.domain.PrazoLegal;
import br.edu.utfpr.cidadetransparente.domain.TipoManifestacao;
import org.springframework.hateoas.server.core.Relation;

/** Tipo com o prazo que vale para o município do usuário (o municipal, se houver; senão, o federal). */
@Relation(collectionRelation = "tipos", itemRelation = "tipo")
public record TipoManifestacaoResponse(Long id, String codigo, String nome, CategoriaManifestacao categoria,
                                       boolean permiteAnonimo, boolean permiteRecurso, Prazo prazo) {

    public enum Origem { FEDERAL, MUNICIPAL }

    public record Prazo(Origem origem, int diasResposta, boolean permiteProrrogacao, Integer diasProrrogacao,
                        Integer diasInterposicaoRecurso, Integer diasJulgamentoRecurso) {
    }

    public static TipoManifestacaoResponse de(TipoManifestacao t, PrazoLegal p) {
        var prazo = new Prazo(p.getMunicipio() == null ? Origem.FEDERAL : Origem.MUNICIPAL,
                p.getDiasResposta(), p.isPermiteProrrogacao(), p.getDiasProrrogacao(),
                p.getDiasInterposicaoRecurso(), p.getDiasJulgamentoRecurso());
        return new TipoManifestacaoResponse(t.getId(), t.getCodigo(), t.getNome(), t.getCategoria(),
                t.isPermiteAnonimo(), t.isPermiteRecurso(), prazo);
    }
}
