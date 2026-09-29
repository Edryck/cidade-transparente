package br.edu.utfpr.cidadetransparente.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/** Relatório anual de gestão da ouvidoria (Lei 13.460, arts. 14, II, e 15). Publicado, não muda mais. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class RelatorioGestao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Municipio municipio;

    private int ano;

    /** Lei 13.460, art. 15, III. */
    private String analisePontosRecorrentes;

    /** Lei 13.460, art. 15, IV. */
    private String providenciasAdotadas;

    /** Números congelados na publicação; nulo no rascunho. */
    @JdbcTypeCode(SqlTypes.JSON)
    private EstatisticasGestao estatisticas;

    private OffsetDateTime publicadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario publicadoPor;

    @UpdateTimestamp
    private OffsetDateTime atualizadoEm;

    public boolean isPublicado() {
        return publicadoEm != null;
    }
}
