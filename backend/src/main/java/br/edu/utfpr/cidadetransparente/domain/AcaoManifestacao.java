package br.edu.utfpr.cidadetransparente.domain;

/** Ações sobre a manifestação. {@code rel} é o nome do link HATEOAS que o cliente procura para oferecê-la. */
public enum AcaoManifestacao {
    ANALISE("analise"),
    ENCAMINHAMENTO("encaminhamento"),
    PRORROGACAO("prorrogacao"),
    ARQUIVAMENTO("arquivamento"),
    RESPOSTA("resposta"),
    RECURSO("recurso"),
    JULGAMENTO("julgamento"),
    ENCERRAMENTO("encerramento"),
    ANEXO("anexar");

    private final String rel;

    AcaoManifestacao(String rel) {
        this.rel = rel;
    }

    public String rel() {
        return rel;
    }
}
