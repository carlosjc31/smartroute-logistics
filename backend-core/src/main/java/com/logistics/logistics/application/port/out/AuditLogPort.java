package com.logistics.logistics.application.port.out;

import java.util.UUID;

import com.logistics.logistics.application.PageResult;
import com.logistics.logistics.application.audit.AuditEntry;

/** Armazenamento append-only: não há operação de alterar ou apagar. */
public interface AuditLogPort {
  void append(AuditEntry entry);

  /** Histórico de uma entidade, do mais recente para o mais antigo. */
  PageResult<AuditEntry> findByEntity(String entityType, UUID entityId, int page, int size);
}
