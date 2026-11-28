package com.logistics.logistics.trip.application;

import java.util.LinkedHashMap;
import java.util.Map;

import com.logistics.logistics.trip.domain.Trip;

/** Estado da viagem gravado na auditoria: só campos operacionais, sem coordenadas nem endereços (LGPD). */
final class TripAuditSnapshot {

  private TripAuditSnapshot() {}

  static Map<String, Object> of(Trip trip) {
    Map<String, Object> state = new LinkedHashMap<>();
    state.put("status", trip.status().name());
    state.put("payloadWeight", trip.payloadWeight());
    state.put("departureTime", trip.departureTime().toString());
    putIfPresent(state, "predictedArrival", trip.predictedArrival());
    putIfPresent(state, "etaSource", trip.etaSource());
    putIfPresent(state, "etaModelVersion", trip.etaModelVersion());
    putIfPresent(state, "actualArrival", trip.actualArrival());
    return state;
  }

  private static void putIfPresent(Map<String, Object> state, String key, Object value) {
    if (value != null)
      state.put(key, value.toString());
  }
}
