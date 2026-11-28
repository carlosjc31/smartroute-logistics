package com.logistics.logistics.trip.domain;

/** Origem da previsão de chegada. */
public enum EtaSource {
  /** Previsão do modelo de ML do ai-engine. */
  MODEL,
  /** Estimativa heurística usada quando o ai-engine está indisponível. */
  FALLBACK
}
