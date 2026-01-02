package com.example.weatherapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ApiService {
    private static final String API_KEY = "5828bd5b646348de10e5a6be2b917c31";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient client = HttpClient.newHttpClient();

    public static WeatherData fetchWeather(String city) throws Exception {
        String weatherUrl = "https://api.openweathermap.org/data/2.5/weather?q=" +
                city.replace(" ", "+") + "&appid=" + API_KEY + "&units=metric";

        JsonNode weatherRoot = sendRequest(weatherUrl);

        double lat = weatherRoot.path("coord").path("lat").asDouble();
        double lon = weatherRoot.path("coord").path("lon").asDouble();
        String countryCode = weatherRoot.path("sys").path("country").asText();

        String aqiUrl = "http://api.openweathermap.org/data/2.5/air_pollution?lat=" +
                lat + "&lon=" + lon + "&appid=" + API_KEY;

        JsonNode aqiRoot = sendRequest(aqiUrl);
        int aqi = aqiRoot.path("list").get(0).path("main").path("aqi").asInt();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
                .withZone(ZoneId.systemDefault());

        String sunrise = timeFormatter.format(Instant.ofEpochSecond(weatherRoot.path("sys").path("sunrise").asLong()));
        String sunset = timeFormatter.format(Instant.ofEpochSecond(weatherRoot.path("sys").path("sunset").asLong()));

        return new WeatherData(
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