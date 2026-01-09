package com.example.weatherapp;

public class MonthlyData {
    private int day;
    private double high;
    private double low;
    private String condition;

    public MonthlyData(int day, double high, double low, String condition) {
        this.day = day;
        this.high = high;
        this.low = low;
        this.condition = condition;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}