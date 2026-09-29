package br.edu.utfpr.cidadetransparente.controller;

import br.edu.utfpr.cidadetransparente.config.UsuarioAutenticado;
import br.edu.utfpr.cidadetransparente.domain.Perfil;
import br.edu.utfpr.cidadetransparente.dto.RelatorioGestaoRequest;
import br.edu.utfpr.cidadetransparente.dto.RelatorioGestaoResponse;
import br.edu.utfpr.cidadetransparente.dto.RelatorioGestaoResponse.Situacao;
import br.edu.utfpr.cidadetransparente.service.RelatorioGestaoService;
import br.edu.utfpr.cidadetransparente.service.RelatorioGestaoService.Visao;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Relatório de gestão")
public class RelatorioGestaoController {

    private final RelatorioGestaoService service;

    /** Prévia para a equipe (ADMIN representa a autoridade máxima que recebe o relatório, art. 15, p. ú., I). */
    @GetMapping("/relatorios/gestao/{ano}")
    @PreAuthorize("hasAnyRole('OUVIDOR', 'ADMIN')")
    public EntityModel<RelatorioGestaoResponse> buscar(@PathVariable int ano) {
        return comLinks(service.buscar(ano));
    }

    /** Rascunho da parte escrita. Quem elabora o relatório é a ouvidoria (Lei 13.460, art. 14, II). */
    @PutMapping("/relatorios/gestao/{ano}")
    @PreAuthorize("hasRole('OUVIDOR')")
    public EntityModel<RelatorioGestaoResponse> salvarRascunho(@PathVariable int ano,
                                                               @Valid @RequestBody RelatorioGestaoRequest request) {
        return comLinks(service.salvarRascunho(ano, request));
    }

    @PostMapping("/relatorios/gestao/{ano}/publicacao")
    @PreAuthorize("hasRole('OUVIDOR')")
    public EntityModel<RelatorioGestaoResponse> publicar(@PathVariable int ano) {
        return comLinks(service.publicar(ano));
    }

    /** Pública (Lei 13.460, art. 15, parágrafo único, II): relatórios publicados do município. */
    @GetMapping("/municipios/{id}/relatorios-gestao")
    @SecurityRequirements
    public CollectionModel<EntityModel<RelatorioGestaoResponse.Publicado>> publicados(@PathVariable Long id) {
        return CollectionModel.of(service.publicados(id).stream()
                        .map(r -> EntityModel.of(r, linkTo(methodOn(RelatorioGestaoController.class)
                                .publicado(id, r.ano())).withSelfRel()))
                        .toList(),
                linkTo(methodOn(RelatorioGestaoController.class).publicados(id)).withSelfRel());
    }

    @GetMapping("/municipios/{id}/relatorios-gestao/{ano}")
    @SecurityRequirements
    public EntityModel<RelatorioGestaoResponse> publicado(@PathVariable Long id, @PathVariable int ano) {
        return EntityModel.of(service.publicado(id, ano),
                linkTo(methodOn(RelatorioGestaoController.class).publicado(id, ano)).withSelfRel(),
                linkTo(methodOn(RelatorioGestaoController.class).publicados(id)).withRel("relatorios"));
    }

    /** Rascunho oferece edição (e publicação, se já pode); publicado só aponta para a versão pública. */
    private EntityModel<RelatorioGestaoResponse> comLinks(Visao v) {
        int ano = v.dados().ano();
        RelatorioGestaoController c = methodOn(RelatorioGestaoController.class);
        EntityModel<RelatorioGestaoResponse> modelo = EntityModel.of(v.dados(), linkTo(c.buscar(ano)).withSelfRel());
        boolean ouvidor = Perfil.OUVIDOR.equals(UsuarioAutenticado.atual().perfil());
        if (v.dados().situacao() == Situacao.PUBLICADO) {
            modelo.add(linkTo(methodOn(RelatorioGestaoController.class).publicado(v.municipioId(), ano)).withRel("publico"));
        } else if (ouvidor) {
            modelo.add(linkTo(methodOn(RelatorioGestaoController.class).salvarRascunho(ano, null)).withRel("rascunho"));
            if (v.impedimentoPublicacao() == null) {
                modelo.add(linkTo(methodOn(RelatorioGestaoController.class).publicar(ano)).withRel("publicacao"));
            }
        }
        return modelo;
    }
}
