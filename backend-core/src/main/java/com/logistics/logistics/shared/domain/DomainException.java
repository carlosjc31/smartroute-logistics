package com.logistics.logistics.shared.domain;

/** Violação de uma regra de negócio do domínio. */
public class DomainException extends RuntimeException {
  public DomainException(String message) {
    super(message);
  }
}
