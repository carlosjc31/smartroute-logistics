package com.logistics.logistics.application.port.out;

import java.util.Optional;

import com.logistics.logistics.domain.GeoLocation;

public interface GeocodingPort {
  /**
   * @throws com.logistics.logistics.application.exception.GeocodingUnavailableException
   *     se o provedor de geocodificação não responder
   */
  Optional<GeoLocation> geocode(String query);
}
