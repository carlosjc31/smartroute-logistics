package com.logistics.logistics.geocoding.infrastructure.nominatim;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param minInterval intervalo mínimo entre chamadas ao provedor (Nominatim exige no máximo 1 req/s)
 * @param countryCodes códigos ISO separados por vírgula para restringir a busca (vazio = mundo)
 */
@ConfigurationProperties(prefix = "geocoding")
public record GeocodingProperties(String baseUrl, String userAgent, String countryCodes,
    Duration minInterval, Duration connectTimeout, Duration readTimeout) {}
