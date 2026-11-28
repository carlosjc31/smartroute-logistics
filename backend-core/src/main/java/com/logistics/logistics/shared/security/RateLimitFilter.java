package com.logistics.logistics.shared.security;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Limita requisições a /api por usuário autenticado (claim {@code sub}) ou, se anônimo, por IP.
 * Em memória (uma instância); com várias réplicas, trocar por Bucket4j sobre Redis.
 */
public class RateLimitFilter extends OncePerRequestFilter {

  private final SecurityProperties.RateLimit limit;
  private final ProblemDetailSecurityHandler problems;
  private final Cache<String, Bucket> buckets;

  public RateLimitFilter(SecurityProperties.RateLimit limit, ProblemDetailSecurityHandler problems) {
    this.limit = limit;
    this.problems = problems;
    this.buckets = Caffeine.newBuilder()
        .maximumSize(100_000)
        .expireAfterAccess(limit.period().multipliedBy(2).plus(Duration.ofMinutes(1)))
        .build();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !limit.enabled() || !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Bucket bucket = buckets.get(clientKey(request), key -> newBucket());
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    response.setHeader("X-RateLimit-Limit", String.valueOf(limit.capacity()));
    if (probe.isConsumed()) {
      response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
      chain.doFilter(request, response);
      return;
    }
    long retryAfter = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
    response.setHeader("Retry-After", String.valueOf(retryAfter));
    response.setHeader("X-RateLimit-Remaining", "0");
    problems.write(response, HttpStatus.TOO_MANY_REQUESTS, "Muitas requisições",
        "Limite de requisições atingido. Tente novamente em " + retryAfter + " s.");
  }

  private Bucket newBucket() {
    return Bucket.builder()
        .addLimit(Bandwidth.builder().capacity(limit.capacity()).refillGreedy(limit.capacity(), limit.period()).build())
        .build();
  }

  private static String clientKey(HttpServletRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken jwt)
      return "user:" + jwt.getToken().getSubject();
    // Atrás de proxy, getRemoteAddr só é confiável com server.forward-headers-strategy configurado
    return "ip:" + request.getRemoteAddr();
  }
}
