package br.edu.utfpr.cidadetransparente.dto;

import java.util.List;

/**
 * Aviso de privacidade do município (LGPD arts. 9º e 23, I): quem trata, para quê, com qual base legal, por
 * quanto tempo e como o titular exerce seus direitos. Público: é informação que a lei manda divulgar.
 */
public record AvisoPrivacidadeResponse(
        String versao,
        String controlador,
        String operador,
        Encarregado encarregado,
        List<Tratamento> tratamentos,
        List<String> compartilhamento,
        List<Direito> direitos,
        String prazoAtendimento,
        String incidentes) {

    /** Nulo enquanto a prefeitura não indicar o encarregado: o frontend deve exibir o aviso da pendência. */
    public record Encarregado(String nome, String email) {
    }

    public record Tratamento(String finalidade, List<String> dados, String baseLegal, String retencao) {
    }

    public record Direito(String direito, String comoExercer) {
    }
}
