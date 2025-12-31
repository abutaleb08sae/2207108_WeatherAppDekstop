package com.example.weatherapp;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebView;

public class HelloController {
    @FXML private TextField searchField;
    @FXML private Label tempLabel, locationLabel, conditionLabel, feelsLikeLabel;
    @FXML private Label windLabel, humidityLabel, pressureLabel, visibilityLabel;
    @FXML private WebView mapView;

    @FXML
    public void handleSearch() {
        String city = searchField.getText().trim();
        if (city.isEmpty()) return;

        // 1. Update UI Text
        locationLabel.setText(city.toUpperCase());
        tempLabel.setText("68°F"); // Placeholder
        conditionLabel.setText("Mostly Sunny");
        feelsLikeLabel.setText("Feels like: 70°F");

        windLabel.setText("5 mph");
        humidityLabel.setText("62%");
        visibilityLabel.setText("10 km");
        pressureLabel.setText("1015 hPa");

        // 2. Load the Map
        String url = "https://www.google.com/maps/search/" + city.replace(" ", "+");
        mapView.getEngine().load(url);
    }
}