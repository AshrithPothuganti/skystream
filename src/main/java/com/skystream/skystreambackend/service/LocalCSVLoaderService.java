package com.skystream.skystreambackend.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class LocalCSVLoaderService {

    @Value("${fallback.dataset.global}")
    private String globalPath;

    @Value("${fallback.dataset.daily}")
    private String dailyPath;

    private final List<Map<String, String>> globalData = new ArrayList<>();
    private final List<Map<String, String>> dailyData = new ArrayList<>();

    @PostConstruct
    public void init() {
        System.out.println("===== CSV FALLBACK LOADER START =====");

        loadCsv(globalPath, globalData, "Global");
        loadCsv(dailyPath, dailyData, "Daily");

        System.out.println("===== CSV FALLBACK LOADER END =====");
    }

    private void loadCsv(String path,
                         List<Map<String, String>> target,
                         String label) {

        try {
            Resource resource = new ClassPathResource(path);

            if (!resource.exists()) {
                System.err.println("❌ " + label + " CSV not found: " + path);
                return;
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                String headerLine = br.readLine();
                if (headerLine == null) return;

                String[] headers = headerLine.split(",");

                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",", -1);
                    Map<String, String> row = new HashMap<>();

                    for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                        row.put(headers[i].trim(), values[i].trim());
                    }

                    target.add(row);
                }
            }

            System.out.println("✔ Loaded " + label + " CSV rows: " + target.size());

        } catch (Exception e) {
            System.err.println("❌ " + label + " CSV load failed: " + e.getMessage());
        }
    }

    public List<Map<String, String>> getGlobalData() {
        return globalData;
    }

    public List<Map<String, String>> getDailyData() {
        return dailyData;
    }
}
