package com.logistics.logistics.infrastructure.adapters.ai;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Resposta de POST /predict/eta do ai-engine (contrato descrito no CLAUDE.md). */
record PredictionResponse(
  @JsonProperty("predicted_arrival_hours") Double predictedArrivalHours,
  @JsonProperty("confidence_score") Double confidenceScore,
  @JsonProperty("model_version") String modelVersion,
  String status,
  Map<String, Object> metadata
) {}
