package com.logistics.logistics.infrastructure.adapters.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.logistics.logistics.domain.Coordinates;
import com.logistics.logistics.domain.EtaPrediction;
import com.logistics.logistics.domain.Trip;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;

/**
 * Contrato backend ↔ ai-engine. Os exemplos em {@code contracts/ai-engine/} também são verificados pelo
 * ai-engine (tests/test_contract.py); uma mudança de nome ou tipo de campo quebra um dos dois lados.
 */
class AiEngineContractTest {

  private static final Path CONTRACTS = Path.of("..", "contracts", "ai-engine");
  private static final ObjectMapper JSON = new ObjectMapper();

  private static JsonNode contract(String file) throws IOException {
    return JSON.readTree(Files.readString(CONTRACTS.resolve(file)));
  }

  // @req ETA-005
  @Test
  void requestSentByBackendMatchesContract() throws IOException {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://ai-engine");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    Map<String, JsonNode> sent = new HashMap<>();
    server.expect(request -> sent.put("body", JSON.readTree(((MockClientHttpRequest) request).getBodyAsString())))
        .andRespond(withSuccess(Files.readString(CONTRACTS.resolve("predict-eta.response.json")),
            MediaType.APPLICATION_JSON));
    AiEngineAdapter adapter = new AiEngineAdapter(builder.build(), CircuitBreakerRegistry.ofDefaults(),
        RetryRegistry.ofDefaults());
    Trip trip = Trip.create(new Coordinates(-23.55, -46.63), new Coordinates(-22.90, -43.17), null, null, 10.0,
        OffsetDateTime.of(2030, 1, 1, 8, 0, 0, 0, ZoneOffset.ofHours(-3)), OffsetDateTime.now(ZoneOffset.UTC));

    adapter.predict(trip);

    assertSameShape(sent.get("body"), contract("predict-eta.request.json"));
  }

  // @req ETA-005
  @Test
  void contractResponseMapsToEveryPredictionField() throws IOException {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://ai-engine");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    server.expect(request -> {}).andRespond(withSuccess(
        Files.readString(CONTRACTS.resolve("predict-eta.response.json")), MediaType.APPLICATION_JSON));
    AiEngineAdapter adapter = new AiEngineAdapter(builder.build(), CircuitBreakerRegistry.ofDefaults(),
        RetryRegistry.ofDefaults());
    Trip trip = Trip.create(new Coordinates(-23.55, -46.63), new Coordinates(-22.90, -43.17), null, null, 10.0,
        OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC));

    EtaPrediction prediction = adapter.predict(trip);

    JsonNode expected = contract("predict-eta.response.json");
    assertThat(prediction.hours()).isEqualTo(expected.get("predicted_arrival_hours").asDouble());
    assertThat(prediction.confidence()).isEqualTo(expected.get("confidence_score").asDouble());
    assertThat(prediction.modelVersion()).isEqualTo(expected.get("model_version").asText());
  }

  // @req ETA-005
  @Test
  void responseHasNoFieldUnknownToBackend() throws Exception {
    // Um campo renomeado no ai-engine (ex.: "cofidence_score") falha aqui em vez de virar null em silêncio.
    Class<PredictionResponse> responseType = PredictionResponse.class;
    ObjectMapper strict = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

    Object parsed = strict.treeToValue(contract("predict-eta.response.json"), responseType);

    for (var component : responseType.getRecordComponents())
      assertThat(component.getAccessor().invoke(parsed)).as(component.getName()).isNotNull();
  }

  private static void assertSameShape(JsonNode actual, JsonNode expected) {
    assertThat(fieldNames(actual)).containsExactlyInAnyOrderElementsOf(fieldNames(expected));
    expected.fields().forEachRemaining(field -> assertThat(typeOf(actual.get(field.getKey())))
        .as(field.getKey()).isEqualTo(typeOf(field.getValue())));
  }

  private static Iterable<String> fieldNames(JsonNode node) {
    return node::fieldNames;
  }

  /** Inteiros e decimais são "number"; o ai-engine (Pydantic) aceita os dois para float. */
  private static String typeOf(JsonNode node) {
    if (node.isIntegralNumber())
      return "integer";
    return node.getNodeType() == JsonNodeType.NUMBER ? "number" : node.getNodeType().name();
  }
}
