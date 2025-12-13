package com.skystream.skystreambackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skystream.skystreambackend.dto.ForecastDay;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class ForecastDatasetService {

    private Map<String, List<ForecastDay>> dataset;

    @PostConstruct
    public void load() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            InputStream is = getClass().getClassLoader().getResourceAsStream("forecast-dataset.json");

            dataset = mapper.readValue(is, new TypeReference<>() {});
            System.out.println("Loaded fallback forecast dataset!");
        } catch (Exception e) {
            System.out.println("Failed to load dataset: " + e.getMessage());
        }
    }

    public List<ForecastDay> getFallback(String city) {
        return dataset.getOrDefault(city, null);
    }
}
