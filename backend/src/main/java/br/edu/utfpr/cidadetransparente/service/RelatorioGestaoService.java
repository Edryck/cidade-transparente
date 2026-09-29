package br.edu.utfpr.cidadetransparente.service;

import br.edu.utfpr.cidadetransparente.config.UsuarioAutenticado;
import br.edu.utfpr.cidadetransparente.domain.*;
import br.edu.utfpr.cidadetransparente.domain.EstatisticasGestao.Contagem;
import br.edu.utfpr.cidadetransparente.dto.RelatorioGestaoRequest;
import br.edu.utfpr.cidadetransparente.dto.RelatorioGestaoResponse;
import br.edu.utfpr.cidadetransparente.dto.RelatorioGestaoResponse.Situacao;
import br.edu.utfpr.cidadetransparente.exception.ApiException;
import br.edu.utfpr.cidadetransparente.repository.MunicipioRepository;
import br.edu.utfpr.cidadetransparente.repository.RelatorioGestaoRepository;
import br.edu.utfpr.cidadetransparente.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Relatório anual de gestão (Lei 13.460, arts. 14, II, e 15; LAI art. 30, III). A ouvidoria escreve a análise
 * e as providências; o sistema calcula os números. Na publicação os números são congelados e o relatório
 * fica disponível na internet sem login (art. 15, parágrafo único, II), sem nenhum dado pessoal.
 */
@Service
@RequiredArgsConstructor
public class RelatorioGestaoService {

    private final RelatorioGestaoRepository relatorioRepository;
    private final MunicipioRepository municipioRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoria;

    /** Relatório e, se for o caso, o motivo pelo qual ainda não pode ser publicado (vira ou não link). */
    public record Visao(RelatorioGestaoResponse dados, Long municipioId, String impedimentoPublicacao) {
    }

    /** Prévia para a equipe: rascunho com números do momento, ou o relatório já publicado. */
    @Transactional(readOnly = true)
    public Visao buscar(int ano) {
        validarAno(ano);
        Municipio m = municipioRepository.getReferenceById(UsuarioAutenticado.atual().municipioId());
        RelatorioGestao r = relatorioRepository.findByMunicipioIdAndAno(m.getId(), ano).orElse(null);
        return visao(m, ano, r);
    }

    @Transactional
    public Visao salvarRascunho(int ano, RelatorioGestaoRequest request) {
        validarAno(ano);
        Municipio m = municipioRepository.getReferenceById(UsuarioAutenticado.atual().municipioId());
        RelatorioGestao r = relatorioRepository.findByMunicipioIdAndAno(m.getId(), ano).orElseGet(() -> {
            RelatorioGestao novo = new RelatorioGestao();
            novo.setMunicipio(m);
            novo.setAno(ano);
            return novo;
        });
        if (r.isPublicado()) {
            throw ApiException.conflito("Relatório de " + ano + " já publicado: documento oficial não é alterado");
        }
        r.setAnalisePontosRecorrentes(textoOuNulo(request.analisePontosRecorrentes()));
        r.setProvidenciasAdotadas(textoOuNulo(request.providenciasAdotadas()));
        relatorioRepository.save(r);
        return visao(m, ano, r);
    }

    @Transactional
    public Visao publicar(int ano) {
        validarAno(ano);
        UsuarioAutenticado u = UsuarioAutenticado.atual();
        Municipio m = municipioRepository.getReferenceById(u.municipioId());
        RelatorioGestao r = relatorioRepository.findByMunicipioIdAndAno(m.getId(), ano).orElse(null);
        String impedimento = impedimentoPublicacao(ano, r);
        if (impedimento != null) {
            throw ApiException.conflito(impedimento);
        }
        r.setEstatisticas(calcular(m.getId(), ano));
        r.setPublicadoEm(OffsetDateTime.now());
        r.setPublicadoPor(usuarioRepository.getReferenceById(u.id()));
        auditoria.registrar(AcaoAuditoria.RELATORIO_PUBLICADO, m.getId(), u.id(), "RelatorioGestao", r.getId(),
                String.valueOf(ano));
        return visao(m, ano, r);
    }

    /** Pública: anos com relatório publicado. */
    @Transactional(readOnly = true)
    public List<RelatorioGestaoResponse.Publicado> publicados(Long municipioId) {
        municipioExistente(municipioId);
        return relatorioRepository.findByMunicipioIdAndPublicadoEmIsNotNullOrderByAnoDesc(municipioId).stream()
                .map(r -> new RelatorioGestaoResponse.Publicado(r.getAno(), r.getPublicadoEm())).toList();
    }

    /** Pública: só o relatório publicado. Rascunho não é documento oficial e responde 404. */
    @Transactional(readOnly = true)
    public RelatorioGestaoResponse publicado(Long municipioId, int ano) {
        Municipio m = municipioExistente(municipioId);
        RelatorioGestao r = relatorioRepository.findByMunicipioIdAndAno(municipioId, ano)
                .filter(RelatorioGestao::isPublicado)
                .orElseThrow(() -> ApiException.naoEncontrado("Não há relatório publicado para " + ano));
        return visao(m, ano, r).dados();
    }

    /** Fonte única da regra: valida a publicação (409) e decide se o link "publicacao" aparece. */
    private static String impedimentoPublicacao(int ano, RelatorioGestao r) {
        if (r != null && r.isPublicado()) {
            return "Relatório de " + ano + " já publicado";
        }
        if (ano >= FluxoManifestacao.hoje().getYear()) {
            return "O relatório consolida o ano encerrado (Lei 13.460, art. 15, I): " + ano + " ainda não terminou";
        }
        if (r == null || r.getAnalisePontosRecorrentes() == null || r.getProvidenciasAdotadas() == null) {
            return "Preencha a análise dos pontos recorrentes e as providências adotadas (Lei 13.460, art. 15, III e IV)";
        }
        return null;
    }

    private Visao visao(Municipio m, int ano, RelatorioGestao r) {
        boolean publicado = r != null && r.isPublicado();
        RelatorioGestaoResponse dados = new RelatorioGestaoResponse(m.getNome() + "/" + m.getUf(), ano,
                publicado ? Situacao.PUBLICADO : Situacao.RASCUNHO,
                publicado ? r.getEstatisticas() : calcular(m.getId(), ano),
                r == null ? null : r.getAnalisePontosRecorrentes(),
                r == null ? null : r.getProvidenciasAdotadas(),
                publicado ? r.getPublicadoEm() : null);
        return new Visao(dados, m.getId(), impedimentoPublicacao(ano, r));
    }

    /** Números do ano, pela data de abertura (fuso de Brasília). Só contagens: nenhum dado pessoal sai daqui. */
    private EstatisticasGestao calcular(Long municipioId, int ano) {
        OffsetDateTime inicio = LocalDate.of(ano, 1, 1).atStartOfDay(FluxoManifestacao.FUSO).toOffsetDateTime();
        OffsetDateTime fim = LocalDate.of(ano + 1, 1, 1).atStartOfDay(FluxoManifestacao.FUSO).toOffsetDateTime();
        List<Object[]> manifestacoes = relatorioRepository.manifestacoesDoPeriodo(municipioId, inicio, fim);
        List<Object[]> respostas = relatorioRepository.respostasDoPeriodo(municipioId, inicio, fim);
        List<Object> recursos = relatorioRepository.statusDosRecursosDoPeriodo(municipioId, inicio, fim);

        // Primeira e última resposta de cada manifestação (a consulta já vem em ordem cronológica)
        Map<Long, OffsetDateTime> primeiraResposta = new HashMap<>();
        Map<Long, ResultadoLai> ultimoResultado = new HashMap<>();
        for (Object[] r : respostas) {
            primeiraResposta.putIfAbsent((Long) r[0], (OffsetDateTime) r[1]);
            ultimoResultado.put((Long) r[0], (ResultadoLai) r[2]);
        }

        LocalDate hoje = FluxoManifestacao.hoje();
        long anonimas = 0, prorrogadas = 0, noPrazo = 0, foraDoPrazo = 0, vencidas = 0, somaDias = 0;
        long laiRecebidos = 0, laiSemResposta = 0;
        Set<Long> solicitantesLai = new HashSet<>();
        Map<ResultadoLai, Long> resultadosLai = new EnumMap<>(ResultadoLai.class);
        for (Object[] m : manifestacoes) {
            Long id = (Long) m[0];
            StatusManifestacao status = (StatusManifestacao) m[4];
            LocalDate abertura = ((OffsetDateTime) m[7]).atZoneSameInstant(FluxoManifestacao.FUSO).toLocalDate();
            LocalDate limite = (LocalDate) m[8];
            if (m[5] == null) anonimas++;
            if ((Boolean) m[6]) prorrogadas++;

            OffsetDateTime resposta = primeiraResposta.get(id);
            if (resposta != null) {
                LocalDate dia = resposta.atZoneSameInstant(FluxoManifestacao.FUSO).toLocalDate();
                if (dia.isAfter(limite)) foraDoPrazo++; else noPrazo++;
                somaDias += ChronoUnit.DAYS.between(abertura, dia);
            } else if (FluxoManifestacao.aguardandoAdministracao(status) && limite.isBefore(hoje)) {
                vencidas++;
            }

            if (m[2] == CategoriaManifestacao.LAI) {
                laiRecebidos++;
                if (m[5] != null) solicitantesLai.add((Long) m[5]);
                ResultadoLai resultado = ultimoResultado.get(id);
                if (resultado == null) laiSemResposta++; else resultadosLai.merge(resultado, 1L, Long::sum);
            }
        }

        long respondidas = noPrazo + foraDoPrazo;
        EstatisticasGestao.Prazos prazos = new EstatisticasGestao.Prazos(respondidas, noPrazo, foraDoPrazo,
                respondidas == 0 ? null : Math.round(1000.0 * noPrazo / respondidas) / 10.0, vencidas,
                respondidas == 0 ? null : Math.round(10.0 * somaDias / respondidas) / 10.0);
        EstatisticasGestao.Lai lai = new EstatisticasGestao.Lai(laiRecebidos, solicitantesLai.size(),
                resultadosLai.getOrDefault(ResultadoLai.CONCEDIDO, 0L),
                resultadosLai.getOrDefault(ResultadoLai.PARCIALMENTE_CONCEDIDO, 0L),
                resultadosLai.getOrDefault(ResultadoLai.NEGADO, 0L),
                resultadosLai.getOrDefault(ResultadoLai.INEXISTENTE, 0L),
                laiSemResposta, recursos.size(),
                recursos.stream().filter(s -> s == StatusRecurso.DEFERIDO).count(),
                recursos.stream().filter(s -> s == StatusRecurso.INDEFERIDO).count());

        return new EstatisticasGestao(OffsetDateTime.now(), manifestacoes.size(), anonimas, prorrogadas,
                contar(manifestacoes, m -> (String) m[1]),
                contar(manifestacoes.stream().filter(m -> m[3] != null).toList(), m -> (String) m[3]),
                contar(manifestacoes, m -> m[4].toString()),
                prazos, lai);
    }

    private static List<Contagem> contar(List<Object[]> linhas, Function<Object[], String> chave) {
        return linhas.stream()
                .collect(Collectors.groupingBy(chave, TreeMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new Contagem(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(Contagem::total).reversed())
                .toList();
    }

    private Municipio municipioExistente(Long municipioId) {
        return municipioRepository.findById(municipioId)
                .orElseThrow(() -> ApiException.naoEncontrado("Município não encontrado"));
    }

    private static void validarAno(int ano) {
        if (ano < 2000 || ano > FluxoManifestacao.hoje().getYear()) {
            throw ApiException.requisicaoInvalida("Ano deve estar entre 2000 e o ano atual");
        }
    }

    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
