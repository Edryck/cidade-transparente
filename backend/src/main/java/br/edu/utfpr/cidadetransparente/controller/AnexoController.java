package br.edu.utfpr.cidadetransparente.controller;

import br.edu.utfpr.cidadetransparente.domain.Anexo;
import br.edu.utfpr.cidadetransparente.dto.AnexoResponse;
import br.edu.utfpr.cidadetransparente.service.AnexoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v1/manifestacoes/{id}/anexos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OUVIDOR', 'SERVIDOR', 'CIDADAO')")
@Tag(name = "Anexos")
public class AnexoController {

    private final AnexoService anexoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EntityModel<AnexoResponse>> enviar(@PathVariable Long id,
                                                             @RequestParam("arquivo") MultipartFile arquivo) {
        EntityModel<AnexoResponse> modelo = comLinks(id, anexoService.enviar(id, arquivo));
        return ResponseEntity.created(modelo.getRequiredLink("self").toUri()).body(modelo);
    }

    @GetMapping
    public CollectionModel<EntityModel<AnexoResponse>> listar(@PathVariable Long id) {
        return CollectionModel.of(anexoService.listar(id).stream().map(a -> comLinks(id, a)).toList(),
                linkTo(methodOn(AnexoController.class).listar(id)).withSelfRel());
    }

    /** Sempre como download (attachment): o navegador nunca interpreta o arquivo dentro da aplicação. */
    @GetMapping("/{anexoId}")
    public ResponseEntity<byte[]> baixar(@PathVariable Long id, @PathVariable Long anexoId) {
        Anexo anexo = anexoService.baixar(id, anexoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(anexo.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(anexo.getNomeArquivo(), StandardCharsets.UTF_8).build().toString())
                .body(anexo.getConteudo());
    }

    private EntityModel<AnexoResponse> comLinks(Long manifestacaoId, AnexoResponse a) {
        Link download = linkTo(methodOn(AnexoController.class).baixar(manifestacaoId, a.id())).withSelfRel();
        return EntityModel.of(a, download,
                linkTo(methodOn(ManifestacaoController.class).detalhar(manifestacaoId)).withRel("manifestacao"));
    }
}
