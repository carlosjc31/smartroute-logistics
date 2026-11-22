package com.logistics.logistics.infrastructure.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import com.logistics.logistics.application.exception.EtaPredictionUnavailableException;
import com.logistics.logistics.domain.Coordinates;
import com.logistics.logistics.domain.EtaPrediction;
import com.logistics.logistics.domain.EtaSource;
import com.logistics.logistics.domain.Trip;
import com.logistics.logistics.infrastructure.adapters.ai.AiEngineAdapter;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

class AiEngineAdapterTest {

  private static final Trip TRIP = Trip.create(new Coordinates(-23.55, -46.63), new Coordinates(-22.90, -43.17),
      null, null, 10.0, OffsetDateTime.of(2030, 1, 1, 8, 0, 0, 0, ZoneOffset.ofHours(-3)), OffsetDateTime.now());

  private MockRestServiceServer server;
  private AiEngineAdapter adapter;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://ai-engine");
    server = MockRestServiceServer.bindTo(builder).build();
    RetryRegistry retries = RetryRegistry.of(RetryConfig.custom()
        .maxAttempts(2).waitDuration(Duration.ofMillis(1))
        .retryExceptions(HttpServerErrorException.class).build());
    adapter = new AiEngineAdapter(builder.build(), CircuitBreakerRegistry.ofDefaults(), retries);
  }

  @Test
  void mapsSuccessfulPrediction() {
    server.expect(requestTo("http://ai-engine/predict/eta"))
        .andExpect(jsonPath("$.hour_of_day").value(8))
        .andExpect(jsonPath("$.weight").value(10.0))
        .andRespond(withSuccess("""
            {"predicted_arrival_hours": 4.92, "confidence_score": 0.89,
             "model_version": "v1.0-random-forest", "status": "success", "metadata": {}}
            """, MediaType.APPLICATION_JSON));

    EtaPrediction prediction = adapter.predict(TRIP);

    assertThat(prediction.hours()).isEqualTo(4.92);
    assertThat(prediction.confidence()).isEqualTo(0.89);
    assertThat(prediction.modelVersion()).isEqualTo("v1.0-random-forest");
    assertThat(prediction.source()).isEqualTo(EtaSource.MODEL);
  }

  @Test
  void retriesServerErrorsThenReportsUnavailable() {
    server.expect(times(2), requestTo("http://ai-engine/predict/eta")).andRespond(withServerError());

    assertThatThrownBy(() -> adapter.predict(TRIP)).isInstanceOf(EtaPredictionUnavailableException.class);
    server.verify();
  }
}
