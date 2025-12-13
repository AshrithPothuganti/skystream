package com.skystream.skystreambackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RealTimeWeatherService {

    @Autowired
    private WeatherApiClient client;

    public Map<String, Object> fetchCurrent(String city) {
        try {
            Map<String, Object> resp = client.getCurrent(city);
            if (resp == null || resp.containsKey("error")) {
                return Map.of(
                    "error", "primary-api-failed",
                    "message", resp != null ? resp.get("message") : "null response"
                );
            }

            // --------------------------
            // SAFE CASTS WITH FALLBACKS
            // --------------------------
            Map<String, Object> location = safeMap(resp.get("location"));
            Map<String, Object> current  = safeMap(resp.get("current"));
            Map<String, Object> cond     = safeMap(current.get("condition"));

            String cityName     = str(location.get("name"), city);
            String lastUpdated  = str(current.get("last_updated"), "");
            Object tempC        = current.getOrDefault("temp_c", null);
            Object humidity     = current.getOrDefault("humidity", null);
            Object windKph      = current.getOrDefault("wind_kph", null);
            String conditionTxt = str(cond.get("text"), "");

            // ------------------------------------------
            // BUILD FINAL WEATHER DATA FOR FRONTEND
            // ------------------------------------------
            Map<String, Object> out = new HashMap<>();
            out.put("city", cityName);
            out.put("date", lastUpdated);
            out.put("temperature", tempC);
            out.put("high", tempC);
            out.put("low", tempC);
            out.put("humidity", humidity);
            out.put("wind", windKph);
            out.put("condition", conditionTxt);
            out.put("source", "WeatherAPI.com");

            return out;

        } catch (Exception ex) {
            return Map.of(
                "error", "primary-api-failed",
                "message", ex.getMessage()
            );
        }
    }

    // ---------------------
    // HELPERS
    // ---------------------

    /** Safe cast to Map<String,Object>. Avoids ClassCastException */
    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object obj) {
        return (obj instanceof Map<?, ?> m) 
                ? (Map<String, Object>) m 
                : new HashMap<>();
    }

    /** Safe string conversion */
    private String str(Object obj, String fallback) {
        return obj != null ? obj.toString() : fallback;
    }
}

