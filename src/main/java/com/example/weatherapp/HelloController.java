package com.example.weatherapp;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

public class HelloController {
    @FXML private TextField searchField;
    @FXML private Label tempLabel, locationLabel, conditionLabel, feelsLikeLabel;
    @FXML private Label windLabel, humidityLabel, pressureLabel, visibilityLabel;
    @FXML private Label aqiLabel, dewPointLabel, timeLabel, descriptionSentence, weatherIcon;
    @FXML private WebView mapView;
    @FXML private Circle aqiCircle;

    @FXML private HBox hourlyCardsContainer;
    @FXML private LineChart<String, Number> tempChart;
    @FXML private Label highTempLabel, lowTempLabel;

    private static WeatherData currentWeatherData;

    @FXML
    public void initialize() {
        if (locationLabel != null) {
            handleRefresh();
        } else if (hourlyCardsContainer != null && currentWeatherData != null) {
            populateHourlyUI();
        }
    }

    @FXML
    public void handleRefresh() {
        Task<String> locationTask = new Task<>() {
            @Override
            protected String call() {
                return ApiService.getCityByIP();
            }
        };
        locationTask.setOnSucceeded(e -> {
            String city = locationTask.getValue();
            if (city != null && !city.isEmpty()) {
                if (searchField != null) searchField.setText(city);
                performSearch(city);
            } else {
                showLocationPrompt();
            }
        });
        locationTask.setOnFailed(e -> showLocationPrompt());
        new Thread(locationTask).start();
    }

    private void showLocationPrompt() {
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Location Needed");
            dialog.setHeaderText("Auto-detection failed.");
            dialog.setContentText("Please enter a city name:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(this::performSearch);
        });
    }

    @FXML
    public void handleSearch() {
        String city = searchField.getText().trim();
        if (!city.isEmpty()) performSearch(city);
    }

    private void performSearch(String city) {
        Task<WeatherData> task = new Task<>() {
            @Override
            protected WeatherData call() throws Exception {
                return ApiService.fetchWeather(city);
            }
        };
        task.setOnSucceeded(e -> {
            currentWeatherData = task.getValue();
            if (locationLabel != null) updateUI(currentWeatherData);
        });
        new Thread(task).start();
    }

    private void updateUI(WeatherData data) {
        String fullCountryName = new Locale("", data.getCountryCode()).getDisplayCountry();
        locationLabel.setText(data.getCity() + ", " + fullCountryName);
        timeLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")));
        tempLabel.setText(Math.round(data.getTemp()) + "°C");
        conditionLabel.setText(data.getDescription());
        feelsLikeLabel.setText("Feels like " + Math.round(data.getTemp()) + "°C");
        descriptionSentence.setText("The skies will be " + data.getDescription().toLowerCase() + ".");
        weatherIcon.setText(getEmojiForCondition(data.getDescription().toLowerCase()));
        windLabel.setText(data.getWindSpeed() + " m/s");
        humidityLabel.setText(data.getHumidity() + "%");
        pressureLabel.setText(data.getPressure() + " hPa");
        aqiLabel.setText(data.getAqiText());
        updateAqiCircle(data.getAqi());

        double dp = data.getTemp() - ((100 - data.getHumidity()) / 5.0);
        dewPointLabel.setText(Math.round(dp) + "°C");

        String mapUrl = "https://www.openstreetmap.org/export/embed.html?bbox=" +
                (data.getLongitude() - 0.1) + "," + (data.getLatitude() - 0.1) + "," +
                (data.getLongitude() + 0.1) + "," + (data.getLatitude() + 0.1) +
                "&layer=mapnik&marker=" + data.getLatitude() + "," + data.getLongitude();
        mapView.getEngine().load(mapUrl);
    }

    @FXML
    public void showHourlyScene() {
        if (currentWeatherData == null) return;
        switchScene("hourly-view.fxml");
    }

    @FXML
    public void showCurrentScene() {
        switchScene("hello-view.fxml");
    }

    private void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Stage stage = (Stage) (searchField != null ? searchField.getScene() : hourlyCardsContainer.getScene()).getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populateHourlyUI() {
        hourlyCardsContainer.getChildren().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        double max = -100, min = 100;

        for (WeatherData.HourlyPoint point : currentWeatherData.getHourlyForecast()) {
            VBox card = new VBox(8);
            card.setAlignment(Pos.CENTER);
            card.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 15; -fx-padding: 15; -fx-min-width: 100;");

            Label time = new Label(point.time());
            time.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            Label icon = new Label(getEmojiForCondition(point.condition().toLowerCase()));
            icon.setStyle("-fx-font-size: 30;");
            Label temp = new Label(Math.round(point.temp()) + "°");
            temp.setStyle("-fx-text-fill: white; -fx-font-size: 18;");

            card.getChildren().addAll(time, icon, temp);
            hourlyCardsContainer.getChildren().add(card);

            series.getData().add(new XYChart.Data<>(point.time(), point.temp()));
            if (point.temp() > max) max = point.temp();
            if (point.temp() < min) min = point.temp();
        }

        tempChart.getData().add(series);
        highTempLabel.setText(Math.round(max) + "°C");
        lowTempLabel.setText(Math.round(min) + "°C");
    }

    private String getEmojiForCondition(String cond) {
        if (cond.contains("rain")) return "🌧️";
        if (cond.contains("cloud")) return "☁️";
        if (cond.contains("clear") || cond.contains("sun")) return "☀️";
        if (cond.contains("snow")) return "❄️";
        return "🌡️";
    }

    private void updateAqiCircle(int aqi) {
        if (aqiCircle == null) return;
        switch (aqi) {
            case 1 -> aqiCircle.setFill(Color.LIGHTGREEN);
            case 2 -> aqiCircle.setFill(Color.YELLOW);
            case 3 -> aqiCircle.setFill(Color.ORANGE);
            case 4 -> aqiCircle.setFill(Color.RED);
            case 5 -> aqiCircle.setFill(Color.PURPLE);
            default -> aqiCircle.setFill(Color.GRAY);
        }
    }
}