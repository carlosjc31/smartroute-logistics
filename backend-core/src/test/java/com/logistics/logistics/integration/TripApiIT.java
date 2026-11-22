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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

/**
 * Ponta a ponta no backend: HTTP → controller → caso de uso → PostgreSQL real (Testcontainers), com o
 * ai-engine e o Nominatim simulados pelo WireMock. Roda em {@code ./mvnw verify} (failsafe) e exige Docker;
 * sem Docker o teste é ignorado.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TripApiIT {

  @Container
  @ServiceConnection
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

  static final WireMockServer EXTERNAL = new WireMockServer(wireMockConfig().dynamicPort());

  static {
    EXTERNAL.start();
  }

  @DynamicPropertySource
  static void externalServices(DynamicPropertyRegistry registry) {
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
    api = RestClient.builder()
        .baseUrl("http://localhost:" + port + "/api/v1")
        .requestFactory(new JdkClientHttpRequestFactory())
        .build();
  }

  @Test
  @SuppressWarnings("unchecked")
  void dispatchesTripWithModelEtaAndCompletesIt() throws Exception {
    EXTERNAL.stubFor(get(urlPathEqualTo("/search")).withQueryParam("q", equalTo("Campinas"))
        .willReturn(okJson("""
            [{"lat": "-22.9056", "lon": "-47.0608", "display_name": "Campinas, São Paulo, Brasil"}]""")));
    EXTERNAL.stubFor(post("/predict/eta")
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
