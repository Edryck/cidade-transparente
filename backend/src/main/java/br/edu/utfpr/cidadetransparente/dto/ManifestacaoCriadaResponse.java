package br.edu.utfpr.cidadetransparente.dto;

import br.edu.utfpr.cidadetransparente.domain.StatusManifestacao;

import java.time.LocalDate;

/**
 * Resposta da abertura. É o único momento em que a chave de acesso aparece: o banco guarda só o hash, então
 * ela não pode ser recuperada depois.
 */
public record ManifestacaoCriadaResponse(Long id, String protocolo, String chaveAcesso, StatusManifestacao status,
                                         LocalDate dataLimite, boolean anonima, String orientacao) {
}
