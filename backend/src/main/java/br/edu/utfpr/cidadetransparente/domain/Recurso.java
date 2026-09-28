package br.edu.utfpr.cidadetransparente.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** Recurso da LAI (art. 15), instância única: no máximo um por resposta, julgado pelo OUVIDOR. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Resposta resposta;

    private String justificativa;

    @Enumerated(EnumType.STRING)
    private StatusRecurso status = StatusRecurso.PENDENTE;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime interpostoEm;

    private LocalDate dataLimiteJulgamento;
    private String decisao;

    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario julgadoPor;

    private OffsetDateTime julgadoEm;
}
