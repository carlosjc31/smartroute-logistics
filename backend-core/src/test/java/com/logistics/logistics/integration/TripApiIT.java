package com.logistics.logistics.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Ponta a ponta no backend: HTTP → segurança (JWT) → controller → caso de uso → PostgreSQL real
 * (Testcontainers), com o ai-engine, o Nominatim e o JWKS do Keycloak simulados pelo WireMock. Os tokens são
 * assinados por uma chave RSA gerada no teste e validados de verdade (assinatura, issuer, audience). Roda em {@code ./mvnw verify} (failsafe) e exige Docker;
 * sem Docker o teste é ignorado.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TripApiIT {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

  static final WireMockServer EXTERNAL = new WireMockServer(wireMockConfig().dynamicPort());
  static final String REALM_PATH = "/realms/smartroute";
  static final String JWKS_PATH = REALM_PATH + "/protocol/openid-connect/certs";
  static final String AI_ENGINE_TOKEN = "it-service-token";
  static final RSAKey SIGNING_KEY = rsaKey("it-key");
  static final RSAKey FOREIGN_KEY = rsaKey("it-key");

  static {
    EXTERNAL.start();
  }

  @DynamicPropertySource
  static void externalServices(DynamicPropertyRegistry registry) {
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> EXTERNAL.baseUrl() + REALM_PATH);
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> EXTERNAL.baseUrl() + JWKS_PATH);
    registry.add("ai-engine.token", () -> AI_ENGINE_TOKEN);
    registry.add("ai-engine.url", EXTERNAL::baseUrl);
    registry.add("geocoding.base-url", EXTERNAL::baseUrl);
    registry.add("geocoding.min-interval", () -> "0s");
    registry.add("resilience4j.retry.instances.aiEngine.wait-duration", () -> "10ms");
  }

  @AfterAll
  static void stopExternal() {
    EXTERNAL.stop();
  }

  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;

  private RestClient api;

  @BeforeEach
  void setUp() {
    EXTERNAL.resetAll();
    EXTERNAL.stubFor(get(JWKS_PATH).willReturn(okJson(new JWKSet(SIGNING_KEY.toPublicJWK()).toString())));
    api = client(token(SIGNING_KEY, "ana.despacho", "smartroute-api", "DISPATCHER"));
  }

  private RestClient client(String bearer) {
    RestClient.Builder builder = RestClient.builder()
        .baseUrl("http://localhost:" + port + "/api/v1")
        .requestFactory(new JdkClientHttpRequestFactory());
    if (bearer != null)
      builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearer);
    return builder.build();
  }

  @Test
  @SuppressWarnings("unchecked")
  void dispatchesTripWithModelEtaAndCompletesIt() throws Exception {
    EXTERNAL.stubFor(get(urlPathEqualTo("/search")).withQueryParam("q", equalTo("Campinas"))
        .willReturn(okJson("""
            [{"lat": "-22.9056", "lon": "-47.0608", "display_name": "Campinas, São Paulo, Brasil"}]""")));
    EXTERNAL.stubFor(post("/predict/eta")
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer " + AI_ENGINE_TOKEN))
        .withRequestBody(equalToJson("{\"hour_of_day\": 8, \"weight\": 12.0, \"is_rainy\": 0}", true, true))
        .willReturn(okJson(Files.readString(Path.of("..", "contracts", "ai-engine", "predict-eta.response.json")))));

    Map<String, Object> place = api.get().uri("/geocode?q={q}", "Campinas").retrieve().body(MAP);
    assertThat(place).containsEntry("displayName", "Campinas, São Paulo, Brasil");

    OffsetDateTime departure = OffsetDateTime.now(ZoneOffset.ofHours(-3)).plusDays(1)
        .withHour(8).withMinute(0).withSecond(0).withNano(0);
    ResponseEntity<Map<String, Object>> created = api.post().uri("/trips")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("originLat", -23.5505, "originLng", -46.6333, "originLabel", "São Paulo",
            "destLat", place.get("lat"), "destLng", place.get("lng"), "destinationLabel", "Campinas",
            "payloadWeight", 12.0, "departureTime", departure.toString()))
        .retrieve().toEntity(MAP);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    Map<String, Object> trip = created.getBody();
    String id = (String) trip.get("id");
    assertThat(created.getHeaders().getLocation().getPath()).isEqualTo("/api/v1/trips/" + id);
    assertThat(trip).containsEntry("status", "PENDING");
    assertThat(trip.get("eta")).isEqualTo(Map.of("source", "MODEL", "modelVersion", "v1.0-random-forest"));
    // 4,92 h do contrato = 295 min, no fuso enviado pelo cliente
    assertThat(OffsetDateTime.parse((String) trip.get("predictedArrival"))).isEqualTo(departure.plusMinutes(295));
    EXTERNAL.verify(1, getRequestedFor(urlPathEqualTo("/search")));
    EXTERNAL.verify(1, postRequestedFor(urlPathEqualTo("/predict/eta")));

    patchStatus(id, "IN_TRANSIT");
    Map<String, Object> completed = patchStatus(id, "COMPLETED");
    assertThat(completed.get("actualArrival")).isNotNull();
    assertThat(completed.get("allowedTransitions")).isEqualTo(List.of());

    assertThat(jdbc.queryForObject("select status from trips where id = ?::uuid", String.class, id))
        .isEqualTo("COMPLETED");
    Map<String, Object> page = api.get().uri("/trips?status=COMPLETED").retrieve().body(MAP);
    assertThat((List<Map<String, Object>>) page.get("items")).extracting(item -> item.get("id")).contains((Object) id);

    // Auditoria: quem fez cada passo, legível só pelo ADMIN
    RestClient admin = client(token(SIGNING_KEY, "bruno.admin", "smartroute-api", "ADMIN"));
    Map<String, Object> audit = admin.get().uri("/audit/trips/{id}", id).retrieve().body(MAP);
    List<Map<String, Object>> history = (List<Map<String, Object>>) audit.get("items");
    assertThat(history).extracting(entry -> entry.get("action"))
        .containsExactly("TRIP_STATUS_CHANGED", "TRIP_STATUS_CHANGED", "TRIP_CREATED");
    assertThat(history).extracting(entry -> entry.get("actor")).containsOnly("ana.despacho");
    assertThat(history.get(0).get("after").toString()).contains("COMPLETED").doesNotContain("Campinas");
    assertThat(status(api.get().uri("/audit/trips/{id}", id))).isEqualTo(HttpStatus.FORBIDDEN);
  }

  @Test
  void rejectsMissingForgedOrMisaddressedTokens() {
    assertThat(status(client(null).get().uri("/trips"))).isEqualTo(HttpStatus.UNAUTHORIZED);
    // Mesmo kid, chave diferente: assinatura inválida
    assertThat(status(client(token(FOREIGN_KEY, "eve", "smartroute-api", "ADMIN")).get().uri("/trips")))
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    // Token válido, mas emitido para outro serviço
    assertThat(status(client(token(SIGNING_KEY, "eve", "outro-servico", "ADMIN")).get().uri("/trips")))
        .isEqualTo(HttpStatus.UNAUTHORIZED);
    // Autenticado, mas sem perfil de despacho
    assertThat(status(client(token(SIGNING_KEY, "carlos.motorista", "smartroute-api", "DRIVER")).get().uri("/trips")))
        .isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(status(client(null).get().uri("http://localhost:" + port + "/actuator/health"))).isEqualTo(HttpStatus.OK);
  }

  private static HttpStatus status(RestClient.RequestHeadersSpec<?> request) {
    return request.exchange((req, res) -> HttpStatus.valueOf(res.getStatusCode().value()));
  }

  private static String token(RSAKey key, String username, String audience, String... roles) {
    Instant now = Instant.now();
    JWTClaimsSet claims = new JWTClaimsSet.Builder()
        .issuer(EXTERNAL.baseUrl() + REALM_PATH)
        .subject(username + "-id")
        .audience(audience)
        .claim("preferred_username", username)
        .claim("realm_access", Map.of("roles", List.of(roles)))
        .issueTime(Date.from(now))
        .expirationTime(Date.from(now.plusSeconds(300)))
        .build();
    try {
      SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims);
      jwt.sign(new RSASSASigner(key));
      return jwt.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }

  private static RSAKey rsaKey(String kid) {
    try {
      return new RSAKeyGenerator(2048).keyID(kid).generate();
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  void usesHeuristicFallbackWhenAiEngineIsDown() {
    EXTERNAL.stubFor(post("/predict/eta").willReturn(aResponse().withStatus(503)));

    Map<String, Object> trip = api.post().uri("/trips")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("originLat", -23.5505, "originLng", -46.6333, "destLat", -22.9068, "destLng", -43.1729,
            "payloadWeight", 5.0, "departureTime", OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).toString()))
        .retrieve().body(MAP);

    assertThat(trip.get("eta")).isEqualTo(Map.of("source", "FALLBACK", "modelVersion", "fallback-heuristic-v1"));
    assertThat(trip.get("predictedArrival")).isNotNull();
    EXTERNAL.verify(2, postRequestedFor(urlPathEqualTo("/predict/eta")));
  }

  @Test
  void invalidTransitionIsRejectedWithProblemDetail() {
    Map<String, Object> trip = api.post().uri("/trips")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("originLat", -23.5505, "originLng", -46.6333, "destLat", -22.9068, "destLng", -43.1729,
            "payloadWeight", 5.0, "departureTime", OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).toString()))
        .retrieve().body(MAP);

    ResponseEntity<String> response = api.patch().uri("/trips/{id}/status", trip.get("id"))
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("status", "COMPLETED"))
        .exchange((request, res) -> ResponseEntity.status(res.getStatusCode())
            .headers(res.getHeaders()).body(new String(res.getBody().readAllBytes())));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
  }

  private Map<String, Object> patchStatus(String id, String status) {
    return api.patch().uri("/trips/{id}/status", id)
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("status", status))
        .retrieve().body(MAP);
  }

  private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {};
}
