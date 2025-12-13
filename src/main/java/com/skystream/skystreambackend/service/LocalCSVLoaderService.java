package com.skystream.skystreambackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;  // <-- FIXED

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;


@Service
public class LocalCSVLoaderService {

    @Value("${fallback.dataset.global}")
    private Resource globalCsv;

    @Value("${fallback.dataset.daily}")
    private Resource dailyCsv;

    private List<Map<String, String>> globalData = new ArrayList<>();
    private List<Map<String, String>> dailyData = new ArrayList<>();

    @PostConstruct
    public void init() {
        System.out.println("\n========= CSV FALLBACK LOADER =========");

        globalData = loadCsv(globalCsv);
        System.out.println("✔ Loaded GlobalWeatherRepository.csv → rows = " + globalData.size());

        dailyData = loadCsv(dailyCsv);
        System.out.println("✔ Loaded history_latest.csv → rows = " + dailyData.size());

        System.out.println("=======================================\n");
    }

    /** Load CSV resource into List<Map<String,String>> safely */
    private List<Map<String, String>> loadCsv(Resource res) {
        List<Map<String, String>> rows = new ArrayList<>();

        try {
            if (res == null || !res.exists()) {
                System.err.println("⚠ CSV resource not found: " + res);
                return rows;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {

                String headerLine = reader.readLine();
                if (headerLine == null) return rows;

                String[] headers = safeSplit(headerLine);

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] values = safeSplit(line);

                    Map<String, String> row = new HashMap<>();
                    for (int i = 0; i < headers.length && i < values.length; i++) {
                        row.put(headers[i].trim(), values[i].trim());
                    }
                    rows.add(row);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ CSV load failed: " + e.getMessage());
        }

        return rows;
    }

    /** Better CSV splitting supporting values like "New York, USA" */
    private String[] safeSplit(String line) {
        List<String> tokens = new ArrayList<>();
        boolean insideQuotes = false;
        StringBuilder sb = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                insideQuotes = !insideQuotes;
            } else if (c == ',' && !insideQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());

        return tokens.toArray(new String[0]);
    }

    public List<Map<String, String>> getGlobalData() {
        return globalData;
    }

    public List<Map<String, String>> getDailyData() {
        return dailyData;
    }
}
