package br.edu.utfpr.cidadetransparente.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Erro de negócio com o status HTTP que ele representa. Vira ProblemDetail no GlobalExceptionHandler. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public static ApiException requisicaoInvalida(String mensagem) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensagem);
    }

    public static ApiException proibido(String mensagem) {
        return new ApiException(HttpStatus.FORBIDDEN, mensagem);
    }

    public static ApiException naoEncontrado(String mensagem) {
        return new ApiException(HttpStatus.NOT_FOUND, mensagem);
    }

    public static ApiException conflito(String mensagem) {
        return new ApiException(HttpStatus.CONFLICT, mensagem);
    }
}
