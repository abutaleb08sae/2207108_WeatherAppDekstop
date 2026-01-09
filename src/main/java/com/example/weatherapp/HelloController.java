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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class HelloController {
    @FXML private TextField searchField;
    @FXML private Label tempLabel, locationLabel, conditionLabel, feelsLikeLabel;
    @FXML private Label windLabel, humidityLabel, pressureLabel, visibilityLabel;
    @FXML private Label aqiLabel, dewPointLabel, timeLabel, descriptionSentence, weatherIcon;
    @FXML private WebView mapView;
    @FXML private Circle aqiCircle;

    @FXML private HBox hourlyCardsContainer, monthBar;
    @FXML private LineChart<String, Number> tempChart;
    @FXML private Label highTempLabel, lowTempLabel;

    @FXML private Label detailTimeHeader, detTemp, detWind, detHum, detDew, detAqi, detUv, detMoon, detailVisibility, detailPressure;
    @FXML private LineChart<String, Number> miniTempChart;
    @FXML private Polygon compassNeedle;
    @FXML private Arc uvArc;
    @FXML private GridPane calendarGrid;

    private static WeatherData currentWeatherData;

    @FXML
    public void initialize() {
        if (calendarGrid != null) {
            if (currentWeatherData != null) {
                populateCalendar(1);
            }
        } else if (locationLabel != null) {
            handleRefresh();
        } else if (hourlyCardsContainer != null && currentWeatherData != null) {
            populateHourlyUI();
        } else if (detailTimeHeader != null && currentWeatherData != null) {
            populateDetailsUI();
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
        if (searchField == null) return;
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

    @FXML
    public void showDetailsScene() {
        if (currentWeatherData == null) return;
        switchScene("details-view.fxml");
    }

    @FXML
    public void showMonthlyScene() {
        if (currentWeatherData == null) {
            handleRefresh();
        }
        switchScene("monthly-view.fxml");
    }

    private void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Stage stage = findStage();
            if (stage != null) {
                stage.getScene().setRoot(root);
            }
        } catch (Exception e) {
            System.err.println("Error loading scene: " + fxmlFile);
            e.printStackTrace();
        }
    }

    private Stage findStage() {
        if (calendarGrid != null && calendarGrid.getScene() != null) return (Stage) calendarGrid.getScene().getWindow();
        if (searchField != null && searchField.getScene() != null) return (Stage) searchField.getScene().getWindow();
        if (hourlyCardsContainer != null && hourlyCardsContainer.getScene() != null) return (Stage) hourlyCardsContainer.getScene().getWindow();
        if (detailTimeHeader != null && detailTimeHeader.getScene() != null) return (Stage) detailTimeHeader.getScene().getWindow();
        if (locationLabel != null && locationLabel.getScene() != null) return (Stage) locationLabel.getScene().getWindow();
        return null;
    }

    private void populateHourlyUI() {
        if (hourlyCardsContainer == null) return;
        hourlyCardsContainer.getChildren().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        double max = -100, min = 100;
        if (currentWeatherData.getHourlyForecast() != null) {
            for (WeatherData.HourlyPoint point : currentWeatherData.getHourlyForecast()) {
                VBox card = new VBox(8);
                card.setAlignment(Pos.CENTER);
                card.getStyleClass().add("mini-forecast-card");
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
        }
        if (tempChart != null) {
            tempChart.getData().clear();
            tempChart.getData().add(series);
        }
        if (highTempLabel != null) highTempLabel.setText(Math.round(max) + "°C");
        if (lowTempLabel != null) lowTempLabel.setText(Math.round(min) + "°C");
    }

    private void populateDetailsUI() {
        if (detailTimeHeader != null) detailTimeHeader.setText("Weather details " + LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm A")));
        if (detTemp != null) detTemp.setText(Math.round(currentWeatherData.getTemp()) + "°");
        if (detWind != null) detWind.setText(currentWeatherData.getWindSpeed() + " mph");
        if (detHum != null) detHum.setText(currentWeatherData.getHumidity() + "%");
        if (detAqi != null) detAqi.setText(String.valueOf(currentWeatherData.getAqi()));
        if (detUv != null) detUv.setText(String.valueOf(currentWeatherData.getUvIndex()));
        if (detMoon != null) detMoon.setText(currentWeatherData.getMoonPhase());
        if (detailVisibility != null) detailVisibility.setText("10 km");
        if (detailPressure != null) detailPressure.setText(currentWeatherData.getPressure() + " hPa");
        if (feelsLikeLabel != null) feelsLikeLabel.setText(Math.round(currentWeatherData.getTemp()) + "°");
        double dp = currentWeatherData.getTemp() - ((100 - currentWeatherData.getHumidity()) / 5.0);
        if (detDew != null) detDew.setText(Math.round(dp) + "° Dew point");
        if (miniTempChart != null) {
            miniTempChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            int count = 0;
            if (currentWeatherData.getHourlyForecast() != null) {
                for (WeatherData.HourlyPoint p : currentWeatherData.getHourlyForecast()) {
                    if (count++ > 6) break;
                    series.getData().add(new XYChart.Data<>(p.time(), p.temp()));
                }
            }
            miniTempChart.getData().add(series);
        }
        if (compassNeedle != null) compassNeedle.setRotate(45);
    }

    @FXML
    public void handleMonthClick(javafx.event.ActionEvent event) {
        if (monthBar == null) return;
        Button clicked = (Button) event.getSource();
        monthBar.getChildren().forEach(node -> node.getStyleClass().setAll("month-tab"));
        clicked.getStyleClass().setAll("month-tab-active");
        populateCalendar(1);
    }

    private void populateCalendar(int monthIndex) {
        if (calendarGrid == null || currentWeatherData == null) return;
        calendarGrid.getChildren().clear();

        Task<List<MonthlyData>> task = new Task<>() {
            @Override
            protected List<MonthlyData> call() {
                return ApiService.fetchMonthlyForecast(currentWeatherData.getCity());
            }
        };

        task.setOnSucceeded(e -> {
            List<MonthlyData> monthData = task.getValue();
            int dayIndex = 0;
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 7; col++) {
                    if (dayIndex >= monthData.size()) break;
                    MonthlyData data = monthData.get(dayIndex);
                    VBox dayCard = new VBox(5);
                    dayCard.getStyleClass().add("calendar-day-card");
                    Label dayNum = new Label(String.valueOf(data.getDay()));
                    dayNum.setStyle("-fx-text-fill: #5D5A88; -fx-font-size: 14;");
                    HBox content = new HBox(10);
                    content.setAlignment(Pos.CENTER_LEFT);
                    Label icon = new Label(getEmojiForCondition(data.getCondition().toLowerCase()));
                    icon.setStyle("-fx-font-size: 22;");
                    VBox temps = new VBox(0);
                    Label high = new Label(Math.round(data.getHigh()) + "°");
                    high.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
                    Label low = new Label(Math.round(data.getLow()) + "°");
                    low.setStyle("-fx-text-fill: #A09EBC;");
                    temps.getChildren().addAll(high, low);
                    content.getChildren().addAll(icon, temps);
                    dayCard.getChildren().addAll(dayNum, content);
                    if (data.getDay() == 9) dayCard.getStyleClass().add("calendar-day-card-today");
                    calendarGrid.add(dayCard, col, row);
                    dayIndex++;
                }
            }
        });
        new Thread(task).start();
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