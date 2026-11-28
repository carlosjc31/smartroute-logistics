package com.logistics.logistics.shared.application;

public abstract class NotFoundException extends RuntimeException {
  protected NotFoundException(String message) {
    super(message);
  }
}
