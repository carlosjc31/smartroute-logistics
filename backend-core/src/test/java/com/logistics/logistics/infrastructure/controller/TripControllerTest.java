package com.logistics.logistics.infrastructure.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.client.ResourceAccessException;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.logistics.logistics.application.PageResult;
import com.logistics.logistics.application.exception.GeocodingUnavailableException;
import com.logistics.logistics.application.exception.LocationNotFoundException;
import com.logistics.logistics.application.exception.TripNotFoundException;
import com.logistics.logistics.application.service.CreateTripCommand;
import com.logistics.logistics.application.service.GeocodingService;
import com.logistics.logistics.application.service.TripService;
import com.logistics.logistics.domain.Coordinates;
import com.logistics.logistics.domain.EtaPrediction;
import com.logistics.logistics.domain.EtaSource;
import com.logistics.logistics.domain.GeoLocation;
import com.logistics.logistics.domain.Trip;
import com.logistics.logistics.domain.TripStatus;
import com.logistics.logistics.domain.exception.InvalidStatusTransitionException;
import com.logistics.logistics.infrastructure.security.SecurityConfig;

@WebMvcTest({TripController.class, GeocodingController.class})
@Import(SecurityConfig.class)
@WithMockUser(roles = "DISPATCHER")
@ExtendWith(OutputCaptureExtension.class)
class TripControllerTest {

  private static final Trip TRIP = Trip.create(new Coordinates(-23.55, -46.63), new Coordinates(-22.90, -43.17),
          "São Paulo", "Rio de Janeiro", 10.0, OffsetDateTime.of(2030, 1, 1, 8, 0, 0, 0, ZoneOffset.ofHours(-3)),
          OffsetDateTime.of(2029, 12, 31, 12, 0, 0, 0, ZoneOffset.UTC))
      .withPrediction(new EtaPrediction(5.0, 0.89, "v1", EtaSource.MODEL));

  @Autowired MockMvc mvc;
  @MockBean TripService tripService;
  @MockBean GeocodingService geocodingService;

  // @req TRIP-001
  @Test
  void createsTrip() throws Exception {
    when(tripService.createTrip(any())).thenReturn(TRIP);

    mvc.perform(post("/api/v1/trips").contentType(MediaType.APPLICATION_JSON).content("""
            {"originLat": -23.55, "originLng": -46.63, "destLat": -22.90, "destLng": -43.17,
             "originLabel": "São Paulo", "destinationLabel": "Rio de Janeiro",
             "payloadWeight": 10, "departureTime": "2030-01-01T08:00:00-03:00"}
            """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/api/v1/trips/" + TRIP.id()))
        .andExpect(jsonPath("$.id").value(TRIP.id().toString()))
        .andExpect(jsonPath("$.origin.label").value("São Paulo"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.eta.source").value("MODEL"))
        .andExpect(jsonPath("$.predictedArrival").value("2030-01-01T13:00:00-03:00"));

    // A hora local de saída (08h, pico) precisa chegar intacta ao caso de uso, sem conversão para UTC
    ArgumentCaptor<CreateTripCommand> command = ArgumentCaptor.forClass(CreateTripCommand.class);
    verify(tripService).createTrip(command.capture());
    assertThat(command.getValue().departureTime().getHour()).isEqualTo(8);
    assertThat(command.getValue().departureTime().getOffset()).isEqualTo(ZoneOffset.ofHours(-3));
  }

  // @req TRIP-002
  @Test
  void rejectsInvalidPayloadWithFieldErrors() throws Exception {
    mvc.perform(post("/api/v1/trips").contentType(MediaType.APPLICATION_JSON).content("""
            {"originLat": 95, "originLng": -46.63, "destLat": -22.90, "destLng": -43.17,
             "departureTime": "2000-01-01T08:00:00Z"}
            """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.originLat").exists())
        .andExpect(jsonPath("$.errors.payloadWeight").exists())
        .andExpect(jsonPath("$.errors.departureTime").exists());
  }

  // @req TRIP-005
  @Test
  void listsTripsWithPagination() throws Exception {
    when(tripService.listTrips(TripStatus.PENDING, 0, 20)).thenReturn(new PageResult<>(List.of(TRIP), 0, 20, 1, 1));

    mvc.perform(get("/api/v1/trips").param("status", "PENDING"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(TRIP.id().toString()))
        .andExpect(jsonPath("$.totalItems").value(1));
  }

  // @req TRIP-005
  @Test
  void rejectsPageSizeAboveLimit() throws Exception {
    mvc.perform(get("/api/v1/trips").param("size", "500")).andExpect(status().isBadRequest());
  }

  // @req TRIP-006
  @Test
  void returns404ForUnknownTrip() throws Exception {
    UUID id = UUID.randomUUID();
    when(tripService.getTrip(id)).thenThrow(new TripNotFoundException(id));

    mvc.perform(get("/api/v1/trips/{id}", id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
  }

  // @req TRIP-007
  @Test
  void returns409ForInvalidTransition() throws Exception {
    when(tripService.updateStatus(eq(TRIP.id()), eq(TripStatus.COMPLETED)))
        .thenThrow(new InvalidStatusTransitionException(TripStatus.PENDING, TripStatus.COMPLETED));

    mvc.perform(patch("/api/v1/trips/{id}/status", TRIP.id())
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"COMPLETED\"}"))
        .andExpect(status().isConflict());
  }

  // @req SEC-010
  @Test
  void allowsCrossOriginStatusUpdate() throws Exception {
    when(tripService.updateStatus(TRIP.id(), TripStatus.IN_TRANSIT))
        .thenReturn(TRIP.transitionTo(TripStatus.IN_TRANSIT, OffsetDateTime.now()));

    mvc.perform(patch("/api/v1/trips/{id}/status", TRIP.id())
            .header("Origin", "http://localhost:5173")
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"IN_TRANSIT\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
  }

  // @req SEC-020
  @Test
  void unexpectedErrorHidesInternalDetails() throws Exception {
    when(tripService.getTrip(any())).thenThrow(new IllegalStateException("senha=abc em jdbc:postgresql://db"));

    mvc.perform(get("/api/v1/trips/{id}", UUID.randomUUID()))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.title").value("Erro interno"))
        .andExpect(content().string(not(containsString("senha=abc"))))
        .andExpect(content().string(not(containsString("IllegalStateException"))));
  }

  // @req GEO-001
  @Test
  void geocodesCity() throws Exception {
    when(geocodingService.geocode("Campinas"))
        .thenReturn(new GeoLocation("Campinas, SP, Brasil", new Coordinates(-22.9, -47.06)));

    mvc.perform(get("/api/v1/geocode").param("q", "Campinas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lat").value(-22.9));
  }

  // @req GEO-001
  @Test
  void rejectsBlankGeocodingQuery() throws Exception {
    mvc.perform(get("/api/v1/geocode").param("q", "   ")).andExpect(status().isBadRequest());

    verifyNoInteractions(geocodingService);
  }

  // @req GEO-002
  @Test
  void geocodeReturns404WhenNotFound() throws Exception {
    when(geocodingService.geocode("Xyz")).thenThrow(new LocationNotFoundException("Xyz"));

    mvc.perform(get("/api/v1/geocode").param("q", "Xyz")).andExpect(status().isNotFound());
  }

  // @req GEO-005
  @Test
  void geocodingOutageDoesNotLogTheSearchedAddress(CapturedOutput output) throws Exception {
    String address = "Rua das Acácias 123, Campinas";
    when(geocodingService.geocode(address)).thenThrow(new GeocodingUnavailableException("Falha ao consultar",
        new ResourceAccessException("I/O error on GET request for \"https://nominatim/search?q=" + address + "\"")));

    mvc.perform(get("/api/v1/geocode").param("q", address)).andExpect(status().isServiceUnavailable());

    assertThat(output).contains("Geocodificação indisponível").doesNotContain("Acácias");
  }
}
