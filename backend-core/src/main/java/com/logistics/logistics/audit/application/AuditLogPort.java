package com.logistics.logistics.audit.application;

import java.util.UUID;

import com.logistics.logistics.audit.AuditEntry;
import com.logistics.logistics.shared.application.PageResult;

/** Armazenamento append-only: não há operação de alterar ou apagar. */
public interface AuditLogPort {
  void append(AuditEntry entry);

  /** Histórico de uma entidade, do mais recente para o mais antigo. */
  PageResult<AuditEntry> findByEntity(String entityType, UUID entityId, int page, int size);
}
