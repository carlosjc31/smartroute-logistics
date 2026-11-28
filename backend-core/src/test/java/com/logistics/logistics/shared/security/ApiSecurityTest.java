package com.logistics.logistics.shared.security;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.logistics.logistics.audit.AuditService;
import com.logistics.logistics.audit.infrastructure.web.AuditController;
import com.logistics.logistics.geocoding.application.GeocodingService;
import com.logistics.logistics.geocoding.domain.GeoLocation;
import com.logistics.logistics.geocoding.infrastructure.web.GeocodingController;
import com.logistics.logistics.shared.application.PageResult;
import com.logistics.logistics.shared.domain.Coordinates;
import com.logistics.logistics.trip.application.TripService;
import com.logistics.logistics.trip.infrastructure.web.TripController;

/** Regras de acesso, CORS, headers e rate limit, sem Keycloak (tokens simulados pelo spring-security-test). */
@WebMvcTest({TripController.class, GeocodingController.class, AuditController.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
    "app.security.rate-limit.capacity=5",
    "app.security.cors-allowed-origins=https://painel.transportadora.com.br"})
class ApiSecurityTest {

  private static final String ALLOWED_ORIGIN = "https://painel.transportadora.com.br";

  @Autowired MockMvc mvc;
  @MockBean TripService tripService;
  @MockBean GeocodingService geocodingService;
  @MockBean AuditService auditService;

  private static JwtRequestPostProcessor as(String subject, String... roles) {
    // Claims no formato do Keycloak, convertidos pelo mesmo conversor da aplicação
    return jwt().jwt(token -> token.subject(subject).claim("realm_access", Map.of("roles", List.of(roles))))
        .authorities(new KeycloakRolesConverter());
  }

  // @req SEC-001
  @Test
  void anonymousRequestGets401ProblemDetail() throws Exception {
    mvc.perform(get("/api/v1/trips"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Não autenticado"));
  }

  // @req TRIP-020
  @Test
  void dispatcherCanOperateTrips() throws Exception {
    when(tripService.listTrips(any(), anyInt(), anyInt())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

    mvc.perform(get("/api/v1/trips").with(as("dispatcher-1", Roles.DISPATCHER))).andExpect(status().isOk());
  }

  // @req TRIP-020
  @Test
  void driverAndCustomerCannotAccessTripsYet() throws Exception {
    mvc.perform(get("/api/v1/trips").with(as("driver-1", Roles.DRIVER)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.title").value("Acesso negado"));
    mvc.perform(post("/api/v1/trips").with(as("customer-1", Roles.CUSTOMER))
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
  }

  // @req GEO-010
  @Test
  void onlyAdminAndDispatcherCanGeocode() throws Exception {
    when(geocodingService.geocode("Campinas")).thenReturn(new GeoLocation("Campinas, SP", new Coordinates(-22.9, -47.06)));

    mvc.perform(get("/api/v1/geocode").param("q", "Campinas").with(as("dispatcher-3", Roles.DISPATCHER)))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/geocode").param("q", "Campinas").with(as("admin-3", Roles.ADMIN)))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/geocode").param("q", "Campinas").with(as("driver-3", Roles.DRIVER)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/geocode").param("q", "Campinas").with(as("customer-3", Roles.CUSTOMER)))
        .andExpect(status().isForbidden());
  }

  // @req TRIP-021
  @Test
  void auditIsAdminOnly() throws Exception {
    UUID id = UUID.randomUUID();
    when(auditService.tripHistory(eq(id), anyInt(), anyInt())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

    mvc.perform(get("/api/v1/audit/trips/{id}", id).with(as("dispatcher-2", Roles.DISPATCHER)))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/audit/trips/{id}", id).with(as("admin-1", Roles.ADMIN)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalItems").value(0));
  }

  // @req SEC-004
  @Test
  void unknownPathsAreDenied() throws Exception {
    mvc.perform(get("/api/v2/anything").with(as("admin-2", Roles.ADMIN))).andExpect(status().isForbidden());
  }

  // @req SEC-010
  @Test
  void corsAllowsOnlyConfiguredOrigins() throws Exception {
    mvc.perform(options("/api/v1/trips")
            .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN));

    mvc.perform(options("/api/v1/trips")
            .header(HttpHeaders.ORIGIN, "https://site-malicioso.example")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  // @req SEC-011
  @Test
  void sendsSecurityHeaders() throws Exception {
    mvc.perform(get("/api/v1/trips").secure(true))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("X-Frame-Options", "DENY"))
        .andExpect(header().string("Referrer-Policy", "no-referrer"))
        .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
        .andExpect(header().string("Strict-Transport-Security", containsString("max-age=31536000")));
  }

  // @req SEC-012
  @Test
  void rateLimitsPerUserWith429() throws Exception {
    when(tripService.listTrips(any(), anyInt(), anyInt())).thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));

    for (int i = 0; i < 5; i++)
      mvc.perform(get("/api/v1/trips").with(as("burst-user", Roles.DISPATCHER))).andExpect(status().isOk());

    mvc.perform(get("/api/v1/trips").with(as("burst-user", Roles.DISPATCHER)))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.title").value("Muitas requisições"));
    // O limite é por usuário: outro usuário não é afetado
    mvc.perform(get("/api/v1/trips").with(as("other-user", Roles.DISPATCHER))).andExpect(status().isOk());
  }
}
