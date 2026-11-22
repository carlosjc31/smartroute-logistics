package com.logistics.logistics.infrastructure.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.logistics.logistics.application.exception.GeocodingUnavailableException;
import com.logistics.logistics.domain.GeoLocation;
import com.logistics.logistics.infrastructure.adapters.geocoding.GeocodingProperties;
import com.logistics.logistics.infrastructure.adapters.geocoding.NominatimGeocodingAdapter;

class NominatimGeocodingAdapterTest {

  private final RestClient.Builder builder = RestClient.builder().baseUrl("http://nominatim");
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

  private NominatimGeocodingAdapter adapter(String countryCodes) {
    return new NominatimGeocodingAdapter(builder.build(),
        new GeocodingProperties("http://nominatim", "test-agent", countryCodes, Duration.ZERO, null, null));
  }

  @Test
  void mapsFirstPlaceAndRestrictsCountry() {
    server.expect(requestTo(Matchers.startsWith("http://nominatim/search")))
        .andExpect(method(HttpMethod.GET))
        .andExpect(queryParam("q", "Campinas"))
        .andExpect(queryParam("limit", "1"))
        .andExpect(queryParam("countrycodes", "br"))
        .andRespond(withSuccess("""
            [{"lat": "-22.9056", "lon": "-47.0608", "display_name": "Campinas, São Paulo, Brasil"}]
            """, MediaType.APPLICATION_JSON));

    GeoLocation location = adapter("br").geocode("Campinas").orElseThrow();

    assertThat(location.displayName()).isEqualTo("Campinas, São Paulo, Brasil");
    assertThat(location.coordinates().lat()).isEqualTo(-22.9056);
    assertThat(location.coordinates().lng()).isEqualTo(-47.0608);
    server.verify();
  }

  @Test
  void omitsCountryFilterWhenBlank() {
    server.expect(requestTo(Matchers.not(Matchers.containsString("countrycodes"))))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    assertThat(adapter("").geocode("Lisboa")).isEmpty();
    server.verify();
  }

  @Test
  void providerFailureBecomesUnavailable() {
    server.expect(requestTo(Matchers.startsWith("http://nominatim/search"))).andRespond(withServerError());

    assertThatThrownBy(() -> adapter("br").geocode("Campinas")).isInstanceOf(GeocodingUnavailableException.class);
  }
}
