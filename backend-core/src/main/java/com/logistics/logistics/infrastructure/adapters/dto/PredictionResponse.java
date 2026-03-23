package com.logistics.logistics.infrastructure.adapters.dto;

public record PredictionResponse(
    Double predicted_arrival, // Deve bater com a chave do JSON do Python
    Double confidence_score,
    String model_version
) {

}
