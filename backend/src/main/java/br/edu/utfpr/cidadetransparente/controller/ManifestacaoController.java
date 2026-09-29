package br.edu.utfpr.cidadetransparente.controller;

import br.edu.utfpr.cidadetransparente.domain.StatusManifestacao;
import br.edu.utfpr.cidadetransparente.dto.*;
import br.edu.utfpr.cidadetransparente.service.ManifestacaoService;
import br.edu.utfpr.cidadetransparente.service.ManifestacaoService.Detalhe;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Manifestações. Nível 3 de Richardson: os links de ação de cada manifestação vêm do FluxoManifestacao e
 * mudam com o estado, o perfil e o prazo. ENCERRADA e ARQUIVADA não trazem link de ação nenhum.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Manifestações")
public class ManifestacaoController {

    private static final String LEITORES = "hasAnyRole('OUVIDOR', 'SERVIDOR', 'CIDADAO')";

    private final ManifestacaoService service;

    /** Com token de CIDADAO: identificada. Sem token: anônima, só para tipos que admitem (denúncia). */
    @PostMapping("/manifestacoes")
    @SecurityRequirements
    public ResponseEntity<EntityModel<ManifestacaoCriadaResponse>> abrir(@Valid @RequestBody ManifestacaoRequest request) {
        ManifestacaoCriadaResponse criada = service.abrir(request);
        Link self = linkTo(methodOn(ManifestacaoController.class).detalhar(criada.id())).withSelfRel();
        // linkTo(Classe).slash em vez de methodOn: consultar() devolve um record (final), que não aceita proxy
        EntityModel<ManifestacaoCriadaResponse> modelo = EntityModel.of(criada, self,
                linkTo(ProtocoloController.class).slash(criada.protocolo()).withRel("consulta-publica"));
        return ResponseEntity.created(self.toUri()).body(modelo);
    }

    /** Filtros opcionais; {@code vencimentoAte} destaca o que está perto de vencer. Ordenação padrão: prazo. */
    @GetMapping("/manifestacoes")
    @PreAuthorize(LEITORES)
    public PagedModel<EntityModel<ManifestacaoResumo>> listar(
            @RequestParam(required = false) StatusManifestacao status,
            @RequestParam(required = false) Long tipoId,
            @RequestParam(required = false) Long secretariaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimentoAte,
            @PageableDefault(size = 20, sort = "dataLimite", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<ManifestacaoResumo> assembler) {
        return assembler.toModel(service.listar(status, tipoId, secretariaId, vencimentoAte, pageable),
                r -> EntityModel.of(r, linkTo(methodOn(ManifestacaoController.class).detalhar(r.id())).withSelfRel()));
    }

    @GetMapping("/manifestacoes/{id}")
    @PreAuthorize(LEITORES)
    public EntityModel<ManifestacaoResponse> detalhar(@PathVariable Long id) {
        return comLinks(service.detalhar(id));
    }

    @GetMapping("/manifestacoes/{id}/tramites")
    @PreAuthorize(LEITORES)
    public CollectionModel<TramiteResponse> tramites(@PathVariable Long id) {
        return CollectionModel.of(service.tramites(id),
                linkTo(methodOn(ManifestacaoController.class).tramites(id)).withSelfRel(),
                linkTo(methodOn(ManifestacaoController.class).detalhar(id)).withRel("manifestacao"));
    }

    // ------------------------------------------------ ações (substantivos: a ação é um recurso criado)

    @PostMapping("/manifestacoes/{id}/analise")
    @PreAuthorize("hasRole('OUVIDOR')")
    public EntityModel<ManifestacaoResponse> analisar(@PathVariable Long id) {
        return comLinks(service.analisar(id));
    }

    @PostMapping("/manifestacoes/{id}/encaminhamento")
    @PreAuthorize("hasRole('OUVIDOR')")
    public EntityModel<ManifestacaoResponse> encaminhar(@PathVariable Long id,
                                                        @Valid @RequestBody EncaminhamentoRequest request) {
        return comLinks(service.encaminhar(id, request));
    }

    @PostMapping("/manifestacoes/{id}/prorrogacao")
    @PreAuthorize("hasRole('OUVIDOR')")
    public EntityModel<ManifestacaoResponse> prorrogar(@PathVariable Long id,
                                                       @Valid @RequestBody JustificativaRequest request) {
        return comLinks(service.prorrogar(id, request));
    }

    @PostMapping("/manifestacoes/{id}/arquivamento")
    @PreAuthorize("hasRole('OUVIDOR')")
    public EntityModel<ManifestacaoResponse> arquivar(@PathVariable Long id,
                                                      @Valid @RequestBody JustificativaRequest request) {
        return comLinks(service.arquivar(id, request));
    }

    @PostMapping("/manifestacoes/{id}/encerramento")
    @PreAuthorize("hasRole('OUVIDOR')")
    public EntityModel<ManifestacaoResponse> encerrar(@PathVariable Long id) {
        return comLinks(service.encerrar(id));
    }

    // ------------------------------------------------ respostas e recursos

    @PostMapping("/manifestacoes/{id}/respostas")
    @PreAuthorize("hasRole('SERVIDOR')")
    public ResponseEntity<EntityModel<RespostaResponse>> responder(@PathVariable Long id,
                                                                   @Valid @RequestBody RespostaRequest request) {
        EntityModel<RespostaResponse> modelo = comLinks(id, service.responder(id, request));
        return ResponseEntity.created(modelo.getRequiredLink("self").toUri()).body(modelo);
    }

    @GetMapping("/manifestacoes/{id}/respostas")
    @PreAuthorize(LEITORES)
    public CollectionModel<EntityModel<RespostaResponse>> respostas(@PathVariable Long id) {
        return CollectionModel.of(service.respostas(id).stream().map(r -> comLinks(id, r)).toList(),
                linkTo(methodOn(ManifestacaoController.class).respostas(id)).withSelfRel());
    }

    @GetMapping("/manifestacoes/{id}/respostas/{respostaId}")
    @PreAuthorize(LEITORES)
    public EntityModel<RespostaResponse> resposta(@PathVariable Long id, @PathVariable Long respostaId) {
        return comLinks(id, service.resposta(id, respostaId));
    }

    @PostMapping("/respostas/{id}/recursos")
    @PreAuthorize("hasRole('CIDADAO')")
    public ResponseEntity<EntityModel<RecursoResponse>> recorrer(@PathVariable Long id,
                                                                 @Valid @RequestBody JustificativaRequest request) {
        RecursoResponse recurso = service.recorrer(id, request);
        URI local = linkTo(methodOn(ManifestacaoController.class).recurso(recurso.id())).toUri();
        return ResponseEntity.created(local).body(EntityModel.of(recurso,
                linkTo(methodOn(ManifestacaoController.class).recurso(recurso.id())).withSelfRel()));
    }

    @GetMapping("/recursos/{id}")
    @PreAuthorize(LEITORES)
    public EntityModel<RecursoResponse> recurso(@PathVariable Long id) {
        return EntityModel.of(service.recurso(id),
                linkTo(methodOn(ManifestacaoController.class).recurso(id)).withSelfRel());
    }

    /** Julgamento pela autoridade superior: aqui, a ouvidoria (Lei 12.527, art. 15, parágrafo único). */
    @PutMapping("/recursos/{id}")
    @PreAuthorize("hasRole('OUVIDOR')")
    public EntityModel<RecursoResponse> julgar(@PathVariable Long id, @Valid @RequestBody JulgamentoRequest request) {
        return EntityModel.of(service.julgar(id, request),
                linkTo(methodOn(ManifestacaoController.class).recurso(id)).withSelfRel());
    }

    // ------------------------------------------------ links

    /** Cada ação disponível vira um link com o nome que o frontend usa para mostrar o botão. */
    private EntityModel<ManifestacaoResponse> comLinks(Detalhe d) {
        Long id = d.dados().id();
        ManifestacaoController c = methodOn(ManifestacaoController.class);
        EntityModel<ManifestacaoResponse> modelo = EntityModel.of(d.dados(),
                linkTo(c.detalhar(id)).withSelfRel(),
                linkTo(methodOn(ManifestacaoController.class).tramites(id)).withRel("tramites"),
                linkTo(methodOn(ManifestacaoController.class).respostas(id)).withRel("respostas"),
                linkTo(methodOn(AnexoController.class).listar(id)).withRel("anexos"));
        for (var acao : d.acoes()) {
            ManifestacaoController m = methodOn(ManifestacaoController.class);
            Link link = switch (acao) {
                case ANALISE -> linkTo(m.analisar(id)).withRel(acao.rel());
                case ENCAMINHAMENTO -> linkTo(m.encaminhar(id, null)).withRel(acao.rel());
                case PRORROGACAO -> linkTo(m.prorrogar(id, null)).withRel(acao.rel());
                case ARQUIVAMENTO -> linkTo(m.arquivar(id, null)).withRel(acao.rel());
                case ENCERRAMENTO -> linkTo(m.encerrar(id)).withRel(acao.rel());
                case RESPOSTA -> linkTo(m.responder(id, null)).withRel(acao.rel());
                case RECURSO -> linkTo(m.recorrer(d.ultimaRespostaId(), null)).withRel(acao.rel());
                case JULGAMENTO -> linkTo(m.julgar(d.recursoId(), null)).withRel(acao.rel());
                case ANEXO -> linkTo(methodOn(AnexoController.class).enviar(id, null)).withRel(acao.rel());
            };
            modelo.add(link);
        }
        return modelo;
    }

    private EntityModel<RespostaResponse> comLinks(Long manifestacaoId, RespostaResponse r) {
        return EntityModel.of(r,
                linkTo(methodOn(ManifestacaoController.class).resposta(manifestacaoId, r.id())).withSelfRel(),
                linkTo(methodOn(ManifestacaoController.class).detalhar(manifestacaoId)).withRel("manifestacao"));
    }
}
