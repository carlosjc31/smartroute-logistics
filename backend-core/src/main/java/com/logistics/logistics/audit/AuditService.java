package com.logistics.logistics.audit;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.logistics.logistics.audit.application.AuditLogPort;
import com.logistics.logistics.shared.application.CurrentActorPort;
import com.logistics.logistics.shared.application.PageResult;

/**
 * API pública do módulo de auditoria. Os outros módulos chamam {@link #record} dentro da transação do
 * caso de uso (constituição, art. 6): se a auditoria falhar, a alteração também é desfeita. O estado
 * gravado é montado pelo módulo dono do dado, que sabe quais campos são pessoais e os deixa de fora.
 */
@Service
public class AuditService {

  /** Tipo de entidade das viagens (rota {@code /api/v1/audit/trips/{id}}). */
  public static final String TRIP = "TRIP";

  private final AuditLogPort auditLog;
  private final CurrentActorPort currentActor;
  private final Clock clock;

  public AuditService(AuditLogPort auditLog, CurrentActorPort currentActor, Clock clock) {
    this.auditLog = auditLog;
    this.currentActor = currentActor;
    this.clock = clock;
  }

  /**
   * @param before estado anterior (null na criação)
   * @param after estado novo; nunca com coordenadas nem endereços (LGPD)
   */
  @Transactional
  public void record(AuditAction action, String entityType, UUID entityId, Map<String, Object> before,
      Map<String, Object> after) {
    auditLog.append(new AuditEntry(UUID.randomUUID(), OffsetDateTime.now(clock), currentActor.currentActor(), action,
        entityType, entityId, before, after));
  }

  @Transactional(readOnly = true)
  public PageResult<AuditEntry> tripHistory(UUID tripId, int page, int size) {
    return auditLog.findByEntity(TRIP, tripId, page, size);
  }
}
