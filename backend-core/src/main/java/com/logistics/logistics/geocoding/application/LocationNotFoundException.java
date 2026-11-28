package com.logistics.logistics.geocoding.application;

import com.logistics.logistics.shared.application.NotFoundException;

public class LocationNotFoundException extends NotFoundException {
  public LocationNotFoundException(String query) {
    super("Local não encontrado: " + query);
  }
}
