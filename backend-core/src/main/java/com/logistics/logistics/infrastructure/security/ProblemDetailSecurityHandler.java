package com.logistics.logistics.infrastructure.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** 401 e 403 no mesmo formato RFC 7807 do restante da API. */
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  private final ObjectMapper json;

  public ProblemDetailSecurityHandler(ObjectMapper json) {
    this.json = json;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
      throws IOException {
    // Não detalha o motivo (expirado, assinatura inválida...) para não ajudar um atacante
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    write(response, HttpStatus.UNAUTHORIZED, "Não autenticado", "Faça login para acessar este recurso.");
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
      throws IOException {
    write(response, HttpStatus.FORBIDDEN, "Acesso negado", "Seu perfil não tem permissão para esta ação.");
  }

  void write(HttpServletResponse response, HttpStatus status, String title, String detail) throws IOException {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    json.writeValue(response.getOutputStream(), problem);
  }
}
