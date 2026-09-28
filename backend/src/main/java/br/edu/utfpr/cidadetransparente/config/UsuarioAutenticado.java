package br.edu.utfpr.cidadetransparente.config;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Dados do usuário extraídos do JWT. É o principal da autenticação: nenhuma consulta ao banco por requisição. */
public record UsuarioAutenticado(Long id, String perfil, Long municipioId) {

    public static UsuarioAutenticado atual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            throw new AuthenticationCredentialsNotFoundException("Requisição sem usuário autenticado");
        }
        return usuario;
    }

    /**
     * Município do token, usado para filtrar toda consulta de dado municipal.
     * Falha fechado: se o token não tem município (ADMIN_PLATAFORMA), nega o acesso em vez de devolver
     * nulo, que numa query mal escrita viraria "sem filtro" e vazaria dados de todas as prefeituras.
     */
    @Override
    public Long municipioId() {
        if (municipioId == null) {
            throw new AccessDeniedException("Usuário sem município não acessa dados municipais");
        }
        return municipioId;
    }
}
