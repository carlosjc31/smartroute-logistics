package com.logistics.logistics.infrastructure.adapters;


import java.util.Map;

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
    double dist = calculateDistance(
          trip.origin().lat(), trip.origin().lng(),
          trip.destination().lat(), trip.destination().lng()
      );

    Map<String, Object> aiRequest = Map.of(
      "distance", dist,
      "weight", trip.payloadWeight(),
      "hour_of_day", trip.departureTime().getHour(),
      "is_rainy", 0
    );

    return restClient.post()
        .uri("/predict/eta")
        .body(aiRequest)
        .retrieve()
        .body(PredictionResponse.class);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2){
      double theta = lon1 - lon2;
      double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));

      dist = Math.acos(dist);
      dist = Math.toDegrees(dist);
      return dist * 60 * 1.1515 * 1.609344;
    }
}
