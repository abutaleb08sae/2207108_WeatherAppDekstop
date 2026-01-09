package com.example.weatherapp;

import java.util.List;

public class WeatherData {
    private String city;
    private String countryCode;
    private double temp;
    private String description;
    private int humidity;
    private double windSpeed;
    private int pressure;
    private int aqi;
    private double latitude;
    private double longitude;
    private String sunrise;
    private String sunset;
    private List<HourlyPoint> hourlyForecast;

    public record HourlyPoint(String time, double temp, String condition, int rainChance) {}

    public WeatherData(String city, String countryCode, double temp, String description, int humidity,
                       double windSpeed, int pressure, int aqi, double latitude,
                       double longitude, String sunrise, String sunset) {
        this.city = city;
        this.countryCode = countryCode;
        this.temp = temp;
        this.description = description;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.pressure = pressure;
        this.aqi = aqi;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sunrise = sunrise;
        this.sunset = sunset;
    }

    public String getCity() { return city; }
    public String getCountryCode() { return countryCode; }
    public double getTemp() { return temp; }
    public String getDescription() { return description; }
    public int getHumidity() { return humidity; }
    public double getWindSpeed() { return windSpeed; }
    public int getPressure() { return pressure; }
    public int getAqi() { return aqi; }
    public int getAqiValue() { return aqi; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getSunrise() { return sunrise; }
    public String getSunset() { return sunset; }

    // Dashboard specific mocks/methods for UI parity
    public int getUvIndex() { return 4; }
    public String getWindDirection() { return "N"; }
    public double getWindGust() { return windSpeed * 1.2; }
    public String getMoonrise() { return "11:24 PM"; }
    public String getMoonset() { return "11:18 AM"; }
    public String getMoonPhase() { return "63%"; }
    public double getPrecipitation() { return 0.0; }
    public int getCloudCover() { return 32; }

    public List<HourlyPoint> getHourlyForecast() { return hourlyForecast; }
    public void setHourlyForecast(List<HourlyPoint> hourlyForecast) { this.hourlyForecast = hourlyForecast; }

    public String getAqiText() {
        return switch (aqi) {
            case 1 -> "Good";
            case 2 -> "Fair";
            case 3 -> "Moderate";
            case 4 -> "Poor";
            case 5 -> "Very Poor";
            default -> "Unknown";
        };
    }
}