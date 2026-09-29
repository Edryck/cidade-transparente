package br.edu.utfpr.cidadetransparente.domain;

/**
 * Desfecho da resposta a um pedido de acesso à informação (Lei 12.527, art. 11, § 1º).
 * Só NEGADO e PARCIALMENTE_CONCEDIDO admitem recurso: o art. 15 o prevê contra indeferimento de acesso.
 */
public enum ResultadoLai {
    CONCEDIDO,
    PARCIALMENTE_CONCEDIDO,
    NEGADO,
    INEXISTENTE;

    public boolean admiteRecurso() {
        return this == NEGADO || this == PARCIALMENTE_CONCEDIDO;
    }
}
