package br.edu.utfpr.cidadetransparente.dto;

import br.edu.utfpr.cidadetransparente.domain.Secretaria;
import br.edu.utfpr.cidadetransparente.domain.Usuario;
import org.springframework.hateoas.server.core.Relation;

import java.time.OffsetDateTime;

/** Nunca expõe o hash da senha. */
@Relation(collectionRelation = "usuarios", itemRelation = "usuario")
public record UsuarioResponse(Long id, String nome, String email, String perfil, Long secretariaId,
                              String secretariaSigla, boolean ativo, OffsetDateTime criadoEm) {

    public static UsuarioResponse de(Usuario u) {
        Secretaria s = u.getSecretaria();
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil().getNome(),
                s == null ? null : s.getId(), s == null ? null : s.getSigla(), u.isAtivo(), u.getCriadoEm());
    }
}
