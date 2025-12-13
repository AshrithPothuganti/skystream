package com.skystream.skystreambackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
public class OpenWeatherService {

    private final RestTemplate rest = new RestTemplate();

    @Value("${openweather.api.key}")
    private String apiKey;

    // e.g. https://api.openweathermap.org/data/2.5/weather
    @Value("${openweather.api.current.url}")
    private String currentUrl;

    /**
     * Fetch current weather from OpenWeatherMap (fallback).
     * Returns frontend-friendly map:
     * city, date, temperature, high, low, humidity, wind, condition
     */
    public Map<String, Object> fetchCurrent(String city) {
        try {
            if (apiKey == null || apiKey.isBlank() || currentUrl == null || currentUrl.isBlank()) {
                return Map.of("error", "fallback-api-config-missing");
            }

            String url = UriComponentsBuilder.fromUriString(java.util.Objects.requireNonNull(currentUrl, "currentUrl must not be null"))
                    .queryParam("q", city)
                    .queryParam("appid", apiKey)
                    .queryParam("units", "metric")
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> resp = rest.getForObject(url, Map.class);
            if (resp == null) return Map.of("error", "no-response");

            @SuppressWarnings("unchecked")
            Map<String, Object> main = (Map<String, Object>) resp.get("main");

            @SuppressWarnings("unchecked")
            Map<String, Object> wind = (Map<String, Object>) resp.get("wind");

            // Weather description extraction (safe)
            String condition = "";
            Object weatherObj = resp.get("weather");
            if (weatherObj instanceof java.util.List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> w0 = (Map<String, Object>) first;
                    Object desc = w0.get("description");
                    if (desc != null) condition = desc.toString();
                }
            }

            Map<String, Object> out = new HashMap<>();
            out.put("city", resp.getOrDefault("name", city));
            out.put("date", java.time.Instant.now().toString());
            out.put("temperature", main != null ? main.get("temp") : null);
            out.put("high", main != null ? main.get("temp_max") : null);
            out.put("low", main != null ? main.get("temp_min") : null);
            out.put("humidity", main != null ? main.get("humidity") : null);

            // Convert wind speed from m/s → km/h (one decimal)
            if (wind != null && wind.get("speed") != null) {
                double mps = ((Number) wind.get("speed")).doubleValue();
                out.put("wind", Math.round(mps * 3.6 * 10.0) / 10.0);
            } else {
                out.put("wind", null);
            }

            out.put("condition", condition);

            return out;

        } catch (Exception ex) {
            return Map.of(
                    "error", "fallback-api-failure",
                    "message", ex.getMessage()
            );
        }
    }
}
