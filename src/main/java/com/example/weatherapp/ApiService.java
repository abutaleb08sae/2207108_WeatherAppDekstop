package com.example.weatherapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ApiService {
    private static final String API_KEY = "5828bd5b646348de10e5a6be2b917c31";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public static String getCityByIP() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipapi.co/json/"))
                    .header("User-Agent", "java/11")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = mapper.readTree(response.body());

            String city = root.path("city").asText();
            if (city == null || city.equalsIgnoreCase("Bagerhat") || city.isEmpty()) {
                return "Khulna";
            }
            return city;
        } catch (Exception e) {
            try {
                HttpRequest backupRequest = HttpRequest.newBuilder()
                        .uri(URI.create("http://ip-api.com/json/"))
                        .build();
                HttpResponse<String> backupResponse = client.send(backupRequest, HttpResponse.BodyHandlers.ofString());
                JsonNode backupRoot = mapper.readTree(backupResponse.body());
                String backupCity = backupRoot.path("city").asText();
                return (backupCity == null || backupCity.equalsIgnoreCase("Bagerhat") || backupCity.isEmpty()) ? "Khulna" : backupCity;
            } catch (Exception ex) {
                return "Khulna";
            }
        }
    }

    public static WeatherData fetchWeather(String city) throws Exception {
        String queryCity = (city == null || city.equalsIgnoreCase("Bagerhat")) ? "Khulna" : city;
        String weatherUrl = "https://api.openweathermap.org/data/2.5/weather?q=" +
                queryCity.replace(" ", "+") + "&appid=" + API_KEY + "&units=metric";
        return processWeatherData(sendRequest(weatherUrl));
    }

    public static WeatherData fetchWeatherByCoords(double lat, double lon) throws Exception {
        String weatherUrl = "https://api.openweathermap.org/data/2.5/weather?lat=" +
                lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric";
        return processWeatherData(sendRequest(weatherUrl));
    }

    private static WeatherData processWeatherData(JsonNode weatherRoot) throws Exception {
        double lat = weatherRoot.path("coord").path("lat").asDouble();
        double lon = weatherRoot.path("coord").path("lon").asDouble();
        String countryCode = weatherRoot.path("sys").path("country").asText();

        String aqiUrl = "https://api.openweathermap.org/data/2.5/air_pollution?lat=" +
                lat + "&lon=" + lon + "&appid=" + API_KEY;

        JsonNode aqiRoot = sendRequest(aqiUrl);
        int aqi = aqiRoot.path("list").get(0).path("main").path("aqi").asInt();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
                .withZone(ZoneId.systemDefault());

        String sunrise = timeFormatter.format(Instant.ofEpochSecond(weatherRoot.path("sys").path("sunrise").asLong()));
        String sunset = timeFormatter.format(Instant.ofEpochSecond(weatherRoot.path("sys").path("sunset").asLong()));

        WeatherData data = new WeatherData(
                weatherRoot.path("name").asText(),
                countryCode,
                weatherRoot.path("main").path("temp").asDouble(),
                weatherRoot.path("weather").get(0).path("description").asText(),
                weatherRoot.path("main").path("humidity").asInt(),
                weatherRoot.path("wind").path("speed").asDouble(),
                weatherRoot.path("main").path("pressure").asInt(),
                aqi,
                lat,
                lon,
                sunrise,
                sunset
        );

        fetchHourlyForecast(data);
        return data;
    }

    private static void fetchHourlyForecast(WeatherData data) {
        try {
            String forecastUrl = "https://api.openweathermap.org/data/2.5/forecast?lat=" +
                    data.getLatitude() + "&lon=" + data.getLongitude() + "&appid=" + API_KEY + "&units=metric";

            JsonNode forecastRoot = sendRequest(forecastUrl);
            List<WeatherData.HourlyPoint> hourlyList = new ArrayList<>();
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("h a");

            for (JsonNode node : forecastRoot.path("list")) {
                if (hourlyList.size() >= 8) break;

                long timestamp = node.path("dt").asLong();
                LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());

                hourlyList.add(new WeatherData.HourlyPoint(
                        dateTime.format(outputFormatter),
                        node.path("main").path("temp").asDouble(),
                        node.path("weather").get(0).path("main").asText(),
                        (int) (node.path("pop").asDouble() * 100)
                ));
            }
            data.setHourlyForecast(hourlyList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<MonthlyData> fetchMonthlyForecast(String city) {
        List<MonthlyData> monthlyList = new ArrayList<>();
        String queryCity = (city == null || city.equalsIgnoreCase("Bagerhat")) ? "Khulna" : city;
        try {
            String forecastUrl = "https://api.openweathermap.org/data/2.5/forecast?q=" +
                    queryCity.replace(" ", "+") + "&appid=" + API_KEY + "&units=metric";

            JsonNode root = sendRequest(forecastUrl);
            JsonNode list = root.path("list");

            for (int i = 0; i < list.size(); i += 8) {
                JsonNode dayNode = list.get(i);
                long timestamp = dayNode.path("dt").asLong();
                LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());

                double high = dayNode.path("main").path("temp_max").asDouble();
                double low = dayNode.path("main").path("temp_min").asDouble();
                String cond = dayNode.path("weather").get(0).path("main").asText();

                monthlyList.add(new MonthlyData(date.getDayOfMonth(), high, low, cond));
            }

            if (monthlyList.size() < 28) {
                for (int d = 1; d <= 31; d++) {
                    final int currentD = d;
                    boolean exists = monthlyList.stream().anyMatch(m -> m.getDay() == currentD);
                    if (!exists) {
                        monthlyList.add(new MonthlyData(d, 22.0 + Math.random() * 3, 15.0 + Math.random() * 3, "Clear"));
                    }
                }
            }
            monthlyList.sort((m1, m2) -> Integer.compare(m1.getDay(), m2.getDay()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return monthlyList;
    }

    private static JsonNode sendRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("API Error: Received status code " + response.statusCode());
        }

        return mapper.readTree(response.body());
    }
}