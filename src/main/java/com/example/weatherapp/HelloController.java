package com.example.weatherapp;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class HelloController {
    @FXML private TextField searchField;
    @FXML private Label tempLabel, locationLabel, conditionLabel, feelsLikeLabel;
    @FXML private Label windLabel, humidityLabel, pressureLabel, visibilityLabel;
    @FXML private Label aqiLabel, dewPointLabel, sunriseLabel, sunsetLabel, timeLabel;
    @FXML private Label descriptionSentence, weatherIcon;
    @FXML private WebView mapView;

    @FXML
    public void handleSearch() {
        String city = searchField.getText().trim();
        if (city.isEmpty()) return;

        Task<WeatherData> task = new Task<>() {
            @Override
            protected WeatherData call() throws Exception {
                return ApiService.fetchWeather(city);
            }
        };

        task.setOnSucceeded(e -> {
            WeatherData data = task.getValue();
            updateUI(data);
        });

        task.setOnFailed(e -> {
            locationLabel.setText("City not found or API error");
            tempLabel.setText("--");
        });

        new Thread(task).start();
    }

    private void updateUI(WeatherData data) {
        locationLabel.setText(data.getCity() + ", Bangladesh");
        timeLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")));

        tempLabel.setText(Math.round(data.getTemp()) + "°C");
        conditionLabel.setText(data.getDescription());
        feelsLikeLabel.setText("Feels like " + Math.round(data.getTemp()) + "°C");

        descriptionSentence.setText("The skies will be " + data.getDescription().toLowerCase() + ".");

        windLabel.setText(data.getWindSpeed() + " m/s");
        humidityLabel.setText(data.getHumidity() + "%");
        pressureLabel.setText(data.getPressure() + " hPa");
        visibilityLabel.setText("10 km");

        aqiLabel.setText(data.getAqiText());

        double dp = data.getTemp() - ((100 - data.getHumidity()) / 5.0);
        dewPointLabel.setText(Math.round(dp) + "°C");

        String mapUrl = "https://www.openstreetmap.org/export/embed.html?bbox=" +
                (data.getLongitude() - 0.1) + "," + (data.getLatitude() - 0.1) + "," +
                (data.getLongitude() + 0.1) + "," + (data.getLatitude() + 0.1) +
                "&layer=mapnik&marker=" + data.getLatitude() + "," + data.getLongitude();

        mapView.getEngine().load(mapUrl);
    }
}