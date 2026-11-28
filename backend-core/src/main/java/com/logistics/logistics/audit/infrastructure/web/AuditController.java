package com.logistics.logistics.audit.infrastructure.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.logistics.api.AuditApi;
import com.logistics.logistics.api.dto.AuditEntryPageDTO;
import com.logistics.logistics.audit.AuditService;

/** Histórico de auditoria (somente ADMIN, ver {@code SecurityConfig}). */
@RestController
public class AuditController implements AuditApi {

  private final AuditService auditService;

  public AuditController(AuditService auditService) {
    this.auditService = auditService;
  }

  @Override
  public ResponseEntity<AuditEntryPageDTO> getTripAuditHistory(UUID id, Integer page, Integer size) {
    return ResponseEntity.ok(AuditApiMapper.toPage(auditService.tripHistory(id, page, size)));
  }
}
