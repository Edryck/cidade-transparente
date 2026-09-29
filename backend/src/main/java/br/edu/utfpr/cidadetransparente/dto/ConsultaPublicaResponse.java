package br.edu.utfpr.cidadetransparente.dto;

import br.edu.utfpr.cidadetransparente.domain.StatusManifestacao;
import br.edu.utfpr.cidadetransparente.domain.StatusRecurso;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** Acompanhamento pelo protocolo e pela chave: andamento e respostas, sem nenhum dado de identificação. */
public record ConsultaPublicaResponse(String protocolo, String municipio, String tipo, String assunto,
                                      StatusManifestacao status, OffsetDateTime dataAbertura, LocalDate dataLimite,
                                      boolean prorrogada, List<TramiteResponse> tramites,
                                      List<RespostaResponse> respostas, StatusRecurso recurso) {
}
