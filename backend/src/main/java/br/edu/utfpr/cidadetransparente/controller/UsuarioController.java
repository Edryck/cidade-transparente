package br.edu.utfpr.cidadetransparente.controller;

import br.edu.utfpr.cidadetransparente.dto.UsuarioRequest;
import br.edu.utfpr.cidadetransparente.dto.UsuarioResponse;
import br.edu.utfpr.cidadetransparente.service.UsuarioService;
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
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;

    /** Todos os usuários do município; {@code ?perfil=SERVIDOR} filtra por perfil. */
    @GetMapping
    public CollectionModel<EntityModel<UsuarioResponse>> listar(@RequestParam(required = false) String perfil) {
        return CollectionModel.of(usuarioService.listar(perfil).stream().map(this::comLinks).toList(),
                linkTo(methodOn(UsuarioController.class).listar(perfil)).withSelfRel());
    }

    @GetMapping("/{id}")
    public EntityModel<UsuarioResponse> buscar(@PathVariable Long id) {
        return comLinks(usuarioService.buscar(id));
    }

    @PostMapping
    public ResponseEntity<EntityModel<UsuarioResponse>> criar(@Valid @RequestBody UsuarioRequest request) {
        EntityModel<UsuarioResponse> modelo = comLinks(usuarioService.criar(request));
        return ResponseEntity.created(modelo.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(modelo);
    }

    @PutMapping("/{id}")
    public EntityModel<UsuarioResponse> atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        return comLinks(usuarioService.atualizar(id, request));
    }

    /** Desativa (não apaga). Para reativar: PUT com "ativo": true. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desativar(@PathVariable Long id) {
        usuarioService.desativar(id);
    }

    private EntityModel<UsuarioResponse> comLinks(UsuarioResponse usuario) {
        EntityModel<UsuarioResponse> modelo = EntityModel.of(usuario,
                linkTo(methodOn(UsuarioController.class).buscar(usuario.id())).withSelfRel(),
                linkTo(methodOn(UsuarioController.class).listar(null)).withRel("usuarios"));
        if (usuario.secretariaId() != null) {
            modelo.add(linkTo(methodOn(SecretariaController.class).buscar(usuario.secretariaId())).withRel("secretaria"));
        }
        return modelo;
    }
}
