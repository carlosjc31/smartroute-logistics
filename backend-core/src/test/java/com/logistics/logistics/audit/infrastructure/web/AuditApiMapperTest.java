package com.logistics.logistics.audit.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.logistics.logistics.api.dto.AuditActionDTO;
import com.logistics.logistics.audit.AuditAction;

class AuditApiMapperTest {

  @Test
  void auditActionHasTheSameValuesInTheContractAndInTheModule() {
    assertThat(Arrays.stream(AuditActionDTO.values()).map(Enum::name))
        .containsExactlyInAnyOrderElementsOf(Arrays.stream(AuditAction.values()).map(Enum::name).toList());
    for (AuditAction action : AuditAction.values()) {
      assertThat(AuditApiMapper.toDto(action).name()).isEqualTo(action.name());
    }
  }
}
