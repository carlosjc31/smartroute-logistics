package com.logistics.logistics.infrastructure.adapters.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PredictionResponse(

  @JsonProperty("predicted_arrival_hours")
  Double predicted_arrival,

  @JsonProperty("cofidence_score")
  Double confidence_score,

  @JsonProperty("model_version")
  String model_version

) {}
