package br.edu.utfpr.cidadetransparente.dto;

import br.edu.utfpr.cidadetransparente.domain.Municipio;

/** Visão pública do município: só o necessário para o cidadão escolher onde se cadastrar. */
public record MunicipioResumo(Long id, String nome, String uf) {

    public static MunicipioResumo de(Municipio municipio) {
        return new MunicipioResumo(municipio.getId(), municipio.getNome(), municipio.getUf());
    }
}
