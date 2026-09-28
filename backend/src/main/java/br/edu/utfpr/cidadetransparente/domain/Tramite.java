package br.edu.utfpr.cidadetransparente.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/** Histórico de negócio da manifestação: uma linha por transição (ou prorrogação). */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Tramite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Manifestacao manifestacao;

    /** Nulo no registro da abertura. */
    @Enumerated(EnumType.STRING)
    private StatusManifestacao statusAnterior;

    @Enumerated(EnumType.STRING)
    private StatusManifestacao statusNovo;

    /** Nulo quando a ação é de cidadão anônimo. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    private Secretaria secretariaDestino;

    private String descricao;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime registradoEm;
}
