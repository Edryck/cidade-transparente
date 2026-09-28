package br.edu.utfpr.cidadetransparente.config;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lê o "Authorization: Bearer ..." e autentica a requisição. Não é @Component de propósito: é criado só
 * dentro do SecurityConfig, senão o Spring Boot o registraria também como filtro de servlet fora da cadeia.
 */
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIXO)) {
            try {
                UsuarioAutenticado usuario = jwtService.validar(header.substring(PREFIXO.length()));
                var autoridades = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.perfil()));
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(usuario, null, autoridades));
            } catch (JwtException | IllegalArgumentException e) {
                // Token inválido ou expirado: segue como anônimo; rota protegida responde 401 no entry point
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
