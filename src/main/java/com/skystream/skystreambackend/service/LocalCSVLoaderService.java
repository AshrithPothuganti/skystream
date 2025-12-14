package com.skystream.skystreambackend.service;



import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class LocalCSVLoaderService {

    private final List<Map<String, String>> globalData = new ArrayList<>();
    private final List<Map<String, String>> dailyData = new ArrayList<>();

    @PostConstruct
    public void init() {
        System.out.println("\n===== CSV FALLBACK LOADER START =====");

        loadCsv("data/GlobalWeatherRepository.csv", globalData);
        loadCsv("data/history_latest.csv", dailyData);

        System.out.println("Loaded Global CSV rows: " + globalData.size());
        System.out.println("Loaded Daily CSV rows: " + dailyData.size());

        System.out.println("===== CSV FALLBACK LOADER END =====\n");
    }

    private void loadCsv(String path, List<Map<String, String>> target) {
        try {
            Resource resource = new ClassPathResource(path);

            if (!resource.exists()) {
                System.err.println("CSV not found: " + path);
                return;
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                String headerLine = br.readLine();
                if (headerLine == null) return;

                String[] headers = headerLine.split(",");

                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");

                    Map<String, String> row = new HashMap<>();
                    for (int i = 0; i < headers.length && i < values.length; i++) {
                        row.put(headers[i].trim(), values[i].trim());
                    }
                    target.add(row);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to load CSV: " + path, e);
        }
    }

    public List<Map<String, String>> getGlobalData() {
        return globalData;
    }

    public List<Map<String, String>> getDailyData() {
        return dailyData;
    }
}
