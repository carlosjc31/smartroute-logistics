package com.logistics.logistics.shared.security;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param corsAllowedOrigins origens do frontend autorizadas a chamar a API (nunca "*")
 * @param rateLimit limite de requisições por usuário (ou IP, se anônimo)
 */
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(List<String> corsAllowedOrigins, RateLimit rateLimit) {

  public SecurityProperties {
    corsAllowedOrigins = corsAllowedOrigins == null ? List.of() : List.copyOf(corsAllowedOrigins);
    if (corsAllowedOrigins.contains("*"))
      throw new IllegalArgumentException("app.security.cors-allowed-origins não pode conter '*'");
    rateLimit = rateLimit == null ? new RateLimit(true, 120, Duration.ofMinutes(1)) : rateLimit;
  }

  /** @param capacity requisições permitidas a cada {@code period} (balde reabastecido aos poucos) */
  public record RateLimit(boolean enabled, long capacity, Duration period) {}
}
