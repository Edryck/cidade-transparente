package br.edu.utfpr.cidadetransparente.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
@OpenAPIDefinition(
        info = @Info(title = "Cidade Transparente API", version = "v1"),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService, ObjectMapper mapper) throws Exception {
        AuthenticationEntryPoint naoAutenticado = (req, res, ex) ->
                escreverErro(res, mapper, HttpStatus.UNAUTHORIZED, "Token ausente, inválido ou expirado");
        http
                // API stateless com token no header: sem sessão, sem cookie, então CSRF não se aplica
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/protocolos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/municipios/ativos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/municipios/*/privacidade").permitAll()
                        // Relatório de gestão publicado é público por lei (Lei 13.460, art. 15, parágrafo único, II)
                        .requestMatchers(HttpMethod.GET, "/api/v1/municipios/*/relatorios-gestao", "/api/v1/municipios/*/relatorios-gestao/*").permitAll()
                        // Pública para a denúncia anônima; com token, o service exige perfil CIDADAO
                        .requestMatchers(HttpMethod.POST, "/api/v1/manifestacoes").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Sem isso, qualquer erro encaminhado para /error viraria 401
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler((req, res, ex) ->
                                escreverErro(res, mapper, HttpStatus.FORBIDDEN, "Seu perfil não tem permissão para este recurso")))
                .addFilterBefore(new JwtFilter(jwtService, naoAutenticado), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 401/403 da camada de segurança no mesmo formato (ProblemDetail) dos erros dos controllers. */
    private static void escreverErro(HttpServletResponse res, ObjectMapper mapper, HttpStatus status, String detalhe)
            throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(res.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detalhe));
    }
}
