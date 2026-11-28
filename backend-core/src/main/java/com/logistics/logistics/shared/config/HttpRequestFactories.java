package com.logistics.logistics.shared.config;

import java.time.Duration;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

/** Fábrica de requisições HTTP de saída com timeouts explícitos (nenhum cliente fica sem timeout). */
public final class HttpRequestFactories {

  private HttpRequestFactories() {}

  public static SimpleClientHttpRequestFactory withTimeouts(Duration connect, Duration read) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(connect);
    factory.setReadTimeout(read);
    return factory;
  }
}
