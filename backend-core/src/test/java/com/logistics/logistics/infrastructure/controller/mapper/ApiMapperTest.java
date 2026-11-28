package com.logistics.logistics.infrastructure.controller.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.logistics.logistics.application.audit.AuditAction;
import com.logistics.logistics.domain.EtaSource;
import com.logistics.logistics.domain.TripStatus;
import com.logistics.logistics.infrastructure.controller.dto.AuditActionDTO;
import com.logistics.logistics.infrastructure.controller.dto.EtaSourceDTO;
import com.logistics.logistics.infrastructure.controller.dto.TripStatusDTO;

/** Os enums do contrato e do domínio precisam ter os mesmos valores, senão o mapeamento falha em runtime. */
class ApiMapperTest {

  @Test
  void tripStatusHasTheSameValuesInTheContractAndInTheDomain() {
    for (TripStatus status : TripStatus.values()) {
      assertThat(ApiMapper.toDomain(ApiMapper.toDto(status))).isEqualTo(status);
    }
    assertThat(names(TripStatusDTO.values())).containsExactlyInAnyOrder(names(TripStatus.values()));
  }

  @Test
  void etaSourceAndAuditActionHaveTheSameValuesInTheContractAndInTheApplication() {
    assertThat(names(EtaSourceDTO.values())).containsExactlyInAnyOrder(names(EtaSource.values()));
    assertThat(names(AuditActionDTO.values())).containsExactlyInAnyOrder(names(AuditAction.values()));
    for (AuditAction action : AuditAction.values()) {
      assertThat(ApiMapper.toDto(action).name()).isEqualTo(action.name());
    }
  }

  @Test
  void missingStatusFilterStaysNull() {
    assertThat(ApiMapper.toDomain(null)).isNull();
  }

  private static String[] names(Enum<?>[] values) {
    return Arrays.stream(values).map(Enum::name).toArray(String[]::new);
  }
}
