package com.logistics.logistics.geocoding.application;

import com.logistics.logistics.shared.application.DependencyUnavailableException;

public class GeocodingUnavailableException extends DependencyUnavailableException {
  public GeocodingUnavailableException(String message, Throwable cause) {
    super("Geocodificação", "O serviço de geocodificação não respondeu. Tente novamente em instantes.", message, cause);
  }
}
