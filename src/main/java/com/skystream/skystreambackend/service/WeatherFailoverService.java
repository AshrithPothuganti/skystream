package com.skystream.skystreambackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class WeatherFailoverService {

    @Autowired
    private WeatherApiClient api;

    @Autowired
    private OpenWeatherService openWeatherService;

    @Autowired
    private LocalCSVLoaderService csvService;

    @Autowired
    private ResourceLoader resourceLoader;

    private Map<String, Object> fallback;

    @PostConstruct
    public void init() {
        try {
            Resource res = resourceLoader.getResource("classpath:static/fallback-weatherapi.json");
            ObjectMapper mapper = new ObjectMapper();
            fallback = mapper.readValue(
                    res.getInputStream(),
                    new TypeReference<Map<String, Object>>() {}
            );
            System.out.println("✔ Loaded WeatherAPI fallback JSON");
        } catch (Exception e) {
            fallback = new HashMap<>();
            System.err.println("❌ Failed to load fallback JSON: " + e.getMessage());
        }
    }

    // =====================================================
    // CURRENT WEATHER
    // =====================================================
    public Map<String, Object> getWeather(String city) {

        // 1️⃣ WeatherAPI primary
        Map<String, Object> live = api.getCurrent(city);
        if (live != null && !live.containsKey("error")) {

            // 🔧 FIX AQI HERE (correct Java syntax)
            Object currentObj = live.get("current");
            if (currentObj instanceof Map<?, ?>) {

                @SuppressWarnings("unchecked")
                Map<String, Object> current = (Map<String, Object>) currentObj;

                Object airObj = current.get("air_quality");
                if (airObj instanceof Map<?, ?>) {

                    @SuppressWarnings("unchecked")
                    Map<String, Object> air = (Map<String, Object>) airObj;

                    Object epaObj = air.get("us-epa-index");
                    if (epaObj instanceof Number) {
                        int epa = ((Number) epaObj).intValue();

                        Integer aqi = switch (epa) {
                            case 1 -> 25;
                            case 2 -> 75;
                            case 3 -> 125;
                            case 4 -> 175;
                            case 5 -> 250;
                            case 6 -> 350;
                            default -> null;
                        };

                        live.put("aqi", aqi); // ✅ AQI exposed to frontend
                    }
                }
            }

            return live;
        }

        // 2️⃣ OpenWeather fallback
        Map<String, Object> ow = openWeatherService.fetchCurrent(city);
        if (ow != null && !ow.containsKey("error")) {
            return ow;
        }

        // 3️⃣ CSV fallback
        Map<String, Object> csv = findInLocalCsv(city);
        if (csv != null) {
            return csv;
        }

        // 4️⃣ Static JSON fallback
        if (fallback != null && !fallback.isEmpty()) {
            return fallback;
        }

        return Map.of("error", "no-valid-data");
    }

    // =====================================================
    // FORECAST
    // =====================================================
    public Map<String, Object> getForecast(String city, int days) {

        Map<String, Object> live = api.getForecast(city, days);
        if (live != null && !live.containsKey("error")) {
            return live;
        }

        if (fallback.containsKey("forecast")) {
            return fallback;
        }

        return Map.of("error", "forecast-not-available");
    }

    // =====================================================
    // CSV FALLBACK
    // =====================================================
    private Map<String, Object> findInLocalCsv(String city) {
        return csvService.getGlobalData().stream()
                .filter(r -> city.equalsIgnoreCase(r.get("city")))
                .findFirst()
                .map(this::convertCsvRow)
                .orElse(null);
    }

    private Map<String, Object> convertCsvRow(Map<String, String> row) {
        Map<String, Object> out = new HashMap<>();
        out.put("city", row.get("city"));
        out.put("temperature", toDouble(row.get("temperature")));
        out.put("humidity", toDouble(row.get("humidity")));
        out.put("wind", toDouble(row.get("wind")));
        out.put("condition", row.getOrDefault("condition", "Unknown"));
        out.put("source", "CSV");
        return out;
    }

    private Double toDouble(String v) {
        try {
            return v == null ? null : Double.parseDouble(v);
        } catch (Exception e) {
            return null;
        }
    }
}
