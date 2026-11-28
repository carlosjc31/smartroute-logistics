package com.logistics.logistics.trip.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.logistics.logistics.api.dto.EtaSourceDTO;
import com.logistics.logistics.api.dto.TripStatusDTO;
import com.logistics.logistics.trip.domain.EtaSource;
import com.logistics.logistics.trip.domain.TripStatus;

/** Os enums do contrato e do domínio precisam ter os mesmos valores, senão o mapeamento falha em runtime. */
class TripApiMapperTest {

  @Test
  void tripStatusHasTheSameValuesInTheContractAndInTheDomain() {
    for (TripStatus status : TripStatus.values()) {
      assertThat(TripApiMapper.toDomain(TripApiMapper.toDto(status))).isEqualTo(status);
    }
    assertThat(names(TripStatusDTO.values())).containsExactlyInAnyOrder(names(TripStatus.values()));
  }

  @Test
  void etaSourceHasTheSameValuesInTheContractAndInTheDomain() {
    assertThat(names(EtaSourceDTO.values())).containsExactlyInAnyOrder(names(EtaSource.values()));
  }

  @Test
  void missingStatusFilterStaysNull() {
    assertThat(TripApiMapper.toDomain(null)).isNull();
  }

  static String[] names(Enum<?>[] values) {
    return Arrays.stream(values).map(Enum::name).toArray(String[]::new);
  }
}
