package com.skystream.skystreambackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skystream.skystreambackend.dto.ForecastDay;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WeatherForecastApiClient {

    @Value("${weather.api.key}")
    private String apiKey;

    private final ObjectMapper mapper = new ObjectMapper();

    public List<ForecastDay> getForecast(String city, int days) {
        try {
            String url = "http://api.weatherapi.com/v1/forecast.json?key=" + apiKey +
                    "&q=" + city + "&days=" + days + "&aqi=yes";

            JsonNode root = mapper.readTree(new java.net.URL(url));

            List<ForecastDay> list = new ArrayList<>();

            JsonNode arr = root.get("forecast").get("forecastday");

            for (JsonNode node : arr) {
                list.add(new ForecastDay(
                        node.get("date").asText(),
                        node.get("day").get("condition").get("text").asText(),
                        node.get("day").get("maxtemp_c").asDouble(),
                        node.get("day").get("mintemp_c").asDouble()
                ));
            }

            return list;

        } catch (Exception e) {
            System.out.println("WeatherAPI forecast failed: " + e.getMessage());
            return null;
        }
    }
}
