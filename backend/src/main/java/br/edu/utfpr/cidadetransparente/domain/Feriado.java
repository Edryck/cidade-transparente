package br.edu.utfpr.cidadetransparente.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.MonthDay;

/**
 * Dia sem expediente, usado na contagem de prazo (Lei 9.784, art. 66, § 1º). Sem município é nacional
 * (cadastrado por migration, porque muda só com lei federal); com município, é cadastrado pela prefeitura.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Feriado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Municipio municipio;

    /** Em feriado anual, a primeira ocorrência; vale o dia e o mês. */
    private LocalDate data;

    private boolean anual;
    private String descricao;

    public boolean cai(LocalDate dia) {
        return anual ? MonthDay.from(data).equals(MonthDay.from(dia)) : data.equals(dia);
    }
}
