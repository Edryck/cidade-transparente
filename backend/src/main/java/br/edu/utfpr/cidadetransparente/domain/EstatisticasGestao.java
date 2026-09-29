package br.edu.utfpr.cidadetransparente.domain;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Números do relatório de gestão: só contagens agregadas, nenhum dado pessoal. Congelado em JSON na
 * publicação (RelatorioGestao.estatisticas).
 */
public record EstatisticasGestao(
        OffsetDateTime calculadoEm,
        /** Lei 13.460, art. 15, I. */
        long recebidas,
        long anonimas,
        long prorrogadas,
        /** Motivos das manifestações (art. 15, II). */
        List<Contagem> porTipo,
        List<Contagem> porSecretaria,
        List<Contagem> porStatus,
        Prazos prazos,
        Lai lai) {

    public record Contagem(String item, long total) {
    }

    /** Resposta no prazo = primeira resposta até a data limite vigente. */
    public record Prazos(long respondidas, long respondidasNoPrazo, long respondidasForaDoPrazo,
                         Double percentualNoPrazo, long pendentesVencidas, Double tempoMedioRespostaDias) {
    }

    /**
     * Estatística da LAI (art. 30, III), pelo resultado da última resposta de cada pedido. Sobre os
     * solicitantes, só a contagem: o sistema não coleta perfil (LGPD art. 6º, III).
     */
    public record Lai(long pedidosRecebidos, long solicitantesDistintos, long atendidos, long parcialmenteAtendidos,
                      long indeferidos, long informacaoInexistente, long semResposta, long recursosInterpostos,
                      long recursosDeferidos, long recursosIndeferidos) {
    }
}
