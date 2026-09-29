package br.edu.utfpr.cidadetransparente.controller;

import br.edu.utfpr.cidadetransparente.dto.MunicipioResumo;
import br.edu.utfpr.cidadetransparente.service.MunicipioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/municipios")
@RequiredArgsConstructor
@Tag(name = "Municípios")
public class MunicipioController {

    private final MunicipioService municipioService;

    /** Pública: alimenta a escolha de município no registro do cidadão. */
    @GetMapping("/ativos")
    @SecurityRequirements
    public List<MunicipioResumo> listarAtivos() {
        return municipioService.listarAtivos();
    }
}
