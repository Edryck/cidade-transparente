package br.edu.utfpr.cidadetransparente.controller;

import br.edu.utfpr.cidadetransparente.dto.PrazoRequest;
import br.edu.utfpr.cidadetransparente.dto.TipoManifestacaoResponse;
import br.edu.utfpr.cidadetransparente.service.TipoManifestacaoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1/tipos-manifestacao")
@RequiredArgsConstructor
@Tag(name = "Tipos e prazos")
public class TipoManifestacaoController {

    private final TipoManifestacaoService tipoService;

    /** Qualquer perfil com município: o cidadão escolhe o tipo, a equipe consulta os prazos. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OUVIDOR', 'SERVIDOR', 'CIDADAO')")
    public CollectionModel<EntityModel<TipoManifestacaoResponse>> listar() {
        return CollectionModel.of(tipoService.listar().stream().map(this::comLinks).toList(),
                linkTo(methodOn(TipoManifestacaoController.class).listar()).withSelfRel());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OUVIDOR', 'SERVIDOR', 'CIDADAO')")
    public EntityModel<TipoManifestacaoResponse> buscar(@PathVariable Long id) {
        return comLinks(tipoService.buscar(id));
    }

    /** Cria ou substitui a regra de prazo do município para o tipo. */
    @PutMapping("/{id}/prazo")
    @PreAuthorize("hasRole('ADMIN')")
    public EntityModel<TipoManifestacaoResponse> definirPrazo(@PathVariable Long id,
                                                             @Valid @RequestBody PrazoRequest request) {
        return comLinks(tipoService.definirPrazoMunicipal(id, request));
    }

    /** Remove a regra municipal: o tipo volta a seguir o prazo federal. */
    @DeleteMapping("/{id}/prazo")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerPrazo(@PathVariable Long id) {
        tipoService.removerPrazoMunicipal(id);
    }

    private EntityModel<TipoManifestacaoResponse> comLinks(TipoManifestacaoResponse tipo) {
        return EntityModel.of(tipo,
                linkTo(methodOn(TipoManifestacaoController.class).buscar(tipo.id())).withSelfRel(),
                linkTo(methodOn(TipoManifestacaoController.class).listar()).withRel("tipos"));
    }
}
