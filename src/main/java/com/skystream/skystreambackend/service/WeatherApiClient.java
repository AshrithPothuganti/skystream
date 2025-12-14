package com.skystream.skystreambackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
public class WeatherApiClient {

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${weather.api.key}")
    private String apiKey;

    private static final String BASE = "https://api.weatherapi.com/v1";

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------



    // -------------------------------------------------------------------------
    // CURRENT WEATHER API
    // -------------------------------------------------------------------------

    public Map<String, Object> getCurrent(String q) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE + "/current.json")
                    .queryParam("key", apiKey)
                    .queryParam("q", q)
                    .queryParam("aqi", "yes")
                    .toUriString();

            // Type-safe parse into Map<String,Object>
            String json = rest.getForObject(url, String.class);
            Map<String, Object> parsed =
                    mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            return parsed;

        } catch (Exception ex) {
            return Map.of(
                    "error", "external-api-failure",
                    "message", ex.getMessage()
            );
        }
    }

    // -------------------------------------------------------------------------
    // FORECAST API
    // -------------------------------------------------------------------------

    public Map<String, Object> getForecast(String q, int days) {
        if (days < 1) days = 1;
        if (days > 15) days = 15;

        try {
            String url = UriComponentsBuilder.fromHttpUrl(BASE + "/forecast.json")
                    .queryParam("key", apiKey)
                    .queryParam("q", q)
                    .queryParam("days", days)
                    .queryParam("aqi", "yes")
                    .queryParam("alerts", "yes")
                    .toUriString();

            String json = rest.getForObject(url, String.class);
            Map<String, Object> parsed =
                    mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            return parsed;

        } catch (Exception ex) {
            return Map.of(
                    "error", "external-api-failure",
                    "message", ex.getMessage()
            );
        }
    }


// -------------------------------------------------------------------------
// AUTOCOMPLETE / CITY SEARCH API
// -------------------------------------------------------------------------

public List<Map<String, Object>> searchCitiesTyped(String q) {
    try {
        String url = UriComponentsBuilder
                .fromHttpUrl(BASE + "/search.json")
                .queryParam("key", apiKey)
                .queryParam("q", q)
                .toUriString();

        System.out.println("WeatherAPI SEARCH URL = " + url);

        String json = rest.getForObject(url, String.class);
        System.out.println("WeatherAPI RAW RESPONSE = " + json);

        return mapper.readValue(
                json,
                new TypeReference<List<Map<String, Object>>>() {}
        );

    } catch (Exception ex) {
        ex.printStackTrace();   // 👈 THIS IS CRITICAL
        return List.of();
    }
}


}


