package com.skystream.skystreambackend.service;

import com.opencsv.CSVReader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Simple CSV loader that reads a CSV from classpath:/data/{fileName}
 * and returns the rows as List<String[]> (first row included — caller may skip header).
 */
@Service
public class CsvLoader {

    /**
     * Load a CSV from src/main/resources/data/{fileName}
     * @param fileName example: "GlobalWeatherRepository.csv"
     * @return list of rows (String[]), or empty list on error
     */
    public List<String[]> load(String fileName) {
        // try-with-resources ensures both the InputStream and CSVReader are closed
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("data/" + fileName)) {
            if (is == null) {
                System.out.println("CSV NOT FOUND: " + fileName);
                return Collections.emptyList();
            }

            try (CSVReader reader = new CSVReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.readAll();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
