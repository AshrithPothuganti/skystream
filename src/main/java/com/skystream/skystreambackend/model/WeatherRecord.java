package com.skystream.skystreambackend.model;

public class WeatherRecord {

    private String city;
    private String date;
    private double temperature;
    private double high;
    private double low;
    private int humidity;
    private double wind;
    private String condition;

    public WeatherRecord() {}

    public WeatherRecord(String city, String date, double temperature, double high, double low, int humidity, double wind, String condition) {
        this.city = city;
        this.date = date;
        this.temperature = temperature;
        this.high = high;
        this.low = low;
        this.humidity = humidity;
        this.wind = wind;
        this.condition = condition;
    }

    // Getters and setters
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHigh() { return high; }
    public void setHigh(double high) { this.high = high; }

    public double getLow() { return low; }
    public void setLow(double low) { this.low = low; }

    public int getHumidity() { return humidity; }
    public void setHumidity(int humidity) { this.humidity = humidity; }

    public double getWind() { return wind; }
    public void setWind(double wind) { this.wind = wind; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
}
