package com.logistics.logistics.infrastructure.controller.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.logistics.logistics.application.audit.AuditEntry;

public record AuditEntryDTO(UUID id, OffsetDateTime occurredAt, String actor, String action,
    Map<String, Object> before, Map<String, Object> after) {

  public static AuditEntryDTO from(AuditEntry entry) {
    return new AuditEntryDTO(entry.id(), entry.occurredAt(), entry.actor(), entry.action().name(),
        entry.before(), entry.after());
  }
}
