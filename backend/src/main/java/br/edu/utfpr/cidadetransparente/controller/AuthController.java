package br.edu.utfpr.cidadetransparente.controller;

import br.edu.utfpr.cidadetransparente.dto.LoginRequest;
import br.edu.utfpr.cidadetransparente.dto.LoginResponse;
import br.edu.utfpr.cidadetransparente.dto.RegistroCidadaoRequest;
import br.edu.utfpr.cidadetransparente.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class AuthController {

    private final AuthService authService;

    /** Rota pública: o @SecurityRequirements vazio tira o cadeado dela no Swagger. */
    @PostMapping("/login")
    @SecurityRequirements
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Cria o usuário CIDADAO já autenticado: devolve o token para o app seguir direto para a manifestação. */
    @PostMapping("/registro-cidadao")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    public LoginResponse registrarCidadao(@Valid @RequestBody RegistroCidadaoRequest request) {
        return authService.registrarCidadao(request);
    }
}
