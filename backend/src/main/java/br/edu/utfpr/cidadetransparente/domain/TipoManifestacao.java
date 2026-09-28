package br.edu.utfpr.cidadetransparente.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tipo nacional (definido em lei, não por município). As flags tiram as regras do código. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class TipoManifestacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;
    private String nome;

    @Enumerated(EnumType.STRING)
    private CategoriaManifestacao categoria;

    /** Denúncia aceita anonimato; pedido LAI exige identificação. */
    private boolean permiteAnonimo;

    /** Só a LAI prevê fase recursal. */
    private boolean permiteRecurso;
}
