package com.logistics.logistics.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.logistics.logistics.application.exception.LocationNotFoundException;
import com.logistics.logistics.application.service.GeocodingService;
import com.logistics.logistics.domain.Coordinates;
import com.logistics.logistics.domain.GeoLocation;

class GeocodingServiceTest {

  private final List<String> queries = new ArrayList<>();
  private final GeoLocation campinas = new GeoLocation("Campinas, SP", new Coordinates(-22.9, -47.06));

  // @req GEO-001
  @Test
  void trimsQueryBeforeCallingProvider() {
    GeocodingService service = new GeocodingService(query -> {
      queries.add(query);
      return Optional.of(campinas);
    });

    assertThat(service.geocode("  Campinas ")).isEqualTo(campinas);
    assertThat(queries).containsExactly("Campinas");
  }

  // @req GEO-002
  @Test
  void unknownPlaceThrowsNotFound() {
    GeocodingService service = new GeocodingService(query -> Optional.empty());

    assertThatThrownBy(() -> service.geocode("Lugar Inexistente")).isInstanceOf(LocationNotFoundException.class);
  }
}
