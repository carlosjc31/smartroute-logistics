package com.logistics.logistics.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class KeycloakRolesConverterTest {

  private final KeycloakRolesConverter converter = new KeycloakRolesConverter();

  private static Jwt token(Map<String, Object> claims) {
    Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "RS256").subject("user-1")
        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
    claims.forEach(builder::claim);
    return builder.build();
  }

  @Test
  void mapsKnownRealmRolesToAuthorities() {
    Jwt jwt = token(Map.of("realm_access", Map.of("roles", List.of("DISPATCHER", "offline_access", "ADMIN"))));

    assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_DISPATCHER", "ROLE_ADMIN");
  }

  @Test
  void tokenWithoutRolesHasNoAuthorities() {
    assertThat(converter.convert(token(Map.of("scope", "openid")))).isEmpty();
    assertThat(converter.convert(token(Map.of("realm_access", Map.of("roles", "ADMIN"))))).isEmpty();
  }

  @Test
  void actorIsPreferredUsernameThenSubjectThenSystem() {
    SecurityContextActorAdapter actor = new SecurityContextActorAdapter();
    try {
      assertThat(actor.currentActor()).isEqualTo("system");

      SecurityContextHolder.getContext().setAuthentication(
          new JwtAuthenticationToken(token(Map.of("preferred_username", "ana.despacho")), List.of()));
      assertThat(actor.currentActor()).isEqualTo("ana.despacho");

      SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token(Map.of()), List.of()));
      assertThat(actor.currentActor()).isEqualTo("user-1");
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  void rejectsWildcardCorsOrigin() {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> new SecurityProperties(List.of("*"), null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
