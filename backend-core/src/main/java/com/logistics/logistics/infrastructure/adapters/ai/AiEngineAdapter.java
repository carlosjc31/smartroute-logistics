package com.logistics.logistics.infrastructure.adapters.ai;

import java.util.Map;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.logistics.logistics.application.exception.EtaPredictionUnavailableException;
import com.logistics.logistics.application.port.out.EtaPredictionPort;
import com.logistics.logistics.domain.EtaPrediction;
import com.logistics.logistics.domain.EtaSource;
import com.logistics.logistics.domain.Trip;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;

/**
 * Cliente do ai-engine com timeout (configurado no RestClient), retry e circuit breaker
 * (instância "aiEngine" em application.properties). Qualquer falha vira
 * {@link EtaPredictionUnavailableException}, e o caso de uso decide o fallback.
 */
@Component
public class AiEngineAdapter implements EtaPredictionPort {

  static final String RESILIENCE_NAME = "aiEngine";

  private final RestClient restClient;
  private final CircuitBreaker circuitBreaker;
  private final Retry retry;

  public AiEngineAdapter(@Qualifier("aiEngineRestClient") RestClient restClient,
      CircuitBreakerRegistry circuitBreakers, RetryRegistry retries) {
    this.restClient = restClient;
    this.circuitBreaker = circuitBreakers.circuitBreaker(RESILIENCE_NAME);
    this.retry = retries.retry(RESILIENCE_NAME);
  }

  @Override
  public EtaPrediction predict(Trip trip) {
    Supplier<EtaPrediction> call = () -> callAiEngine(trip);
    try {
      return Retry.decorateSupplier(retry, CircuitBreaker.decorateSupplier(circuitBreaker, call)).get();
    } catch (RuntimeException e) {
      throw new EtaPredictionUnavailableException("Falha ao consultar o ai-engine: " + e.getMessage(), e);
    }
  }

  private EtaPrediction callAiEngine(Trip trip) {
    Map<String, Object> request = Map.of(
        "distance", trip.distanceKm(),
        "weight", trip.payloadWeight(),
        "hour_of_day", trip.departureTime().getHour(),
        "is_rainy", 0 // TODO(#47): clima real via Open-Meteo
    );

    PredictionResponse response = restClient.post()
        .uri("/predict/eta")
        .body(request)
        .retrieve()
        .body(PredictionResponse.class);

    if (response == null || response.predictedArrivalHours() == null)
      throw new IllegalStateException("ai-engine retornou uma previsão vazia");
    return new EtaPrediction(response.predictedArrivalHours(), response.confidenceScore(),
        response.modelVersion(), EtaSource.MODEL);
  }
}
