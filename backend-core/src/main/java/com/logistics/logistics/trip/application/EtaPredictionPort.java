package com.logistics.logistics.trip.application;

import com.logistics.logistics.trip.domain.EtaPrediction;
import com.logistics.logistics.trip.domain.Trip;

public interface EtaPredictionPort {
  /**
   * @throws com.logistics.logistics.trip.application.EtaPredictionUnavailableException
   *     se o serviço de previsão não responder
   */
  EtaPrediction predict(Trip trip);
}
