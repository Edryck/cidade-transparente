package br.edu.utfpr.cidadetransparente.domain;

/** Ciclo de vida da manifestação. Prorrogação não é status: é a flag {@code Manifestacao.prorrogada}. */
public enum StatusManifestacao {
    RECEBIDA,
    EM_ANALISE,
    ENCAMINHADA,
    RESPONDIDA,
    EM_RECURSO,
    ENCERRADA,
    ARQUIVADA
}
