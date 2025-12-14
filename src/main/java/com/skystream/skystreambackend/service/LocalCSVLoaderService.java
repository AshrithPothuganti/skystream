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

    private volatile boolean loaded = false;

    @PostConstruct
    public void init() {
        System.out.println("LocalCSVLoaderService initialized (lazy mode)");
    }

    private synchronized void ensureLoaded() {
        if (loaded) return;

        loadCsv(globalPath, globalData, "Global", 5000);
        loadCsv(dailyPath, dailyData, "Daily", 5000);

        loaded = true;
        System.out.println("CSV fallback datasets loaded");
    }

    private void loadCsv(String path,
                         List<Map<String, String>> target,
                         String label,
                         int maxRows) {

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
                int count = 0;

                while ((line = br.readLine()) != null && count < maxRows) {
                    String[] values = line.split(",", -1);
                    Map<String, String> row = new HashMap<>();

                    for (int i = 0; i < Math.min(headers.length, values.length); i++) {
                        row.put(headers[i].trim().toLowerCase(), values[i].trim());
                    }

                    target.add(row);
                    count++;
                }
            }

            System.out.println("✔ Loaded " + label + " CSV rows: " + target.size());

        } catch (Exception e) {
            System.err.println("❌ " + label + " CSV load failed: " + e.getMessage());
        }
    }

    public List<Map<String, String>> getGlobalData() {
        ensureLoaded();
        return globalData;
    }

    public List<Map<String, String>> getDailyData() {
        ensureLoaded();
        return dailyData;
    }
}
