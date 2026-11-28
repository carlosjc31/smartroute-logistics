package com.logistics.logistics.geocoding.application;

import java.util.Optional;

import com.logistics.logistics.geocoding.domain.GeoLocation;

public interface GeocodingPort {
  /**
   * @throws com.logistics.logistics.geocoding.application.GeocodingUnavailableException
   *     se o provedor de geocodificação não responder
   */
  Optional<GeoLocation> geocode(String query);
}
