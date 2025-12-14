package com.skystream.skystreambackend.controller;

import com.skystream.skystreambackend.dto.WeatherResponse;
import com.skystream.skystreambackend.service.*;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherFailoverService failoverService;
    private final WeatherMapper mapper;
    private final WeatherApiClient api;
    private final IPLocationService ipService;

    // ✅ SINGLE constructor, ALL dependencies injected safely
    public WeatherController(
            WeatherFailoverService failoverService,
            WeatherMapper mapper,
            WeatherApiClient api,
            IPLocationService ipService
    ) {
        this.failoverService = failoverService;
        this.mapper = mapper;
        this.api = api;
        this.ipService = ipService;
    }

    // ---------------- CURRENT WEATHER ----------------
    @GetMapping("/current")
    public ResponseEntity<WeatherResponse> current(@RequestParam String city) {
        Map<String, Object> raw = failoverService.getWeather(city);
        WeatherResponse resp = mapper.mapToUnified(raw);
        return ResponseEntity.ok(resp);
    }

    // ---------------- FORECAST ----------------
    @GetMapping("/forecast")
    public ResponseEntity<?> forecast(
            @RequestParam String city,
            @RequestParam(defaultValue = "7") int days) {

        Map<String, Object> raw = failoverService.getForecast(city, days);
        return ResponseEntity.ok(raw);
    }

    // ---------------- SEARCH / AUTOCOMPLETE ----------------
    @GetMapping("/search")
   public ResponseEntity<?> search(@RequestParam String q) {

    try {
        List<Map<String, Object>> list = api.searchCitiesTyped(q);

        return ResponseEntity.ok(
                Map.of("results", list)
        );

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(500).body(
                Map.of(
                        "error", "search-failed",
                        "message", e.getMessage()
                )
        );
    }
}


    // ---------------- IP LOCATION ----------------
    @GetMapping("/location/ip")
    public ResponseEntity<?> detectCityFromIP(HttpServletRequest request) {

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null) ip = request.getRemoteAddr();

        Map<String, Object> data = ipService.lookup(ip);

        return ResponseEntity.ok(
                Map.of(
                        "ip", ip,
                        "city", data.get("city"),
                        "raw", data
                )
        );
    }
}
