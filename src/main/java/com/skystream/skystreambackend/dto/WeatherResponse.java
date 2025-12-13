package com.skystream.skystreambackend.dto;

import java.util.List;
import java.util.Map;

/**
 * Unified weather response sent to frontend.
 * This is the SINGLE source of truth for UI.
 */
public class WeatherResponse {

    // ----------------------------
    // BASIC INFO
    // ----------------------------
    public String source;
    public String city;

    public Double temperature;
    public String condition;
    public String icon;

    public Double humidity;
    public Double wind;

    // ----------------------------
    // SUN / TIME
    // ----------------------------
    public String sunrise;
    public String sunset;

    // ----------------------------
    // AIR QUALITY
    // ----------------------------
    /**
     * AQI value scaled 0–500 (for UI bar)
     */
    public Integer aqi;

    /**
     * US EPA index (1–6)
     */
    public Integer epaIndex;

    /**
     * Raw air_quality block from WeatherAPI
     */
    public Map<String, Object> airQualityRaw;

    // ----------------------------
    // EXTRA METRICS
    // ----------------------------
    public Double uv;
    public Double visibility;

    // ----------------------------
    // FORECAST
    // ----------------------------
    public List<Map<String, Object>> hourly;
    public List<Map<String, Object>> daily;

    // ----------------------------
    // DEFAULT CONSTRUCTOR
    // ----------------------------
    public WeatherResponse() {
    }
}
