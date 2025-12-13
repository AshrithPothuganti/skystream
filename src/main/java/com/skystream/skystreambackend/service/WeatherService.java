package com.skystream.skystreambackend.service;

import com.opencsv.CSVReader;
import com.skystream.skystreambackend.model.WeatherRecord;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.annotation.PostConstruct;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Robust CSV loader + in-memory index for forgiving city search.
 * - Auto-detects CSV header columns (common names)
 * - Indexes variants: normalized, trimmed, tokens prefixes
 * - Search: exact -> prefix -> contains
 */
@Service
public class WeatherService {

    // Map normalizedKey -> list of WeatherRecord (keep first = latest/simple)
    private final Map<String, List<WeatherRecord>> cityIndex = new ConcurrentHashMap<>();

    // Keep a set of canonical city names for suggestions
    private final Set<String> canonicalCities = Collections.synchronizedSet(new HashSet<>());

    @PostConstruct
    public void init() {
        loadAllCsvs();
        System.out.println("WeatherService initialized. Cities indexed: " + canonicalCities.size());
    }

    private void loadAllCsvs() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            // finds every CSV file under resources/data (recursive)
            Resource[] resources = resolver.getResources("classpath*:data/**/*.csv");

            for (Resource res : resources) {
                try (CSVReader reader = new CSVReader(new InputStreamReader(res.getInputStream(), StandardCharsets.UTF_8))) {
                    List<String[]> all = reader.readAll();
                    if (all.isEmpty()) continue;

                    String[] header = all.get(0);
                    Map<String,Integer> headerIndex = indexHeader(header);

                    // for each data row
                    for (int i = 1; i < all.size(); i++) {
                        String[] row = all.get(i);
                        if (row.length == 0) continue;

                        String city = getValue(row, headerIndex, new String[]{"location_name","location","city","name","loc","station"});
                        if (city == null || city.isBlank()) continue;
                        city = city.trim();

                        String date = getValue(row, headerIndex, new String[]{"date","timestamp","datetime"});
                        if (date == null || date.isBlank()) date = Instant.now().toString();

                        double temp = parseDouble(getValue(row, headerIndex, new String[]{"temp","temperature","air_temperature","t"}));
                        double high = parseDouble(getValue(row, headerIndex, new String[]{"high","temp_max","tmax"}));
                        double low  = parseDouble(getValue(row, headerIndex, new String[]{"low","temp_min","tmin"}));
                        int humidity = (int) Math.round(parseDouble(getValue(row, headerIndex, new String[]{"humidity","rh","rel_humidity"})));
                        double wind = parseDouble(getValue(row, headerIndex, new String[]{"wind","windspeed","wind_speed","wind_kph","wind_mph"}));
                        String cond = getValue(row, headerIndex, new String[]{"condition","weather","weather_description","desc"});

                        WeatherRecord wr = new WeatherRecord(
                                city,
                                date,
                                Double.isFinite(temp) ? temp : 0,
                                Double.isFinite(high) ? high : (Double.isFinite(temp) ? temp : 0),
                                Double.isFinite(low) ? low : (Double.isFinite(temp) ? temp : 0),
                                humidity,
                                Double.isFinite(wind) ? wind : 0,
                                cond != null ? cond : ""
                        );

                        // index under several normalized keys
                        addToIndex(city, wr);
                    }

                } catch (Exception e) {
                    System.err.println("Failed to parse CSV " + res.getFilename() + " : " + e.getMessage());
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // build header map name->index (lowercased)
    private Map<String,Integer> indexHeader(String[] header) {
        Map<String,Integer> map = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            if (header[i] == null) continue;
            map.put(header[i].trim().toLowerCase(), i);
        }
        return map;
    }

    // get value by trying multiple candidate header names
    private String getValue(String[] row, Map<String,Integer> headerIndex, String[] candidates) {
        for (String c : candidates) {
            Integer idx = headerIndex.get(c.toLowerCase());
            if (idx != null && idx < row.length) {
                String v = row[idx];
                if (v != null && !v.isBlank()) return v;
            }
        }
        // fallback: try any header that contains candidate substring
        for (String c : candidates) {
            for (Map.Entry<String,Integer> e : headerIndex.entrySet()) {
                if (e.getKey().contains(c.toLowerCase())) {
                    int idx = e.getValue();
                    if (idx < row.length) {
                        String v = row[idx];
                        if (v != null && !v.isBlank()) return v;
                    }
                }
            }
        }
        return null;
    }

    private static double parseDouble(String s) {
        try {
            if (s == null) return Double.NaN;
            String cleaned = s.replaceAll("[^0-9+\\-\\.eE]", "");
            if (cleaned.isBlank()) return Double.NaN;
            return Double.parseDouble(cleaned);
        } catch (Exception ex) {
            return Double.NaN;
        }
    }

    // Normalize string: lower, trim, remove diacritics, collapse whitespace
    private static String normalize(String s) {
        if (s == null) return "";
        String t = s.trim().toLowerCase(Locale.ROOT);
        t = Normalizer.normalize(t, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", ""); // remove diacritics
        t = t.replaceAll("[^a-z0-9\\s,-]", ""); // keep alnum, space, comma, dash
        t = t.replaceAll("\\s+", " ");
        return t;
    }

    // Add multiple keys for flexible lookup
    private void addToIndex(String city, WeatherRecord wr) {
        String canonical = city.trim();
        canonicalCities.add(canonical);

        String norm = normalize(canonical);               // full normalized
        addKey(norm, wr);

        // add tokens, prefixes for multi-word city names
        String[] parts = norm.split("[,\\-\\s]+");
        // full tokens and prefixes
        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) prefix.append(" ");
            prefix.append(parts[i]);
            addKey(prefix.toString(), wr); // prefixes: "new", "new york", "new york city"
        }
        // also add individual token keys
        for (String p : parts) {
            if (!p.isBlank()) addKey(p, wr);
        }
    }

    private void addKey(String key, WeatherRecord wr) {
        cityIndex.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>())).add(wr);
    }

    // Public: get best match for a city string (exact -> prefix -> contains)
    public WeatherRecord getCurrentWeather(String city) {
        if (city == null || city.isBlank()) return null;
        String q = normalize(city);

        // exact normalized match
        List<WeatherRecord> exact = cityIndex.get(q);
        if (exact != null && !exact.isEmpty()) return exact.get(0);

        // prefix match (entries starting with q)
        Optional<List<WeatherRecord>> prefix = cityIndex.entrySet().stream()
                .filter(e -> e.getKey().startsWith(q))
                .map(Map.Entry::getValue)
                .findFirst();
        if (prefix.isPresent() && !prefix.get().isEmpty()) return prefix.get().get(0);

        // contains match (key contains q)
        Optional<List<WeatherRecord>> contains = cityIndex.entrySet().stream()
                .filter(e -> e.getKey().contains(q))
                .map(Map.Entry::getValue)
                .findFirst();
        if (contains.isPresent() && !contains.get().isEmpty()) return contains.get().get(0);

        // try canonical names contains (case-insensitive)
        Optional<String> canon = canonicalCities.stream()
                .filter(s -> s.toLowerCase().contains(city.trim().toLowerCase()))
                .findFirst();
        if (canon.isPresent()) {
            List<WeatherRecord> list = cityIndex.get(normalize(canon.get()));
            if (list != null && !list.isEmpty()) return list.get(0);
        }

        return null;
    }

    // Autocomplete / search suggestions
    // Autocomplete / search suggestions
// Autocomplete / search suggestions
@GetMapping("/search")
public Map<String, Object> search(@RequestParam String q) {

    // Search canonicalCities for matches to q
    String query = q == null ? "" : q.trim().toLowerCase();
    List<Map<String, Object>> results = canonicalCities.stream()
            .filter(city -> city.toLowerCase().contains(query))
            .limit(10)
            .map(city -> {
                Map<String, Object> map = new HashMap<>();
                map.put("name", city);
                // Optionally, you can add region/country if available in WeatherRecord or elsewhere
                return map;
            })
            .collect(Collectors.toList());

    return Map.of(
        "results", results,
        "bestMatch", results.isEmpty() ? null : results.get(0)
    );
}




}
