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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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
        if (currentWeatherData == null) {
            handleRefresh();
        } else {
            refreshAllUIComponents();
        }
    }

    private void refreshAllUIComponents() {
        if (locationLabel != null) updateUI(currentWeatherData);
        if (calendarGrid != null) populateCalendar(LocalDate.now().getMonthValue());
        if (hourlyCardsContainer != null) populateHourlyUI();
        if (detailTimeHeader != null) populateDetailsUI();
    }

    @FXML
    public void handleRefresh() {
        Task<String> locationTask = new Task<>() {
            @Override
            protected String call() {
                String city = ApiService.getCityByIP();
                return (city == null || city.equalsIgnoreCase("Bagerhat")) ? "Khulna" : city;
            }
        };
        locationTask.setOnSucceeded(e -> performSearch(locationTask.getValue()));
        locationTask.setOnFailed(e -> performSearch("Khulna"));
        new Thread(locationTask).start();
    }

    @FXML
    public void handleSearch() {
        if (searchField == null) return;
        String city = searchField.getText().trim();
        if (!city.isEmpty()) performSearch(city);
    }

    private void performSearch(String city) {
        String finalCity = (city == null || city.equalsIgnoreCase("Bagerhat")) ? "Khulna" : city;
        Task<WeatherData> task = new Task<>() {
            @Override
            protected WeatherData call() throws Exception {
                return ApiService.fetchWeather(finalCity);
            }
        };
        task.setOnSucceeded(e -> {
            currentWeatherData = task.getValue();
            Platform.runLater(this::refreshAllUIComponents);
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
    public void showHourlyScene() { switchScene("hourly-view.fxml"); }

    @FXML
    public void showCurrentScene() { switchScene("hello-view.fxml"); }

    @FXML
    public void showDetailsScene() { switchScene("details-view.fxml"); }

    @FXML
    public void showMonthlyScene() { switchScene("monthly-view.fxml"); }

    private void switchScene(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Stage stage = findStage();
            if (stage != null) {
                stage.getScene().setRoot(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Stage findStage() {
        if (searchField != null && searchField.getScene() != null) return (Stage) searchField.getScene().getWindow();
        if (locationLabel != null && locationLabel.getScene() != null) return (Stage) locationLabel.getScene().getWindow();
        if (calendarGrid != null && calendarGrid.getScene() != null) return (Stage) calendarGrid.getScene().getWindow();
        if (hourlyCardsContainer != null && hourlyCardsContainer.getScene() != null) return (Stage) hourlyCardsContainer.getScene().getWindow();
        if (detailTimeHeader != null && detailTimeHeader.getScene() != null) return (Stage) detailTimeHeader.getScene().getWindow();
        return null;
    }

    private void populateHourlyUI() {
        if (hourlyCardsContainer == null || currentWeatherData == null) return;
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
        if (detailTimeHeader == null || currentWeatherData == null) return;
        detailTimeHeader.setText("Weather details " + LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm A")));
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
            if (currentWeatherData.getHourlyForecast() != null) {
                currentWeatherData.getHourlyForecast().stream().limit(7).forEach(p ->
                        series.getData().add(new XYChart.Data<>(p.time(), p.temp()))
                );
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
        try {
            int monthValue = java.time.Month.valueOf(clicked.getText().toUpperCase()).getValue();
            populateCalendar(monthValue);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void populateCalendar(int monthValue) {
        if (calendarGrid == null || currentWeatherData == null) return;
        calendarGrid.getChildren().clear();
        int year = LocalDate.now().getYear();
        LocalDate firstOfMonth = LocalDate.of(year, monthValue, 1);
        int dayOfWeekOffset = firstOfMonth.getDayOfWeek().getValue() % 7;
        int daysInMonth = YearMonth.of(year, monthValue).lengthOfMonth();

        Task<List<MonthlyData>> task = new Task<>() {
            @Override
            protected List<MonthlyData> call() {
                return ApiService.fetchMonthlyForecast(currentWeatherData.getCity());
            }
        };

        task.setOnSucceeded(e -> {
            List<MonthlyData> monthData = task.getValue();
            int dayCounter = 1;
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 7; col++) {
                    int slotIndex = (row * 7) + col;
                    if (slotIndex < dayOfWeekOffset || dayCounter > daysInMonth) {
                        VBox emptyBox = new VBox();
                        emptyBox.getStyleClass().add("calendar-day-card-empty");
                        calendarGrid.add(emptyBox, col, row);
                        continue;
                    }
                    VBox dayCard = new VBox(5);
                    dayCard.getStyleClass().add("calendar-day-card");
                    Label dayNum = new Label(String.valueOf(dayCounter));
                    dayNum.setStyle("-fx-text-fill: #5D5A88; -fx-font-size: 14;");

                    final int finalDay = dayCounter;
                    MonthlyData data = monthData.stream().filter(d -> d.getDay() == finalDay).findFirst().orElse(null);

                    if (data != null) {
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
                    } else {
                        dayCard.getChildren().add(dayNum);
                        dayCard.setOpacity(0.4);
                    }
                    if (dayCounter == LocalDate.now().getDayOfMonth() && monthValue == LocalDate.now().getMonthValue()) {
                        dayCard.getStyleClass().add("calendar-day-card-today");
                    }
                    calendarGrid.add(dayCard, col, row);
                    dayCounter++;
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