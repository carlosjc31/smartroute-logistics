package com.logistics.logistics.infrastructure.adapters;


import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.logistics.logistics.domain.Trip;
import com.logistics.logistics.infrastructure.adapters.dto.PredictionResponse;

@Service
public class AIClientAdapter {
  private final RestClient restClient;

  public AIClientAdapter(RestClient.Builder builder) {
    this.restClient = builder.baseUrl("http://ai-engine:8000").build();
  }
  public PredictionResponse getETAPrediction(Trip trip) {
        return restClient.post()
            .uri("/predict-eta")
            .body(trip)
            .retrieve()
            .body(PredictionResponse.class);
    }
}
