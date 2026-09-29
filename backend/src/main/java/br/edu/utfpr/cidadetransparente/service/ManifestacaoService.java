package br.edu.utfpr.cidadetransparente.service;

import br.edu.utfpr.cidadetransparente.config.UsuarioAutenticado;
import br.edu.utfpr.cidadetransparente.domain.*;
import br.edu.utfpr.cidadetransparente.dto.*;
import br.edu.utfpr.cidadetransparente.exception.ApiException;
import br.edu.utfpr.cidadetransparente.repository.*;
import br.edu.utfpr.cidadetransparente.service.FluxoManifestacao.Ator;
import br.edu.utfpr.cidadetransparente.service.FluxoManifestacao.Contexto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import static br.edu.utfpr.cidadetransparente.domain.StatusManifestacao.*;

/**
 * Ciclo de vida da manifestação: abertura, consulta, ações do fluxo, resposta e recurso. As regras de
 * transição ficam no {@link FluxoManifestacao}; aqui ficam carga, visibilidade, gravação e trâmites.
 *
 * Visibilidade (quem enxerga a manifestação; fora disso, 404):
 * OUVIDOR vê todas do município; SERVIDOR, só as encaminhadas à própria secretaria; CIDADAO, só as próprias.
 * O ADMIN não vê manifestações: não precisa delas para gerir a prefeitura (LGPD art. 6º, III).
 */
@Service
@RequiredArgsConstructor
public class ManifestacaoService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Sem 0/O e 1/I/L, que se confundem ao copiar à mão. 12 símbolos de 31: cerca de 2^59 combinações. */
    private static final char[] ALFABETO_CHAVE = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final String INSTANCIA_RECURSAL =
            "Ouvidoria do município, autoridade hierarquicamente superior (Lei 12.527, art. 15, parágrafo único)";

    private final ManifestacaoRepository manifestacaoRepository;
    private final TramiteRepository tramiteRepository;
    private final RespostaRepository respostaRepository;
    private final RecursoRepository recursoRepository;
    private final TipoManifestacaoRepository tipoRepository;
    private final MunicipioRepository municipioRepository;
    private final SecretariaRepository secretariaRepository;
    private final CidadaoRepository cidadaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoManifestacaoService tipoService;
    private final FeriadoService feriadoService;
    private final AuditoriaService auditoria;

    /** Manifestação já liberada para quem está pedindo, junto com quem pede. */
    public record Acesso(Manifestacao manifestacao, Ator ator) {
    }

    /** Detalhe mais o que o controller precisa para montar os links HATEOAS. */
    public record Detalhe(ManifestacaoResponse dados, Set<AcaoManifestacao> acoes, Long ultimaRespostaId,
                          Long recursoId) {
    }

    // ---------------------------------------------------------------- abertura

    /**
     * Com token de CIDADAO, a manifestação é identificada e o município vem do token. Sem token, é anônima:
     * só para tipos que a admitem (a denúncia), e o município vem do corpo, porque quem denuncia não lê dado
     * de ninguém ao escolher a prefeitura. Qualquer outro perfil é recusado: a equipe não abre manifestação.
     */
    @Transactional
    public ManifestacaoCriadaResponse abrir(ManifestacaoRequest request) {
        UsuarioAutenticado usuario = UsuarioAutenticado.atualOuNulo();
        if (usuario != null && !Perfil.CIDADAO.equals(usuario.perfil())) {
            throw ApiException.proibido("Manifestações são abertas pelo cidadão");
        }

        Cidadao cidadao = null;
        Long municipioId;
        if (usuario == null) {
            if (request.municipioId() == null) {
                throw ApiException.requisicaoInvalida("Informe o municipioId da prefeitura destinatária");
            }
            municipioId = request.municipioId();
        } else {
            municipioId = usuario.municipioId();
            cidadao = cidadaoRepository.findByUsuarioId(usuario.id())
                    .orElseThrow(() -> new IllegalStateException("Usuário CIDADAO sem cadastro de cidadão"));
        }
        Municipio municipio = municipioRepository.findById(municipioId)
                .filter(Municipio::isAtivo)
                .orElseThrow(() -> ApiException.requisicaoInvalida("Município inexistente ou desativado"));
        TipoManifestacao tipo = tipoRepository.findById(request.tipoId())
                .orElseThrow(() -> ApiException.requisicaoInvalida("Tipo de manifestação inexistente"));
        if (cidadao == null && !tipo.isPermiteAnonimo()) {
            throw ApiException.requisicaoInvalida(tipo.getNome() + " exige identificação: entre com sua conta "
                    + "(Lei 13.460, art. 10; Lei 12.527, art. 10). Só a denúncia pode ser anônima");
        }

        LocalDate hoje = FluxoManifestacao.hoje();
        PrazoLegal prazo = tipoService.prazoVigente(tipo.getId(), municipio.getId());
        int sequencial = manifestacaoRepository.proximoSequencial(municipio.getId(), hoje.getYear());
        String chave = gerarChave();

        Manifestacao m = new Manifestacao();
        m.setMunicipio(municipio);
        m.setProtocolo("%d-%s-%06d".formatted(hoje.getYear(), municipio.getCodigoIbge(), sequencial));
        m.setTipoManifestacao(tipo);
        m.setCidadao(cidadao);
        m.setAssunto(request.assunto().trim());
        m.setDescricao(request.descricao().trim());
        m.setStatus(RECEBIDA);
        m.setDataAbertura(OffsetDateTime.now());
        m.setDataLimite(FluxoManifestacao.somarDias(hoje, prazo.getDiasResposta(),
                feriadoService.semExpediente(municipio.getId())));
        m.setChaveAcessoHash(hash(chave));
        manifestacaoRepository.save(m);

        Long usuarioId = usuario == null ? null : usuario.id();
        registrarTramite(m, null, RECEBIDA, usuarioId, null,
                cidadao == null ? "Manifestação anônima registrada" : "Manifestação registrada");
        auditoria.registrar(AcaoAuditoria.MANIFESTACAO_REGISTRADA, municipio.getId(), usuarioId,
                "Manifestacao", m.getId(), null);

        return new ManifestacaoCriadaResponse(m.getId(), m.getProtocolo(), chave, RECEBIDA, m.getDataLimite(),
                cidadao == null, "Guarde o protocolo e a chave de acesso: juntos, eles permitem acompanhar a "
                + "manifestação sem login. A chave não é guardada pelo sistema e não pode ser recuperada.");
    }

    // ---------------------------------------------------------------- consulta

    @Transactional(readOnly = true)
    public Page<ManifestacaoResumo> listar(StatusManifestacao status, Long tipoId, Long secretariaId,
                                           LocalDate vencimentoAte, Pageable pageable) {
        Ator ator = atorAtual();
        Long municipioId = UsuarioAutenticado.atual().municipioId();

        Specification<Manifestacao> spec = (root, q, cb) -> cb.equal(root.get("municipio").get("id"), municipioId);
        // Mesma regra de visibilidade do acessar(): a listagem nunca mostra o que o detalhe negaria
        if (Perfil.SERVIDOR.equals(ator.perfil())) {
            spec = spec.and(ator.secretariaId() == null
                    ? (root, q, cb) -> cb.disjunction()
                    : (root, q, cb) -> cb.equal(root.get("secretaria").get("id"), ator.secretariaId()));
        } else if (Perfil.CIDADAO.equals(ator.perfil())) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("cidadao").get("id"), ator.cidadaoId()));
        } else if (!ator.ouvidor()) {
            spec = spec.and((root, q, cb) -> cb.disjunction());
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (tipoId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("tipoManifestacao").get("id"), tipoId));
        }
        if (secretariaId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("secretaria").get("id"), secretariaId));
        }
        if (vencimentoAte != null) {
            // "Perto de vencer" só faz sentido para o que ainda aguarda resposta
            spec = spec.and((root, q, cb) -> cb.and(
                    cb.lessThanOrEqualTo(root.get("dataLimite"), vencimentoAte),
                    root.get("status").in(RECEBIDA, EM_ANALISE, ENCAMINHADA)));
        }

        LocalDate hoje = FluxoManifestacao.hoje();
        return manifestacaoRepository.findAll(spec, pageable).map(m -> {
            boolean aguardando = FluxoManifestacao.aguardandoAdministracao(m.getStatus());
            return new ManifestacaoResumo(m.getId(), m.getProtocolo(), m.getTipoManifestacao().getNome(),
                    m.getAssunto(), m.getStatus(), m.getDataAbertura(), m.getDataLimite(), m.isProrrogada(),
                    m.getSecretaria() == null ? null : m.getSecretaria().getSigla(),
                    aguardando ? ChronoUnit.DAYS.between(hoje, m.getDataLimite()) : null,
                    aguardando && m.getDataLimite().isBefore(hoje));
        });
    }

    /** Não é readOnly: quando o OUVIDOR vê a identidade do manifestante, o acesso é registrado. */
    @Transactional
    public Detalhe detalhar(Long id) {
        Acesso acesso = acessar(id);
        return montarDetalhe(acesso.manifestacao(), acesso.ator(), true);
    }

    @Transactional(readOnly = true)
    public List<TramiteResponse> tramites(Long id) {
        acessar(id);
        return tramitesDe(id, false);
    }

    @Transactional(readOnly = true)
    public List<RespostaResponse> respostas(Long id) {
        Manifestacao m = acessar(id).manifestacao();
        Contexto c = contexto(m);
        return respostaRepository.findByManifestacaoIdOrderByRespondidaEmAscIdAsc(id).stream()
                .map(r -> paraResposta(r, c)).toList();
    }

    @Transactional(readOnly = true)
    public RespostaResponse resposta(Long id, Long respostaId) {
        Manifestacao m = acessar(id).manifestacao();
        Resposta r = respostaRepository.findById(respostaId)
                .filter(x -> x.getManifestacao().getId().equals(id))
                .orElseThrow(() -> ApiException.naoEncontrado("Resposta não encontrada"));
        return paraResposta(r, contexto(m));
    }

    @Transactional(readOnly = true)
    public RecursoResponse recurso(Long recursoId) {
        Recurso recurso = buscarRecurso(recursoId);
        acessar(recurso.getResposta().getManifestacao().getId());
        return RecursoResponse.de(recurso);
    }

    /**
     * Consulta sem login: protocolo mais chave. Protocolo inexistente e chave errada dão a mesma resposta
     * (404), para não confirmar quais protocolos existem. Nenhum dado de identificação sai daqui.
     */
    @Transactional
    public ConsultaPublicaResponse consultaPublica(String protocolo, String chave) {
        Manifestacao m = manifestacaoRepository.findByProtocolo(protocolo)
                .filter(x -> MessageDigest.isEqual(
                        x.getChaveAcessoHash().getBytes(StandardCharsets.UTF_8),
                        hash(chave.trim().toUpperCase()).getBytes(StandardCharsets.UTF_8)))
                .orElseThrow(() -> ApiException.naoEncontrado("Protocolo ou chave de acesso inválidos"));
        Contexto c = contexto(m);
        auditoria.registrar(AcaoAuditoria.CONSULTA_PUBLICA, m.getMunicipio().getId(), null,
                "Manifestacao", m.getId(), null);
        return new ConsultaPublicaResponse(m.getProtocolo(),
                m.getMunicipio().getNome() + "/" + m.getMunicipio().getUf(), m.getTipoManifestacao().getNome(),
                m.getAssunto(), m.getStatus(), m.getDataAbertura(), m.getDataLimite(), m.isProrrogada(),
                tramitesDe(m.getId(), true),
                respostaRepository.findByManifestacaoIdOrderByRespondidaEmAscIdAsc(m.getId()).stream()
                        .map(r -> paraResposta(r, c)).toList(),
                c.recurso() == null ? null : c.recurso().getStatus());
    }

    // ---------------------------------------------------------------- ações da ouvidoria

    @Transactional
    public Detalhe analisar(Long id) {
        Acesso a = acessar(id);
        Contexto c = exigir(AcaoManifestacao.ANALISE, a);
        transitar(c.manifestacao(), EM_ANALISE, a.ator(), null, "Em análise pela ouvidoria");
        return montarDetalhe(c.manifestacao(), a.ator(), false);
    }

    @Transactional
    public Detalhe encaminhar(Long id, EncaminhamentoRequest request) {
        Acesso a = acessar(id);
        Manifestacao m = exigir(AcaoManifestacao.ENCAMINHAMENTO, a).manifestacao();
        Secretaria destino = secretariaRepository.findByIdAndMunicipioId(request.secretariaId(), m.getMunicipio().getId())
                .filter(Secretaria::isAtiva)
                .orElseThrow(() -> ApiException.requisicaoInvalida("Secretaria inexistente ou desativada"));
        if (m.getSecretaria() != null && m.getSecretaria().getId().equals(destino.getId())) {
            throw ApiException.conflito("A manifestação já está com a " + destino.getSigla());
        }
        m.setSecretaria(destino);
        String descricao = (m.getStatus() == ENCAMINHADA ? "Reencaminhada" : "Encaminhada") + " à " + destino.getNome()
                + (request.observacao() == null || request.observacao().isBlank() ? "" : ". " + request.observacao().trim());
        transitar(m, ENCAMINHADA, a.ator(), destino, descricao);
        return montarDetalhe(m, a.ator(), false);
    }

    @Transactional
    public Detalhe prorrogar(Long id, JustificativaRequest request) {
        Acesso a = acessar(id);
        Contexto c = exigir(AcaoManifestacao.PRORROGACAO, a);
        Manifestacao m = c.manifestacao();
        int dias = c.prazo().getDiasProrrogacao();
        m.setDataLimite(FluxoManifestacao.somarDias(m.getDataLimite(), dias, c.semExpediente()));
        m.setProrrogada(true);
        // Mesmo status: a prorrogação não é etapa do fluxo, mas é registrada e fica visível ao requerente,
        // que precisa ser cientificado dela (Lei 12.527, art. 11, § 2º)
        transitar(m, m.getStatus(), a.ator(), null, "Prazo prorrogado por " + dias + " dias, até "
                + m.getDataLimite().format(DATA) + ". Justificativa: " + request.justificativa().trim());
        return montarDetalhe(m, a.ator(), false);
    }

    @Transactional
    public Detalhe arquivar(Long id, JustificativaRequest request) {
        Acesso a = acessar(id);
        Manifestacao m = exigir(AcaoManifestacao.ARQUIVAMENTO, a).manifestacao();
        m.setDataEncerramento(OffsetDateTime.now());
        transitar(m, ARQUIVADA, a.ator(), null, "Arquivada. Justificativa: " + request.justificativa().trim());
        return montarDetalhe(m, a.ator(), false);
    }

    @Transactional
    public Detalhe encerrar(Long id) {
        Acesso a = acessar(id);
        Manifestacao m = exigir(AcaoManifestacao.ENCERRAMENTO, a).manifestacao();
        m.setDataEncerramento(OffsetDateTime.now());
        transitar(m, ENCERRADA, a.ator(), null, "Manifestação encerrada");
        return montarDetalhe(m, a.ator(), false);
    }

    // ---------------------------------------------------------------- resposta e recurso

    @Transactional
    public RespostaResponse responder(Long id, RespostaRequest request) {
        Acesso a = acessar(id);
        Contexto c = exigir(AcaoManifestacao.RESPOSTA, a);
        Manifestacao m = c.manifestacao();
        boolean lai = m.getTipoManifestacao().getCategoria() == CategoriaManifestacao.LAI;
        if (lai && request.resultado() == null) {
            throw ApiException.requisicaoInvalida("Pedido LAI exige o resultado: CONCEDIDO, PARCIALMENTE_CONCEDIDO, "
                    + "NEGADO ou INEXISTENTE (Lei 12.527, art. 11, § 1º)");
        }
        if (!lai && request.resultado() != null) {
            throw ApiException.requisicaoInvalida("Resultado só se aplica a pedido de acesso à informação");
        }

        Resposta resposta = new Resposta();
        resposta.setManifestacao(m);
        resposta.setUsuario(usuarioRepository.getReferenceById(a.ator().usuarioId()));
        resposta.setSecretaria(m.getSecretaria());
        resposta.setTexto(request.texto().trim());
        resposta.setResultadoLai(request.resultado());
        respostaRepository.save(resposta);

        transitar(m, RESPONDIDA, a.ator(), null, "Resposta registrada pela " + m.getSecretaria().getSigla());
        auditoria.registrar(AcaoAuditoria.RESPOSTA_REGISTRADA, m.getMunicipio().getId(), a.ator().usuarioId(),
                "Resposta", resposta.getId(), null);
        return paraResposta(resposta, new Contexto(m, c.prazo(), resposta, c.recurso(), c.hoje(), c.semExpediente()));
    }

    @Transactional
    public RecursoResponse recorrer(Long respostaId, JustificativaRequest request) {
        Resposta resposta = respostaRepository
                .findByIdAndManifestacaoMunicipioId(respostaId, UsuarioAutenticado.atual().municipioId())
                .orElseThrow(() -> ApiException.naoEncontrado("Resposta não encontrada"));
        Acesso a = acessar(resposta.getManifestacao().getId());
        Contexto c = exigir(AcaoManifestacao.RECURSO, a);
        if (!c.ultimaResposta().getId().equals(respostaId)) {
            throw ApiException.conflito("Só cabe recurso contra a resposta mais recente");
        }

        Recurso recurso = new Recurso();
        recurso.setResposta(resposta);
        recurso.setJustificativa(request.justificativa().trim());
        recurso.setDataLimiteJulgamento(FluxoManifestacao.somarDias(c.hoje(), c.prazo().getDiasJulgamentoRecurso(),
                c.semExpediente()));
        recursoRepository.save(recurso);

        transitar(c.manifestacao(), EM_RECURSO, a.ator(), null, "Recurso interposto pelo requerente; julgamento até "
                + recurso.getDataLimiteJulgamento().format(DATA));
        auditoria.registrar(AcaoAuditoria.RECURSO_INTERPOSTO, c.manifestacao().getMunicipio().getId(),
                a.ator().usuarioId(), "Recurso", recurso.getId(), null);
        return RecursoResponse.de(recurso);
    }

    @Transactional
    public RecursoResponse julgar(Long recursoId, JulgamentoRequest request) {
        Recurso recurso = buscarRecurso(recursoId);
        Acesso a = acessar(recurso.getResposta().getManifestacao().getId());
        Contexto c = exigir(AcaoManifestacao.JULGAMENTO, a);
        Manifestacao m = c.manifestacao();

        String descricao;
        switch (request.resultado()) {
            case DEFERIDO -> {
                if (request.prazoCumprimento() == null || !request.prazoCumprimento().isAfter(c.hoje())) {
                    throw ApiException.requisicaoInvalida("Recurso deferido exige prazoCumprimento futuro para a nova resposta");
                }
                // Volta à secretaria que respondeu, com o prazo fixado pela autoridade que julgou
                m.setDataLimite(request.prazoCumprimento());
                descricao = "Recurso deferido: nova resposta até " + m.getDataLimite().format(DATA);
                transitar(m, ENCAMINHADA, a.ator(), m.getSecretaria(), descricao);
            }
            case INDEFERIDO -> {
                m.setDataEncerramento(OffsetDateTime.now());
                transitar(m, ENCERRADA, a.ator(), null, "Recurso indeferido");
            }
            default -> throw ApiException.requisicaoInvalida("Resultado do julgamento deve ser DEFERIDO ou INDEFERIDO");
        }
        recurso.setStatus(request.resultado());
        recurso.setDecisao(request.decisao().trim());
        recurso.setJulgadoPor(usuarioRepository.getReferenceById(a.ator().usuarioId()));
        recurso.setJulgadoEm(OffsetDateTime.now());
        auditoria.registrar(AcaoAuditoria.RECURSO_JULGADO, m.getMunicipio().getId(), a.ator().usuarioId(),
                "Recurso", recurso.getId(), request.resultado().name());
        return RecursoResponse.de(recurso);
    }

    // ---------------------------------------------------------------- acesso e regras (também usados por anexos)

    /** Carrega a manifestação só se quem pede pode vê-la. Fora do município ou sem permissão: 404. */
    public Acesso acessar(Long id) {
        Ator ator = atorAtual();
        Manifestacao m = manifestacaoRepository.findByIdAndMunicipioId(id, UsuarioAutenticado.atual().municipioId())
                .filter(x -> ator.ouvidor() || ator.servidorDa(x) || ator.donoDe(x))
                .orElseThrow(() -> ApiException.naoEncontrado("Manifestação não encontrada"));
        return new Acesso(m, ator);
    }

    /** Carrega o contexto e exige que a ação seja permitida; senão, 409 com o motivo. */
    public Contexto exigir(AcaoManifestacao acao, Acesso acesso) {
        Contexto c = contexto(acesso.manifestacao());
        FluxoManifestacao.impedimento(acao, c, acesso.ator()).ifPresent(motivo -> {
            throw ApiException.conflito(motivo);
        });
        return c;
    }

    // ---------------------------------------------------------------- auxiliares

    private Ator atorAtual() {
        UsuarioAutenticado u = UsuarioAutenticado.atual();
        Long secretariaId = Perfil.SERVIDOR.equals(u.perfil())
                ? usuarioRepository.findById(u.id()).map(Usuario::getSecretaria).map(Secretaria::getId).orElse(null)
                : null;
        Long cidadaoId = Perfil.CIDADAO.equals(u.perfil())
                ? cidadaoRepository.findByUsuarioId(u.id()).map(Cidadao::getId).orElse(null)
                : null;
        return new Ator(u.id(), u.perfil(), secretariaId, cidadaoId);
    }

    private Contexto contexto(Manifestacao m) {
        return new Contexto(m,
                tipoService.prazoVigente(m.getTipoManifestacao().getId(), m.getMunicipio().getId()),
                respostaRepository.findFirstByManifestacaoIdOrderByRespondidaEmDescIdDesc(m.getId()).orElse(null),
                recursoRepository.findByRespostaManifestacaoId(m.getId()).orElse(null),
                FluxoManifestacao.hoje(),
                feriadoService.semExpediente(m.getMunicipio().getId()));
    }

    private void transitar(Manifestacao m, StatusManifestacao novo, Ator ator, Secretaria destino, String descricao) {
        StatusManifestacao anterior = m.getStatus();
        m.setStatus(novo);
        registrarTramite(m, anterior, novo, ator.usuarioId(), destino, descricao);
        auditoria.registrar(AcaoAuditoria.TRANSICAO, m.getMunicipio().getId(), ator.usuarioId(),
                "Manifestacao", m.getId(), anterior + " -> " + novo);
    }

    private void registrarTramite(Manifestacao m, StatusManifestacao anterior, StatusManifestacao novo,
                                  Long usuarioId, Secretaria destino, String descricao) {
        Tramite t = new Tramite();
        t.setManifestacao(m);
        t.setStatusAnterior(anterior);
        t.setStatusNovo(novo);
        t.setUsuario(usuarioId == null ? null : usuarioRepository.getReferenceById(usuarioId));
        t.setSecretariaDestino(destino);
        t.setDescricao(descricao);
        tramiteRepository.save(t);
    }

    /**
     * Monta o detalhe respeitando o sigilo da identidade: só o OUVIDOR e o próprio cidadão a veem. Quando o
     * OUVIDOR a vê, o acesso é registrado (Decreto 10.153, art. 6º, § 3º).
     */
    private Detalhe montarDetalhe(Manifestacao m, Ator ator, boolean registrarAcessoIdentidade) {
        Contexto c = contexto(m);
        boolean podeVerIdentidade = ator.ouvidor() || ator.donoDe(m);
        Cidadao cidadao = m.getCidadao();
        ManifestacaoResponse.Manifestante manifestante = podeVerIdentidade && cidadao != null
                ? new ManifestacaoResponse.Manifestante(cidadao.getNome(), cidadao.getEmail(), cidadao.getCpf(),
                cidadao.getTelefone())
                : null;
        if (registrarAcessoIdentidade && ator.ouvidor() && cidadao != null) {
            auditoria.registrar(AcaoAuditoria.IDENTIDADE_ACESSADA, m.getMunicipio().getId(), ator.usuarioId(),
                    "Manifestacao", m.getId(), null);
        }

        boolean aguardando = FluxoManifestacao.aguardandoAdministracao(m.getStatus());
        Secretaria s = m.getSecretaria();
        TipoManifestacao t = m.getTipoManifestacao();
        ManifestacaoResponse dados = new ManifestacaoResponse(m.getId(), m.getProtocolo(),
                new ManifestacaoResponse.Tipo(t.getId(), t.getCodigo(), t.getNome(), t.getCategoria()),
                m.getAssunto(), m.getDescricao(), m.getStatus(), m.getDataAbertura(), m.getDataLimite(),
                m.isProrrogada(),
                aguardando ? ChronoUnit.DAYS.between(c.hoje(), m.getDataLimite()) : null,
                aguardando ? m.getDataLimite().isBefore(c.hoje()) : null,
                m.getDataEncerramento(),
                s == null ? null : new ManifestacaoResponse.Secretaria(s.getId(), s.getSigla(), s.getNome()),
                podeVerIdentidade ? cidadao == null : null,
                manifestante,
                c.recurso() == null ? null : RecursoResponse.de(c.recurso()));
        return new Detalhe(dados, FluxoManifestacao.disponiveis(c, ator),
                c.ultimaResposta() == null ? null : c.ultimaResposta().getId(),
                c.recurso() == null ? null : c.recurso().getId());
    }

    /**
     * Histórico. Ação de cidadão aparece como "Manifestante" (a secretaria também lê o histórico); na
     * consulta pública, nenhum responsável é nomeado.
     */
    private List<TramiteResponse> tramitesDe(Long manifestacaoId, boolean publico) {
        return tramiteRepository.findByManifestacaoIdOrderByRegistradoEmAscIdAsc(manifestacaoId).stream()
                .map(t -> {
                    Usuario u = t.getUsuario();
                    String responsavel = publico ? null
                            : u == null || Perfil.CIDADAO.equals(u.getPerfil().getNome()) ? "Manifestante"
                            : u.getNome() + " (" + u.getPerfil().getNome() + ")";
                    return new TramiteResponse(t.getStatusAnterior(), t.getStatusNovo(), t.getDescricao(),
                            t.getSecretariaDestino() == null ? null : t.getSecretariaDestino().getSigla(),
                            responsavel, t.getRegistradoEm());
                }).toList();
    }

    /** Em resposta LAI que nega acesso, informa prazo e instância do recurso (Lei 12.527, art. 11, § 4º). */
    private RespostaResponse paraResposta(Resposta r, Contexto c) {
        boolean cabivel = c.manifestacao().getTipoManifestacao().isPermiteRecurso()
                && r.getResultadoLai() != null && r.getResultadoLai().admiteRecurso()
                && (c.recurso() == null || c.recurso().getResposta().getId().equals(r.getId()));
        return new RespostaResponse(r.getId(), r.getTexto(), r.getResultadoLai(),
                r.getSecretaria().getSigla(), r.getRespondidaEm(), cabivel,
                cabivel ? FluxoManifestacao.prazoRecurso(r, c.prazo(), c.semExpediente()) : null,
                cabivel ? INSTANCIA_RECURSAL : null);
    }

    private Recurso buscarRecurso(Long recursoId) {
        return recursoRepository.findByIdAndRespostaManifestacaoMunicipioId(recursoId,
                        UsuarioAutenticado.atual().municipioId())
                .orElseThrow(() -> ApiException.naoEncontrado("Recurso não encontrado"));
    }

    private static String gerarChave() {
        StringBuilder chave = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            if (i > 0 && i % 4 == 0) {
                chave.append('-');
            }
            chave.append(ALFABETO_CHAVE[ALEATORIO.nextInt(ALFABETO_CHAVE.length)]);
        }
        return chave.toString();
    }

    /** SHA-256 basta aqui (e não BCrypt): a chave é aleatória e longa, não há dicionário para testar. */
    private static String hash(String chave) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(chave.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
