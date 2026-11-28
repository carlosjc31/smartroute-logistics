package com.logistics.logistics.infrastructure.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.logistics.application.service.AuditService;
import com.logistics.logistics.infrastructure.controller.api.AuditApi;
import com.logistics.logistics.infrastructure.controller.dto.AuditEntryPageDTO;
import com.logistics.logistics.infrastructure.controller.mapper.ApiMapper;

/** Histórico de auditoria (somente ADMIN, ver {@code SecurityConfig}). */
@RestController
public class AuditController implements AuditApi {

  private final AuditService auditService;

  public AuditController(AuditService auditService) {
    this.auditService = auditService;
  }

  @Override
  public ResponseEntity<AuditEntryPageDTO> getTripAuditHistory(UUID id, Integer page, Integer size) {
    return ResponseEntity.ok(ApiMapper.toAuditPage(auditService.tripHistory(id, page, size)));
  }
}
