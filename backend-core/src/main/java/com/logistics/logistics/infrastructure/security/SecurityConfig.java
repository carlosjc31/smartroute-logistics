package com.logistics.logistics.infrastructure.security;

import static com.logistics.logistics.infrastructure.security.Roles.ADMIN;
import static com.logistics.logistics.infrastructure.security.Roles.DISPATCHER;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * API stateless protegida por JWT do Keycloak (OAuth2 Resource Server).
 *
 * <p>Perfis: ADMIN e DISPATCHER operam viagens; só ADMIN lê a auditoria. DRIVER e CUSTOMER ainda não têm
 * acesso: precisam de vínculo com a viagem (motorista/cliente), que chega na Fase 4 (#38, #39).
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

  /** API JSON: nada de scripts, frames ou plugins. O Swagger UI (só fora de produção) precisa de 'self'. */
  static final String CONTENT_SECURITY_POLICY = "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
      + "img-src 'self' data:; object-src 'none'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'";

  @Bean
  SecurityFilterChain apiSecurity(HttpSecurity http, SecurityProperties properties, ObjectMapper json)
      throws Exception {
    ProblemDetailSecurityHandler problems = new ProblemDetailSecurityHandler(json);
    http
        .csrf(AbstractHttpConfigurer::disable) // sem cookies de sessão: o token vai no header Authorization
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(cors -> cors.configurationSource(corsConfigurationSource(properties)))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
            .requestMatchers("/actuator/health", "/actuator/health/**", "/error").permitAll()
            .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll()
            .requestMatchers("/api/v1/audit/**").hasRole(ADMIN)
            .requestMatchers("/api/v1/trips/**", "/api/v1/geocode").hasAnyRole(ADMIN, DISPATCHER)
            .anyRequest().denyAll())
        .oauth2ResourceServer(oauth -> oauth
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            .authenticationEntryPoint(problems)
            .accessDeniedHandler(problems))
        .exceptionHandling(errors -> errors.authenticationEntryPoint(problems).accessDeniedHandler(problems))
        .headers(headers -> headers
            .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
            .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
            .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000)))
        .addFilterAfter(new RateLimitFilter(properties.rateLimit(), problems), BearerTokenAuthenticationFilter.class);
    return http.build();
  }

  static JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new KeycloakRolesConverter());
    return converter;
  }

  static CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
    CorsConfiguration cors = new CorsConfiguration();
    cors.setAllowedOrigins(properties.corsAllowedOrigins());
    cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
    cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    cors.setExposedHeaders(List.of("Location", "Retry-After", "X-RateLimit-Remaining"));
    cors.setMaxAge(Duration.ofHours(1));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", cors);
    return source;
  }
}
