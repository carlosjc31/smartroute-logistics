package com.logistics.logistics.infrastructure.adapters;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.logistics.logistics.domain.Trip;
import com.logistics.logistics.infrastructure.adapters.dto.PredictionResponse;

@Service
public class AIClientAdapter {
  private final RestClient restClient;

  public AIClientAdapter(RestClient.Builder builder, @Value("${ai-engine.url}") String aiEngineUrl) {
    this.restClient = builder.baseUrl(aiEngineUrl).build();
  }

  public PredictionResponse getETAPrediction(Trip trip) {
    Map<String, Object> aiRequest = Map.of(
      "distance", trip.distanceKm(),
      "weight", trip.payloadWeight(),
      "hour_of_day", trip.departureTime().getHour(),
      "is_rainy", 0 // TODO(#47): clima real via Open-Meteo
    );

    PredictionResponse response = restClient.post()
        .uri("/predict/eta")
        .body(aiRequest)
        .retrieve()
        .body(PredictionResponse.class);

    if (response == null || response.predictedArrivalHours() == null)
      throw new IllegalStateException("ai-engine retornou uma previsão vazia");
    return response;
  }
}
