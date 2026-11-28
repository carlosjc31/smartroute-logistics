package com.logistics.logistics.audit;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Registro imutável de auditoria: quem fez o quê, quando, e o estado antes/depois.
 * Os estados guardam só campos operacionais, nunca coordenadas ou endereços (LGPD, minimização).
 *
 * @param before estado anterior (null na criação)
 */
public record AuditEntry(
  UUID id,
  OffsetDateTime occurredAt,
  String actor,
  AuditAction action,
  String entityType,
  UUID entityId,
  Map<String, Object> before,
  Map<String, Object> after
) {
  public AuditEntry {
    if (id == null || occurredAt == null || actor == null || action == null || entityType == null || entityId == null)
      throw new IllegalArgumentException("Registro de auditoria incompleto");
    before = before == null ? null : Map.copyOf(before);
    after = after == null ? null : Map.copyOf(after);
  }
}
