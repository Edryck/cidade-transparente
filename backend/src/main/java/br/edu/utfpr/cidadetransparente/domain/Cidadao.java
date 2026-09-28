package br.edu.utfpr.cidadetransparente.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/** Quem manifesta. Manifestação anônima não tem cidadão (cidadao_id nulo). */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Cidadao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Municipio municipio;

    /** Nulo quando o cidadão não tem login (ex.: registro presencial). */
    @OneToOne(fetch = FetchType.LAZY)
    private Usuario usuario;

    private String nome;
    private String cpf;
    private String email;
    private String telefone;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime criadoEm;
}
