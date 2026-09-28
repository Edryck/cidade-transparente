package br.edu.utfpr.cidadetransparente.dto;

/** Os dados do usuário vão junto para o frontend montar a tela sem decodificar o token. */
public record LoginResponse(
        String token,
        String tipo,
        long expiraEmSegundos,
        Long usuarioId,
        String nome,
        String perfil,
        Long municipioId) {
}
