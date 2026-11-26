package com.logistics.logistics.application.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.logistics.application.PageResult;
import com.logistics.logistics.application.audit.AuditAction;
import com.logistics.logistics.application.audit.AuditEntry;
import com.logistics.logistics.application.port.out.AuditLogPort;
import com.logistics.logistics.application.port.out.CurrentActorPort;
import com.logistics.logistics.domain.Trip;

@Service
public class AuditService {

  public static final String TRIP = "TRIP";

  private final AuditLogPort auditLog;
  private final CurrentActorPort currentActor;
  private final Clock clock;

  public AuditService(AuditLogPort auditLog, CurrentActorPort currentActor, Clock clock) {
    this.auditLog = auditLog;
    this.currentActor = currentActor;
    this.clock = clock;
  }

  /** Grava na transação do caso de uso: se a auditoria falhar, a alteração também é desfeita. */
  @Transactional
  public void recordTripChange(AuditAction action, Trip before, Trip after) {
    auditLog.append(new AuditEntry(UUID.randomUUID(), OffsetDateTime.now(clock), currentActor.currentActor(), action,
        TRIP, after.id(), before == null ? null : snapshot(before), snapshot(after)));
  }

  @Transactional(readOnly = true)
  public PageResult<AuditEntry> tripHistory(UUID tripId, int page, int size) {
    return auditLog.findByEntity(TRIP, tripId, page, size);
  }

  /** Campos operacionais da viagem; coordenadas e endereços ficam de fora (dados pessoais). */
  static Map<String, Object> snapshot(Trip trip) {
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
