package br.edu.utfpr.cidadetransparente.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** ADMIN_PLATAFORMA, ADMIN, OUVIDOR, SERVIDOR ou CIDADAO. Linhas fixas, criadas pela V5. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Perfil {

    public static final String ADMIN_PLATAFORMA = "ADMIN_PLATAFORMA";
    public static final String ADMIN = "ADMIN";
    public static final String OUVIDOR = "OUVIDOR";
    public static final String SERVIDOR = "SERVIDOR";
    public static final String CIDADAO = "CIDADAO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
}
