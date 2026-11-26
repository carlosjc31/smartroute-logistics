package com.logistics.logistics.infrastructure.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.logistics.logistics.application.service.AuditService;
import com.logistics.logistics.infrastructure.controller.dto.AuditEntryDTO;
import com.logistics.logistics.infrastructure.controller.dto.PageResponseDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Auditoria")
public class AuditController {

  private final AuditService auditService;

  public AuditController(AuditService auditService) {
    this.auditService = auditService;
  }

  @GetMapping("/trips/{id}")
  @Operation(summary = "Histórico de alterações de uma viagem (somente ADMIN)")
  public PageResponseDTO<AuditEntryDTO> tripHistory(@PathVariable UUID id,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return PageResponseDTO.from(auditService.tripHistory(id, page, size), AuditEntryDTO::from);
  }
}
