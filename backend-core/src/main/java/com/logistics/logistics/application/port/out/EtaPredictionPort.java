package com.logistics.logistics.application.port.out;

import com.logistics.logistics.domain.EtaPrediction;
import com.logistics.logistics.domain.Trip;

public interface EtaPredictionPort {
  /**
   * @throws com.logistics.logistics.application.exception.EtaPredictionUnavailableException
   *     se o serviço de previsão não responder
   */
  EtaPrediction predict(Trip trip);
}
