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
public class Anexo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Manifestacao manifestacao;

    private String nomeArquivo;
    private String contentType;
    private long tamanho;

    /** Até 5 MB (CHECK no banco). Listagens devem usar projeção sem este campo. */
    private byte[] conteudo;

    /** Nulo quando enviado junto com manifestação anônima. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario enviadoPor;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime enviadoEm;
}
