package com.skystream.skystreambackend.service;

import com.skystream.skystreambackend.dto.WeatherResponse;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Converts different weather provider responses into a unified WeatherResponse.
 */
@Component
public class WeatherMapper {

    // =====================================================
    // PUBLIC ENTRY POINT
    // =====================================================
    public WeatherResponse mapToUnified(Map<String, Object> data) {
        if (data == null) return empty();

        // Already unified
        if (data.containsKey("temperature")) {
            return fromUnified(data);
        }

        // WeatherAPI.com
        if (data.containsKey("location") && data.containsKey("current")) {
            return fromWeatherApi(data);
        }

        // OpenWeatherMap
        if (data.containsKey("main") && data.containsKey("weather")) {
            return fromOpenWeather(data);
        }

        // CSV fallback
        if (data.containsKey("city") && data.containsKey("condition")) {
            return fromCsv(data);
        }

        WeatherResponse r = empty();
        r.source = "unknown";
        return r;
    }

    // =====================================================
    // WEATHERAPI.COM
    // =====================================================
    private WeatherResponse fromWeatherApi(Map<String, Object> data) {
        WeatherResponse out = new WeatherResponse();
        out.source = "WeatherAPI.com";

        Map<String, Object> location = map(data.get("location"));
        Map<String, Object> current = map(data.get("current"));
        Map<String, Object> condition = map(current.get("condition"));

        out.city = str(location.get("name"));
        out.temperature = dbl(current.get("temp_c"));
        out.condition = str(condition.get("text"));
        out.icon = str(condition.get("icon"));
        out.humidity = dbl(current.get("humidity"));
        out.wind = dbl(current.get("wind_kph"));
        out.uv = dbl(current.get("uv"));
        out.visibility = dbl(current.get("vis_km"));

        // ---------------- AQI ----------------
       // ---------------- AQI ----------------
Map<String, Object> air = map(current.get("air_quality"));
Integer epa = intVal(air.get("us-epa-index"));

out.epaIndex = epa;
out.airQualityRaw = air;

// Always expose AQI for frontend
out.aqi = epa != null ? mapEpaToAqi(epa) : null;

        // ---------------- FORECAST ----------------
        Map<String, Object> forecast = map(data.get("forecast"));
        List<Map<String, Object>> days = list(forecast.get("forecastday"));

        if (!days.isEmpty()) {
            Map<String, Object> day0 = map(days.get(0));
            Map<String, Object> astro = map(day0.get("astro"));

            out.sunrise = str(astro.get("sunrise"));
            out.sunset = str(astro.get("sunset"));

            // HOURLY
            List<Map<String, Object>> hours = list(day0.get("hour"));
            List<Map<String, Object>> hourly = new ArrayList<>();

            for (Map<String, Object> h : hours) {
                Map<String, Object> hc = map(h.get("condition"));
                Map<String, Object> m = new HashMap<>();
                m.put("time", h.get("time"));
                m.put("temp", h.get("temp_c"));
                m.put("condition", hc.get("text"));
                m.put("icon", hc.get("icon"));
                hourly.add(m);
            }
            out.hourly = hourly;

            // DAILY
            List<Map<String, Object>> daily = new ArrayList<>();
            for (Map<String, Object> d : days) {
                Map<String, Object> day = map(d.get("day"));
                Map<String, Object> dc = map(day.get("condition"));

                Map<String, Object> dm = new HashMap<>();
                dm.put("date", d.get("date"));
                dm.put("max", day.get("maxtemp_c"));
                dm.put("min", day.get("mintemp_c"));
                dm.put("condition", dc.get("text"));
                daily.add(dm);
            }
            out.daily = daily;
        } 
        
        


        return out;
    }

    // =====================================================
    // OPENWEATHERMAP
    // =====================================================
    private WeatherResponse fromOpenWeather(Map<String, Object> data) {
        WeatherResponse out = new WeatherResponse();
        out.source = "OpenWeatherMap";

        out.city = str(data.get("name"));

        Map<String, Object> main = map(data.get("main"));
        Map<String, Object> wind = map(data.get("wind"));

        out.temperature = dbl(main.get("temp"));
        out.humidity = dbl(main.get("humidity"));

        Double windMps = dbl(wind.get("speed"));
        out.wind = windMps != null ? Math.round(windMps * 3.6 * 10) / 10.0 : null;

        List<Map<String, Object>> arr = list(data.get("weather"));
        if (!arr.isEmpty()) {
            out.condition = str(arr.get(0).get("description"));
        }

        out.hourly = Collections.emptyList();
        out.daily = Collections.emptyList();
        return out;
    }

    // =====================================================
    // UNIFIED MAP
    // =====================================================
    private WeatherResponse fromUnified(Map<String, Object> data) {
        WeatherResponse out = new WeatherResponse();

        out.source = str(data.getOrDefault("source", "unified"));
        out.city = str(data.get("city"));
        out.temperature = dbl(data.get("temperature"));
        out.condition = str(data.get("condition"));
        out.humidity = dbl(data.get("humidity"));
        out.wind = dbl(data.get("wind"));
        out.sunrise = str(data.get("sunrise"));
        out.sunset = str(data.get("sunset"));
        out.hourly = list(data.get("hourly"));
        out.daily = list(data.get("daily"));

        return out;
    }

    // =====================================================
    // CSV FALLBACK
    // =====================================================
    private WeatherResponse fromCsv(Map<String, Object> row) {
        WeatherResponse out = new WeatherResponse();
        out.source = "Offline CSV";

        out.city = str(row.get("city"));
        out.temperature = dbl(row.get("temperature"));
        out.humidity = dbl(row.get("humidity"));
        out.wind = dbl(row.get("wind"));
        out.condition = str(row.get("condition"));

        return out;
    }

    // =====================================================
    // HELPERS
    // =====================================================
    private WeatherResponse empty() {
        WeatherResponse r = new WeatherResponse();
        r.city = "--";
        r.condition = "--";
        r.hourly = Collections.emptyList();
        r.daily = Collections.emptyList();
        return r;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> list(Object o) {
        if (o instanceof List<?>) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object i : (List<?>) o) {
                if (i instanceof Map) out.add((Map<String, Object>) i);
            }
            return out;
        }
        return new ArrayList<>();
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private Double dbl(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return o != null ? Double.parseDouble(o.toString()) : null; }
        catch (Exception e) { return null; }
    }

    private Integer intVal(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return o != null ? Integer.parseInt(o.toString()) : null; }
        catch (Exception e) { return null; }
    }

    private Integer mapEpaToAqi(Integer epa) {
        if (epa == null) return null;
        return switch (epa) {
            case 1 -> 25;
            case 2 -> 75;
            case 3 -> 125;
            case 4 -> 175;
            case 5 -> 250;
            case 6 -> 350;
            default -> null;
        };
    }
}
