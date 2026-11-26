package com.logistics.logistics.infrastructure.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.logistics.logistics.application.exception.GeocodingUnavailableException;
import com.logistics.logistics.application.exception.NotFoundException;
import com.logistics.logistics.domain.exception.DomainException;

/** Converte exceções em respostas RFC 7807 (application/problem+json). */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(NotFoundException.class)
  ProblemDetail handleNotFound(NotFoundException e) {
    return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", e.getMessage());
  }

  @ExceptionHandler(DomainException.class)
  ProblemDetail handleDomainRule(DomainException e) {
    return problem(HttpStatus.CONFLICT, "Regra de negócio violada", e.getMessage());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail handleInvalidInput(IllegalArgumentException e) {
    return problem(HttpStatus.BAD_REQUEST, "Dados inválidos", e.getMessage());
  }

  @ExceptionHandler(GeocodingUnavailableException.class)
  ProblemDetail handleGeocodingUnavailable(GeocodingUnavailableException e) {
    // Só o tipo da causa: a mensagem do RestClient traz a URL com o endereço buscado (dado pessoal, LGPD)
    log.warn("Geocodificação indisponível: {}", e.getCause() == null ? "sem causa" : e.getCause().getClass().getName());
    return problem(HttpStatus.SERVICE_UNAVAILABLE, "Serviço indisponível",
        "O serviço de geocodificação não respondeu. Tente novamente em instantes.");
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnexpected(Exception e) {
    log.error("Erro inesperado", e);
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado.");
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
      @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
    ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "Dados inválidos", "Um ou mais campos são inválidos.");
    Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(FieldError::getField,
            error -> error.getDefaultMessage() == null ? "inválido" : error.getDefaultMessage(),
            (first, second) -> first));
    body.setProperty("errors", errors);
    return ResponseEntity.badRequest().body(body);
  }

  private static ProblemDetail problem(HttpStatus status, String title, String detail) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    return problem;
  }
}
