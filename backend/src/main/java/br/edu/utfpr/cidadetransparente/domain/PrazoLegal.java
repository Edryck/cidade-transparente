package br.edu.utfpr.cidadetransparente.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Regra de prazo de um tipo. Com municipio nulo é a regra federal; com municipio preenchido
 * sobrescreve a federal para aquela prefeitura (art. 45 da LAI).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class PrazoLegal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private TipoManifestacao tipoManifestacao;

    @ManyToOne(fetch = FetchType.LAZY)
    private Municipio municipio;

    private int diasResposta;
    private boolean permiteProrrogacao;
    private Integer diasProrrogacao;

    /** Nulos quando o tipo não tem fase recursal. */
    private Integer diasInterposicaoRecurso;
    private Integer diasJulgamentoRecurso;

    @UpdateTimestamp
    private OffsetDateTime atualizadoEm;
}
