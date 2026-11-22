package com.logistics.logistics.application.exception;

public class LocationNotFoundException extends NotFoundException {
  public LocationNotFoundException(String query) {
    super("Local não encontrado: " + query);
  }
}
