package br.edu.utfpr.cidadetransparente.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/** Tenant: tudo que é específico de uma prefeitura aponta para cá. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Municipio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String uf;
    private String codigoIbge;
    private boolean ativo = true;

    /** Encarregado de dados (LGPD art. 41), indicado pela prefeitura e divulgado no aviso de privacidade. */
    private String encarregadoNome;
    private String encarregadoEmail;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime criadoEm;
}
