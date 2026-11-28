package com.logistics.logistics.trip.domain;

/** Duração prevista de uma viagem, em horas decimais. */
public record EtaPrediction(double hours, Double confidence, String modelVersion, EtaSource source) {
  public EtaPrediction {
    if (Double.isNaN(hours) || hours < 0)
      throw new IllegalArgumentException("Duração prevista inválida: " + hours);
    if (source == null)
      throw new IllegalArgumentException("Origem da previsão é obrigatória");
  }
}
