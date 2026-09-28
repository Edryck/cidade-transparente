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
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String email;
    private String senhaHash;
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Perfil perfil;

    /** Nulo só para ADMIN_PLATAFORMA, que não enxerga dados de nenhum município. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Municipio municipio;

    /** Preenchida só para SERVIDOR: define quais manifestações ele pode responder. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Secretaria secretaria;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime criadoEm;
}
