package br.edu.utfpr.cidadetransparente.controller;

import br.edu.utfpr.cidadetransparente.dto.AlteracaoSenhaRequest;
import br.edu.utfpr.cidadetransparente.dto.AvisoPrivacidadeResponse;
import br.edu.utfpr.cidadetransparente.dto.EncarregadoRequest;
import br.edu.utfpr.cidadetransparente.dto.EncerramentoContaResponse;
import br.edu.utfpr.cidadetransparente.dto.MinhaContaRequest;
import br.edu.utfpr.cidadetransparente.dto.MinhaContaResponse;
import br.edu.utfpr.cidadetransparente.service.PrivacidadeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Privacidade (LGPD)")
public class PrivacidadeController {

    private final PrivacidadeService privacidadeService;

    /** Pública: a LGPD manda divulgar essas informações (arts. 9º, 23, I, e 41, § 1º). */
    @GetMapping("/municipios/{id}/privacidade")
    @SecurityRequirements
    public EntityModel<AvisoPrivacidadeResponse> aviso(@PathVariable Long id) {
        return EntityModel.of(privacidadeService.aviso(id),
                linkTo(methodOn(PrivacidadeController.class).aviso(id)).withSelfRel(),
                linkTo(methodOn(PrivacidadeController.class).minhaConta()).withRel("minha-conta"));
    }

    @PutMapping("/municipios/{id}/encarregado")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void definirEncarregado(@PathVariable Long id, @Valid @RequestBody EncarregadoRequest request) {
        privacidadeService.definirEncarregado(id, request);
    }

    /** Qualquer usuário autenticado é titular dos próprios dados, inclusive a equipe. */
    @GetMapping("/minha-conta")
    public EntityModel<MinhaContaResponse> minhaConta() {
        return comLinks(privacidadeService.minhaConta());
    }

    @PutMapping("/minha-conta")
    public EntityModel<MinhaContaResponse> corrigirMinhaConta(@Valid @RequestBody MinhaContaRequest request) {
        return comLinks(privacidadeService.corrigirMinhaConta(request));
    }

    /** Qualquer perfil troca a própria senha; 204 sem corpo, porque não há recurso a devolver. */
    @PutMapping("/minha-conta/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarSenha(@Valid @RequestBody AlteracaoSenhaRequest request) {
        privacidadeService.alterarSenha(request);
    }

    /** 200 com corpo, e não 204: a lei exige explicar por que os dados não são eliminados (art. 18, § 4º, II). */
    @DeleteMapping("/minha-conta")
    @PreAuthorize("hasRole('CIDADAO')")
    public EncerramentoContaResponse encerrarMinhaConta() {
        return privacidadeService.encerrarMinhaConta();
    }

    private EntityModel<MinhaContaResponse> comLinks(MinhaContaResponse conta) {
        EntityModel<MinhaContaResponse> modelo = EntityModel.of(conta,
                linkTo(methodOn(PrivacidadeController.class).minhaConta()).withSelfRel());
        if (conta.municipioId() != null) {
            modelo.add(linkTo(methodOn(PrivacidadeController.class).aviso(conta.municipioId())).withRel("privacidade"));
        }
        return modelo;
    }
}
