package com.logistics.logistics.audit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.logistics.logistics.audit.application.AuditLogPort;
import com.logistics.logistics.shared.application.PageResult;

/** Auditoria em memória para testes de casos de uso de outros módulos (só em src/test). */
public class InMemoryAuditLog implements AuditLogPort {
  public final List<AuditEntry> entries = new ArrayList<>();

  @Override
  public void append(AuditEntry entry) {
    entries.add(entry);
  }

  @Override
  public PageResult<AuditEntry> findByEntity(String entityType, UUID entityId, int page, int size) {
    List<AuditEntry> matching = entries.stream()
        .filter(e -> e.entityType().equals(entityType) && e.entityId().equals(entityId))
        .sorted(Comparator.comparing(AuditEntry::occurredAt).reversed())
        .toList();
    return new PageResult<>(matching, page, size, matching.size(), matching.isEmpty() ? 0 : 1);
  }
}
