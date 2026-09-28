package br.edu.utfpr.cidadetransparente.config;

import br.edu.utfpr.cidadetransparente.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/** Gera e valida o token. Claims: sub (id do usuário), roles e municipioId (ausente no ADMIN_PLATAFORMA). */
@Component
public class JwtService {

    private final SecretKey chave;
    private final long expiracaoMs;

    public JwtService(@Value("${jwt.secret}") String segredo, @Value("${jwt.expiration}") long expiracaoMs) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 bytes para assinar com HS256");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.expiracaoMs = expiracaoMs;
    }

    public String gerarToken(Usuario usuario) {
        Date agora = new Date();
        var builder = Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("roles", List.of(usuario.getPerfil().getNome()))
                .issuedAt(agora)
                .expiration(new Date(agora.getTime() + expiracaoMs));
        if (usuario.getMunicipio() != null) {
            builder.claim("municipioId", usuario.getMunicipio().getId());
        }
        return builder.signWith(chave).compact();
    }

    /** Valida assinatura e expiração. Lança {@link JwtException} se o token não servir. */
    public UsuarioAutenticado validar(String token) {
        Claims claims = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
        List<?> roles = claims.get("roles", List.class);
        if (roles == null || roles.isEmpty()) {
            throw new JwtException("Token sem perfil");
        }
        Number municipioId = claims.get("municipioId", Number.class);
        return new UsuarioAutenticado(
                Long.valueOf(claims.getSubject()),
                roles.getFirst().toString(),
                municipioId == null ? null : municipioId.longValue());
    }

    public long getExpiracaoMs() {
        return expiracaoMs;
    }
}
