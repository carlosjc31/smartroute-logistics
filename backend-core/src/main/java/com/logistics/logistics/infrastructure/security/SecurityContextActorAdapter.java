package com.logistics.logistics.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.logistics.logistics.application.port.out.CurrentActorPort;

/** Ator da auditoria: {@code preferred_username} do token (ou {@code sub}); "system" fora de requisições. */
@Component
public class SecurityContextActorAdapter implements CurrentActorPort {

  static final String SYSTEM = "system";

  @Override
  public String currentActor() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwt) {
      String username = jwt.getToken().getClaimAsString("preferred_username");
      return username != null ? username : jwt.getToken().getSubject();
    }
    return auth != null && auth.isAuthenticated() ? auth.getName() : SYSTEM;
  }
}
