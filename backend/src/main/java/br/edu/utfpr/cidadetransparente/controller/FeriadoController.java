package br.edu.utfpr.cidadetransparente.controller;

import br.edu.utfpr.cidadetransparente.dto.FeriadoRequest;
import br.edu.utfpr.cidadetransparente.dto.FeriadoResponse;
import br.edu.utfpr.cidadetransparente.service.FeriadoService;
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
@RequestMapping("/api/v1/feriados")
@RequiredArgsConstructor
@Tag(name = "Feriados")
public class FeriadoController {

    private final FeriadoService feriadoService;

    /** Calendário do ano ({@code ?ano=}, padrão o atual): nacionais mais os do município do token. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OUVIDOR', 'SERVIDOR', 'CIDADAO')")
    public CollectionModel<EntityModel<FeriadoResponse>> listar(@RequestParam(required = false) Integer ano) {
        return CollectionModel.of(feriadoService.listar(ano).stream().map(this::comLinks).toList(),
                linkTo(methodOn(FeriadoController.class).listar(ano)).withSelfRel());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OUVIDOR', 'SERVIDOR', 'CIDADAO')")
    public EntityModel<FeriadoResponse> buscar(@PathVariable Long id) {
        return comLinks(feriadoService.buscar(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<FeriadoResponse>> criar(@Valid @RequestBody FeriadoRequest request) {
        EntityModel<FeriadoResponse> modelo = comLinks(feriadoService.criar(request));
        return ResponseEntity.created(modelo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(modelo);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        feriadoService.excluir(id);
    }

    private EntityModel<FeriadoResponse> comLinks(FeriadoResponse f) {
        return EntityModel.of(f,
                linkTo(methodOn(FeriadoController.class).buscar(f.id())).withSelfRel(),
                linkTo(methodOn(FeriadoController.class).listar(null)).withRel("feriados"));
    }
}
