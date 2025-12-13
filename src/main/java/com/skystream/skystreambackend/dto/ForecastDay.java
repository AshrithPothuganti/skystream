package com.skystream.skystreambackend.dto;

public class ForecastDay {
    private String date;
    private String condition;
    private double max;
    private double min;

    public ForecastDay() {}

    public ForecastDay(String date, String condition, double max, double min) {
        this.date = date;
        this.condition = condition;
        this.max = max;
        this.min = min;
    }

    public String getDate() { return date; }
    public String getCondition() { return condition; }
    public double getMax() { return max; }
    public double getMin() { return min; }

    public void setDate(String date) { this.date = date; }
    public void setCondition(String condition) { this.condition = condition; }
    public void setMax(double max) { this.max = max; }
    public void setMin(double min) { this.min = min; }
}
