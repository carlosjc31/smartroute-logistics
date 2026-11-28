package com.logistics.logistics.geocoding.application;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.logistics.logistics.geocoding.domain.GeoLocation;

@Service
public class GeocodingService {

  private final GeocodingPort geocoding;

  public GeocodingService(GeocodingPort geocoding) {
    this.geocoding = geocoding;
  }

  /** Resultados encontrados ficam em cache (ver spring.cache.*); buscas sem resultado não. */
  @Cacheable(cacheNames = "geocoding", key = "#query.trim().toLowerCase()")
  public GeoLocation geocode(String query) {
    return geocoding.geocode(query.trim()).orElseThrow(() -> new LocationNotFoundException(query));
  }
}
