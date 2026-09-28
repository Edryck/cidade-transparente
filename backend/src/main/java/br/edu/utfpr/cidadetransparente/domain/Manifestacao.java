package br.edu.utfpr.cidadetransparente.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Manifestacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Municipio municipio;

    /** ANO-IBGE-SEQUENCIAL, ex.: 2026-4106902-000123. */
    private String protocolo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private TipoManifestacao tipoManifestacao;

    /** Nulo = manifestação anônima. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Cidadao cidadao;

    /** Nula até o encaminhamento. */
    @ManyToOne(fetch = FetchType.LAZY)
    private Secretaria secretaria;

    private String assunto;
    private String descricao;

    @Enumerated(EnumType.STRING)
    private StatusManifestacao status;

    private OffsetDateTime dataAbertura;

    /** Calculada na abertura com o PrazoLegal vigente; mudança posterior da regra não altera. */
    private LocalDate dataLimite;

    /** Prorrogação só pode acontecer uma vez. */
    private boolean prorrogada;

    private OffsetDateTime dataEncerramento;
}
