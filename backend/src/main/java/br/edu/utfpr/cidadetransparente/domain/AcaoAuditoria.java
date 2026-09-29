package br.edu.utfpr.cidadetransparente.domain;

public enum AcaoAuditoria {
    LOGIN,
    MANIFESTACAO_REGISTRADA,
    TRANSICAO,
    RESPOSTA_REGISTRADA,
    RECURSO_INTERPOSTO,
    RECURSO_JULGADO,
    /** OUVIDOR visualizou a identificação do manifestante (Decreto 10.153, art. 6º, § 3º). */
    IDENTIDADE_ACESSADA,
    CONSULTA_PUBLICA,
    ANEXO_ENVIADO,
    ANEXO_BAIXADO,
    RELATORIO_PUBLICADO
}
