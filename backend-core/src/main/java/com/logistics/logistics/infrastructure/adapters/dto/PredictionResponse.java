package com.logistics.logistics.infrastructure.adapters.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PredictionResponse(

  @JsonProperty("predicted_arrival_hours")
  Double predictedArrivalHours,

  @JsonProperty("cofidence_score")
  Double confidence_score,

  @JsonProperty("model_version")
  String model_version,

  String status,

  Map<String, Object> metadata

) {}
