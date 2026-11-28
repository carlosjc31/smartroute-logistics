package com.logistics.logistics.geocoding.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import com.logistics.logistics.geocoding.application.GeocodingService;
import com.logistics.logistics.geocoding.application.GeocodingUnavailableException;
import com.logistics.logistics.geocoding.application.LocationNotFoundException;
import com.logistics.logistics.geocoding.domain.GeoLocation;
import com.logistics.logistics.shared.domain.Coordinates;
import com.logistics.logistics.shared.security.SecurityConfig;

/** Rotas de geocodificação (spec 002). */
@WebMvcTest(GeocodingController.class)
@Import(SecurityConfig.class)
@WithMockUser(roles = "DISPATCHER")
@ExtendWith(OutputCaptureExtension.class)
class GeocodingControllerTest {

  @Autowired MockMvc mvc;
  @MockBean GeocodingService geocodingService;

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
