package com.logistics.logistics.infrastructure.adapters.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PredictionResponse(

  @JsonProperty("predicted_arrival_hours")
  Double predictedArrivalHours,

  @JsonProperty("confidence_score")
  Double confidenceScore,

  @JsonProperty("model_version")
  String modelVersion,

  String status,

  Map<String, Object> metadata

) {}
