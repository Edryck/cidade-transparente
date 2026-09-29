package br.edu.utfpr.cidadetransparente.service;

import br.edu.utfpr.cidadetransparente.domain.AcaoManifestacao;
import br.edu.utfpr.cidadetransparente.domain.Manifestacao;
import br.edu.utfpr.cidadetransparente.domain.Perfil;
import br.edu.utfpr.cidadetransparente.domain.PrazoLegal;
import br.edu.utfpr.cidadetransparente.domain.Recurso;
import br.edu.utfpr.cidadetransparente.domain.Resposta;
import br.edu.utfpr.cidadetransparente.domain.StatusManifestacao;
import br.edu.utfpr.cidadetransparente.domain.StatusRecurso;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static br.edu.utfpr.cidadetransparente.domain.StatusManifestacao.*;

/**
 * Máquina de estados da manifestação. {@link #impedimento} é a fonte única da regra: a mesma função valida a
 * ação (409 com o motivo) e decide quais links HATEOAS a resposta oferece. O cliente nunca vê um link para
 * uma ação que a API recusaria, e nunca precisa de "if status == X" no frontend.
 */
public final class FluxoManifestacao {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Estados em que a administração deve uma resposta: só neles o prazo corre e pode ser prorrogado. */
    private static final Set<StatusManifestacao> AGUARDANDO_ADMINISTRACAO = EnumSet.of(RECEBIDA, EM_ANALISE, ENCAMINHADA);

    /** Quem está agindo. Nulo para quem não tem conta (manifestação anônima). */
    public record Ator(Long usuarioId, String perfil, Long secretariaId, Long cidadaoId) {

        boolean ouvidor() {
            return Perfil.OUVIDOR.equals(perfil);
        }

        boolean servidorDa(Manifestacao m) {
            return Perfil.SERVIDOR.equals(perfil) && m.getSecretaria() != null
                    && m.getSecretaria().getId().equals(secretariaId);
        }

        boolean donoDe(Manifestacao m) {
            return Perfil.CIDADAO.equals(perfil) && m.getCidadao() != null && m.getCidadao().getId().equals(cidadaoId);
        }
    }

    /** Tudo que as regras consultam, carregado uma vez pelo service. */
    public record Contexto(Manifestacao manifestacao, PrazoLegal prazo, Resposta ultimaResposta, Recurso recurso,
                           LocalDate hoje) {
    }

    private FluxoManifestacao() {
    }

    /** Motivo pelo qual a ação não pode ser feita agora, ou vazio se pode. */
    public static Optional<String> impedimento(AcaoManifestacao acao, Contexto c, Ator ator) {
        Manifestacao m = c.manifestacao();
        StatusManifestacao s = m.getStatus();
        String motivo = switch (acao) {
            case ANALISE -> !ator.ouvidor() ? "Ação exclusiva da ouvidoria"
                    : s != RECEBIDA ? "Só manifestação RECEBIDA entra em análise" : null;
            case ENCAMINHAMENTO -> !ator.ouvidor() ? "Ação exclusiva da ouvidoria"
                    : s != EM_ANALISE && s != ENCAMINHADA
                    ? "Só manifestação EM_ANALISE ou ENCAMINHADA pode ser (re)encaminhada" : null;
            case PRORROGACAO -> impedimentoProrrogacao(c, ator);
            case ARQUIVAMENTO -> !ator.ouvidor() ? "Ação exclusiva da ouvidoria"
                    : s != RECEBIDA && s != EM_ANALISE ? "Só manifestação RECEBIDA ou EM_ANALISE pode ser arquivada" : null;
            case RESPOSTA -> !ator.servidorDa(m) ? "Só a secretaria para a qual a manifestação foi encaminhada responde"
                    : s != ENCAMINHADA ? "Só manifestação ENCAMINHADA pode ser respondida" : null;
            case RECURSO -> impedimentoRecurso(c, ator);
            case JULGAMENTO -> !ator.ouvidor() ? "Ação exclusiva da ouvidoria"
                    : s != EM_RECURSO || c.recurso() == null || c.recurso().getStatus() != StatusRecurso.PENDENTE
                    ? "Não há recurso pendente de julgamento" : null;
            case ENCERRAMENTO -> impedimentoEncerramento(c, ator);
            case ANEXO -> !(ator.ouvidor() || ator.servidorDa(m) || ator.donoDe(m)) ? "Sem permissão para anexar"
                    : s == ENCERRADA || s == ARQUIVADA ? "Manifestação finalizada não recebe anexos" : null;
        };
        return Optional.ofNullable(motivo);
    }

    public static Set<AcaoManifestacao> disponiveis(Contexto c, Ator ator) {
        Set<AcaoManifestacao> acoes = EnumSet.noneOf(AcaoManifestacao.class);
        if (ator == null) {
            return acoes;
        }
        for (AcaoManifestacao acao : AcaoManifestacao.values()) {
            if (impedimento(acao, c, ator).isEmpty()) {
                acoes.add(acao);
            }
        }
        return acoes;
    }

    /** Prorrogação única, justificada e antes do vencimento (Lei 13.460, art. 16; Lei 12.527, art. 11, § 2º). */
    private static String impedimentoProrrogacao(Contexto c, Ator ator) {
        Manifestacao m = c.manifestacao();
        if (!ator.ouvidor()) {
            return "Ação exclusiva da ouvidoria";
        }
        if (!AGUARDANDO_ADMINISTRACAO.contains(m.getStatus())) {
            return "Só se prorroga prazo de manifestação que aguarda resposta";
        }
        if (m.isProrrogada()) {
            return "O prazo já foi prorrogado: a lei permite uma única prorrogação";
        }
        if (!c.prazo().isPermiteProrrogacao()) {
            return "A regra de prazo deste tipo não permite prorrogação";
        }
        if (m.getDataLimite().isBefore(c.hoje())) {
            return "Prazo vencido em " + m.getDataLimite().format(DATA) + ": não se prorroga prazo já vencido";
        }
        return null;
    }

    /**
     * Recurso (Lei 12.527, art. 15): só em tipo com fase recursal, contra indeferimento de acesso, pelo próprio
     * requerente, dentro do prazo e em instância única.
     */
    private static String impedimentoRecurso(Contexto c, Ator ator) {
        Manifestacao m = c.manifestacao();
        Resposta r = c.ultimaResposta();
        if (!ator.donoDe(m)) {
            return "Só o próprio requerente pode recorrer";
        }
        if (m.getStatus() != RESPONDIDA || r == null) {
            return "Só cabe recurso depois da resposta";
        }
        if (!m.getTipoManifestacao().isPermiteRecurso()) {
            return "Este tipo de manifestação não tem fase recursal (Lei 13.460/2017)";
        }
        if (r.getResultadoLai() == null || !r.getResultadoLai().admiteRecurso()) {
            return "Recurso só cabe contra indeferimento total ou parcial do acesso (Lei 12.527, art. 15)";
        }
        if (c.recurso() != null) {
            return "Já houve recurso nesta manifestação (instância única)";
        }
        LocalDate prazo = prazoRecurso(r, c.prazo());
        if (c.hoje().isAfter(prazo)) {
            return "Prazo de recurso encerrado em " + prazo.format(DATA);
        }
        return null;
    }

    /** Encerrar antes do fim do prazo de recurso tiraria do cidadão um direito: por isso é bloqueado. */
    private static String impedimentoEncerramento(Contexto c, Ator ator) {
        if (!ator.ouvidor()) {
            return "Ação exclusiva da ouvidoria";
        }
        if (c.manifestacao().getStatus() != RESPONDIDA) {
            return "Só manifestação RESPONDIDA pode ser encerrada";
        }
        Resposta r = c.ultimaResposta();
        boolean recursoAindaPossivel = c.manifestacao().getTipoManifestacao().isPermiteRecurso()
                && c.recurso() == null && r != null && r.getResultadoLai() != null && r.getResultadoLai().admiteRecurso()
                && !c.hoje().isAfter(prazoRecurso(r, c.prazo()));
        return recursoAindaPossivel
                ? "Aguarde o fim do prazo de recurso do requerente (" + prazoRecurso(r, c.prazo()).format(DATA) + ")"
                : null;
    }

    public static boolean aguardandoAdministracao(StatusManifestacao status) {
        return AGUARDANDO_ADMINISTRACAO.contains(status);
    }

    public static LocalDate hoje() {
        return LocalDate.now(FUSO);
    }

    /** Prazo para recorrer: conta da ciência da resposta, aqui a data em que ela foi registrada no sistema. */
    public static LocalDate prazoRecurso(Resposta resposta, PrazoLegal prazo) {
        return somarDias(resposta.getRespondidaEm().atZoneSameInstant(FUSO).toLocalDate(),
                prazo.getDiasInterposicaoRecurso());
    }

    /**
     * Contagem de prazo da Lei 9.784/1999, art. 66: exclui o dia do começo, inclui o do vencimento, conta dias
     * corridos e, se o vencimento cair em dia sem expediente, prorroga para o primeiro dia útil seguinte.
     * Limitação conhecida: só sábados e domingos são tratados como dia sem expediente. Feriados (nacionais e
     * municipais) ainda não, então um vencimento em feriado fica um dia antes do legal, tanto no prazo da
     * administração quanto no prazo de recurso do cidadão. A correção é um calendário de feriados em dado.
     */
    public static LocalDate somarDias(LocalDate inicio, int dias) {
        LocalDate vencimento = inicio.plusDays(dias);
        while (vencimento.getDayOfWeek() == DayOfWeek.SATURDAY || vencimento.getDayOfWeek() == DayOfWeek.SUNDAY) {
            vencimento = vencimento.plusDays(1);
        }
        return vencimento;
    }
}
