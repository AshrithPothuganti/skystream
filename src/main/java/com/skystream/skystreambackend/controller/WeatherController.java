package com.skystream.skystreambackend.controller;

import com.skystream.skystreambackend.service.WeatherApiClient;
import com.skystream.skystreambackend.service.WeatherFailoverService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/weather")
@CrossOrigin
public class WeatherController {

    private final WeatherApiClient apiClient;
    private final WeatherFailoverService failover;

    public WeatherController(
            WeatherApiClient apiClient,
            WeatherFailoverService failover
    ) {
        this.apiClient = apiClient;
        this.failover = failover;
    }

    // ---------------- CURRENT WEATHER ----------------
    @GetMapping("/current")
    public Map<String, Object> getCurrent(@RequestParam String city) {
        return failover.getWeather(city);
    }

    // ---------------- FORECAST (RAW WeatherAPI) ----------------
    @GetMapping("/forecast")
    public Map<String, Object> getForecast(
            @RequestParam String city,
            @RequestParam(defaultValue = "7") int days
    ) {
        Map<String, Object> forecast = apiClient.getForecast(city, days);

        if (forecast != null && !forecast.containsKey("error")) {
            return forecast;
        }

        // fallback JSON (already WeatherAPI-shaped)
        return failover.getForecast(city, days);
    }
}
