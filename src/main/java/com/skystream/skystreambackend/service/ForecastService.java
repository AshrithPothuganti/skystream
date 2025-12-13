package com.skystream.skystreambackend.service;

import com.skystream.skystreambackend.dto.ForecastDay;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForecastService {

    private final WeatherForecastApiClient weatherApi;
    private final ForecastDatasetService fallback;

    public ForecastService(WeatherForecastApiClient weatherApi, ForecastDatasetService fallback) {
        this.weatherApi = weatherApi;
        this.fallback = fallback;
    }

    public List<ForecastDay> getForecast(String city, int days) {

        List<ForecastDay> fromApi = weatherApi.getForecast(city, days);

        if (fromApi != null && !fromApi.isEmpty()) {
            return fromApi;
        }

        return fallback.getFallback(city);
    }
}
