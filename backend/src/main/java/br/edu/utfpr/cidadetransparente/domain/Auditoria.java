package br.edu.utfpr.cidadetransparente.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Registro de acesso (Marco Civil, art. 15: IP guardado por 6 meses) e trilha de operações (LGPD art. 37).
 * Só é gravada, nunca lida pela API: a entrega do IP depende de ordem judicial (Marco Civil, art. 15, § 3º).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Municipio municipio;

    /** Nulo em ação anônima. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    private AcaoAuditoria acao;

    private String entidade;
    private Long entidadeId;
    private String detalhe;

    /** Apagado após o prazo de retenção pela rotina de expurgo. */
    private String ip;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime registradoEm;
}
