package com.logistics.logistics.trip.domain;

/**
 * Estimativa de ETA sem o modelo de ML, usada como fallback quando o ai-engine está fora.
 * Reproduz as regras do dataset de treino: 80 km/h, atraso proporcional ao peso e
 * multiplicador de 1,5 nos horários de pico (7–9h e 17–19h).
 */
public final class HeuristicEtaEstimator {

  public static final String VERSION = "fallback-heuristic-v1";
  private static final double AVERAGE_SPEED_KMH = 80.0;
  private static final double WEIGHT_DELAY_FACTOR = 0.005;
  private static final double PEAK_MULTIPLIER = 1.5;

  private HeuristicEtaEstimator() {}

  public static EtaPrediction estimate(Trip trip) {
    double base = trip.distanceKm() / AVERAGE_SPEED_KMH;
    double withWeight = base * (1 + trip.payloadWeight() * WEIGHT_DELAY_FACTOR);
    int hour = trip.departureTime().getHour();
    boolean peak = (hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 19);
    double hours = peak ? withWeight * PEAK_MULTIPLIER : withWeight;
    return new EtaPrediction(hours, null, VERSION, EtaSource.FALLBACK);
  }
}
