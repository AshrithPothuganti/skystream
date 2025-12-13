package com.skystream.skystreambackend.service;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class FuzzySearchService {

    private static final LevenshteinDistance LD = new LevenshteinDistance();

    /**
     * Returns the closest matched city from WeatherAPI search list.
     */
    public Map<String, Object> bestMatch(String query, List<Map<String, Object>> options) {

        if (options == null || options.isEmpty()) {
            return null;
        }

        query = query.toLowerCase();

        Map<String, Object> best = null;
        int bestScore = Integer.MAX_VALUE;

        for (Map<String, Object> city : options) {
            String name = city.getOrDefault("name", "").toString().toLowerCase();

            int dist = LD.apply(query, name);

            if (dist < bestScore) {
                bestScore = dist;
                best = city;
            }
        }

        // threshold to avoid terrible matches
        if (bestScore > 5) return null;

        return best;
    }
}
