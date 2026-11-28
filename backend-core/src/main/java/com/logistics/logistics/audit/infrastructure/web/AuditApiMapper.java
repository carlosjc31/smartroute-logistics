package com.logistics.logistics.audit.infrastructure.web;

import java.util.LinkedHashMap;
import java.util.Map;

import com.logistics.logistics.api.dto.AuditActionDTO;
import com.logistics.logistics.api.dto.AuditEntryDTO;
import com.logistics.logistics.api.dto.AuditEntryPageDTO;
import com.logistics.logistics.audit.AuditAction;
import com.logistics.logistics.audit.AuditEntry;
import com.logistics.logistics.shared.application.PageResult;

/** Converte o histórico de auditoria para os DTOs gerados do contrato. */
final class AuditApiMapper {

  private AuditApiMapper() {}

  static AuditActionDTO toDto(AuditAction action) {
    return AuditActionDTO.valueOf(action.name());
  }

  static AuditEntryDTO toDto(AuditEntry entry) {
    return new AuditEntryDTO(entry.id(), entry.occurredAt(), entry.actor(), toDto(entry.action()),
        copy(entry.before()), copy(entry.after()));
  }

  static AuditEntryPageDTO toPage(PageResult<AuditEntry> page) {
    return new AuditEntryPageDTO(page.map(AuditApiMapper::toDto).items(), page.page(), page.size(),
        page.totalItems(), page.totalPages());
  }

  private static Map<String, Object> copy(Map<String, Object> state) {
    return state == null ? null : new LinkedHashMap<>(state);
  }
}
