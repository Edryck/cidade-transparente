package br.edu.utfpr.cidadetransparente.controller;

import br.edu.utfpr.cidadetransparente.dto.SecretariaRequest;
import br.edu.utfpr.cidadetransparente.dto.SecretariaResponse;
import br.edu.utfpr.cidadetransparente.service.SecretariaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1/secretarias")
@RequiredArgsConstructor
@Tag(name = "Secretarias")
public class SecretariaController {

    private final SecretariaService secretariaService;

    /** OUVIDOR também lê: precisa escolher a secretaria no encaminhamento. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OUVIDOR')")
    public CollectionModel<EntityModel<SecretariaResponse>> listar() {
        return CollectionModel.of(secretariaService.listar().stream().map(this::comLinks).toList(),
                linkTo(methodOn(SecretariaController.class).listar()).withSelfRel());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OUVIDOR')")
    public EntityModel<SecretariaResponse> buscar(@PathVariable Long id) {
        return comLinks(secretariaService.buscar(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<SecretariaResponse>> criar(@Valid @RequestBody SecretariaRequest request) {
        EntityModel<SecretariaResponse> modelo = comLinks(secretariaService.criar(request));
        return ResponseEntity.created(modelo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(modelo);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public EntityModel<SecretariaResponse> atualizar(@PathVariable Long id,
                                                     @Valid @RequestBody SecretariaRequest request) {
        return comLinks(secretariaService.atualizar(id, request));
    }

    /** Desativa (não apaga). Para reativar: PUT com "ativa": true. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable Long id) {
        secretariaService.desativar(id);
    }

    private EntityModel<SecretariaResponse> comLinks(SecretariaResponse secretaria) {
        return EntityModel.of(secretaria,
                linkTo(methodOn(SecretariaController.class).buscar(secretaria.id())).withSelfRel(),
                linkTo(methodOn(SecretariaController.class).listar()).withRel("secretarias"));
    }
}
