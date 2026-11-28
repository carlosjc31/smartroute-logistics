package com.logistics.logistics.geocoding.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.logistics.logistics.geocoding.domain.GeoLocation;
import com.logistics.logistics.shared.domain.Coordinates;

/** Cache do {@link GeocodingService} com o proxy real do Spring, sem o restante da aplicação. */
@SpringJUnitConfig(GeocodingCacheTest.Config.class)
class GeocodingCacheTest {

  private static final GeoLocation CAMPINAS = new GeoLocation("Campinas, SP", new Coordinates(-22.9, -47.06));
  private static final List<String> PROVIDER_CALLS = new CopyOnWriteArrayList<>();

  @Configuration
  @EnableCaching
  static class Config {
    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("geocoding");
    }

    @Bean
    GeocodingService geocodingService() {
      return new GeocodingService(query -> {
        PROVIDER_CALLS.add(query);
        return query.equals("Campinas") ? Optional.of(CAMPINAS) : Optional.empty();
      });
    }
  }

  @Autowired GeocodingService service;
  @Autowired CacheManager cacheManager;

  @BeforeEach
  void reset() {
    PROVIDER_CALLS.clear();
    cacheManager.getCache("geocoding").clear();
  }

  // @req GEO-003
  @Test
  void cachesFoundPlacesIgnoringCaseAndSurroundingSpaces() {
    assertThat(service.geocode("Campinas")).isEqualTo(CAMPINAS);
    assertThat(service.geocode("  campinas ")).isEqualTo(CAMPINAS);

    assertThat(PROVIDER_CALLS).containsExactly("Campinas");
  }

  // @req GEO-003
  @Test
  void doesNotCacheUnknownPlaces() {
    assertThatThrownBy(() -> service.geocode("Lugar Inexistente")).isInstanceOf(LocationNotFoundException.class);
    assertThatThrownBy(() -> service.geocode("Lugar Inexistente")).isInstanceOf(LocationNotFoundException.class);

    assertThat(PROVIDER_CALLS).containsExactly("Lugar Inexistente", "Lugar Inexistente");
  }
}
