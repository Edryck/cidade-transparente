package br.edu.utfpr.cidadetransparente.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Todo erro sai como ProblemDetail (RFC 9457). A base ResponseEntityExceptionHandler já cobre os erros
 * do Spring MVC (JSON malformado, método não suportado...). AccessDeniedException não é tratada aqui:
 * propaga até o Spring Security, que responde 403 pelo accessDeniedHandler do SecurityConfig.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ProblemDetail tratarApiException(ApiException e) {
        return ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
    }

    /** Última barreira para unique/FK violadas que o service não verificou antes. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail tratarIntegridade(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Operação conflita com dados já existentes");
    }

    /** Ordenação por campo inexistente (?sort=xyz) é erro do cliente, não do servidor. */
    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail tratarOrdenacaoInvalida(PropertyReferenceException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Campo de ordenação inválido: " + e.getPropertyName());
    }

    /** 400 com a lista de campos inválidos, para o frontend marcar cada campo. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> campos.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
        problema.setProperty("campos", campos);
        return ResponseEntity.badRequest().body(problema);
    }
}
