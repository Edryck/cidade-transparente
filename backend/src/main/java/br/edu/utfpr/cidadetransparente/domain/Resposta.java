package br.edu.utfpr.cidadetransparente.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Resposta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Manifestacao manifestacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Secretaria secretaria;

    private String texto;

    /** Só em pedido LAI: define se cabe recurso (Lei 12.527, arts. 11 e 15). */
    @Enumerated(EnumType.STRING)
    private ResultadoLai resultadoLai;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime respondidaEm;
}
