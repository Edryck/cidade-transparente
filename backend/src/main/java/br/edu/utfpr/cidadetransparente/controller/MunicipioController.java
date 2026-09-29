package br.edu.utfpr.cidadetransparente.controller;

import br.edu.utfpr.cidadetransparente.dto.MunicipioAtualizacaoRequest;
import br.edu.utfpr.cidadetransparente.dto.MunicipioRequest;
import br.edu.utfpr.cidadetransparente.dto.MunicipioResponse;
import br.edu.utfpr.cidadetransparente.dto.MunicipioResumo;
import br.edu.utfpr.cidadetransparente.service.MunicipioService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_PLATAFORMA')")
    public CollectionModel<EntityModel<MunicipioResponse>> listar() {
        return CollectionModel.of(municipioService.listar().stream().map(this::comLinks).toList(),
                linkTo(methodOn(MunicipioController.class).listar()).withSelfRel());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_PLATAFORMA')")
    public EntityModel<MunicipioResponse> buscar(@PathVariable Long id) {
        return comLinks(municipioService.buscar(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_PLATAFORMA')")
    public ResponseEntity<EntityModel<MunicipioResponse>> criar(@Valid @RequestBody MunicipioRequest request) {
        EntityModel<MunicipioResponse> modelo = comLinks(municipioService.criar(request));
        return ResponseEntity.created(modelo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(modelo);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_PLATAFORMA')")
    public EntityModel<MunicipioResponse> atualizar(@PathVariable Long id,
                                                    @Valid @RequestBody MunicipioAtualizacaoRequest request) {
        return comLinks(municipioService.atualizar(id, request));
    }

    private EntityModel<MunicipioResponse> comLinks(MunicipioResponse municipio) {
        return EntityModel.of(municipio,
                linkTo(methodOn(MunicipioController.class).buscar(municipio.id())).withSelfRel(),
                linkTo(methodOn(MunicipioController.class).listar()).withRel("municipios"));
    }
}
