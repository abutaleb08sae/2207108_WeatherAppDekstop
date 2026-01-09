package com.example.weatherapp;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
    @FXML private ListView<String> historyListView;
    @FXML private Label tempLabel, locationLabel, conditionLabel, feelsLikeLabel;
    @FXML private Label windLabel, humidityLabel, pressureLabel, visibilityLabel;
    @FXML private Label aqiLabel, dewPointLabel, timeLabel, descriptionSentence, weatherIcon;
    @FXML private WebView mapView, interactiveMapView;
    @FXML private Circle aqiCircle;

    @FXML private HBox hourlyCardsContainer, monthBar;
    @FXML private LineChart<String, Number> tempChart;
    @FXML private Label highTempLabel, lowTempLabel;

    @FXML private Label detailTimeHeader, detTemp, detWind, detHum, detDew, detAqi, detUv, detMoon, detailVisibility, detailPressure;
    @FXML private LineChart<String, Number> miniTempChart;
    @FXML private Polygon compassNeedle;
    @FXML private Arc uvArc;
    @FXML private GridPane calendarGrid;

    @FXML private LineChart<String, Number> trendsChart;

    private static WeatherData currentWeatherData;

    @FXML
    public void initialize() {
        setupHistoryUI();
        if (currentWeatherData == null) {
            handleRefresh();
        } else {
            refreshAllUIComponents();
        }
        if (interactiveMapView != null) {
            loadInteractiveMap();
        }
    }

    private void setupHistoryUI() {
        if (historyListView == null) return;

        historyListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String city, boolean empty) {
                super.updateItem(city, empty);
                if (empty || city == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox container = new HBox();
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.setSpacing(10);

                    Label nameLabel = new Label(city);
                    nameLabel.setStyle("-fx-text-fill: white;");
                    nameLabel.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(nameLabel, Priority.ALWAYS);

                    Label deleteBtn = new Label("✕");
                    deleteBtn.getStyleClass().add("delete-icon");
                    deleteBtn.setOnMouseClicked(e -> {
                        DatabaseManager.deleteSearch(city);
                        updateHistoryList();
                        e.consume();
                    });

                    container.getChildren().addAll(nameLabel, deleteBtn);
                    setGraphic(container);

                    this.setOnMouseClicked(e -> {
                        if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY && !deleteBtn.isHover()) {
                            searchField.setText(city);
                            handleSearch();
                            hideHistory();
                        }
                    });
                }
            }
        });

        searchField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                updateHistoryList();
            } else {
                Platform.runLater(() -> {
                    if (!historyListView.isFocused()) {
                        hideHistory();
                    }
                });
            }
        });

        historyListView.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && !searchField.isFocused()) {
                hideHistory();
            }
        });
    }

    @FXML
    public void showHistory() {
        updateHistoryList();
    }

    private void updateHistoryList() {
        if (historyListView == null) return;
        List<String> history = DatabaseManager.getHistory();
        if (!history.isEmpty()) {
            historyListView.getItems().setAll(history);
            historyListView.setVisible(true);
            historyListView.setManaged(true);
            historyListView.toFront();
        } else {
            hideHistory();
        }
    }

    private void hideHistory() {
        if (historyListView != null) {
            historyListView.setVisible(false);
            historyListView.setManaged(false);
        }
    }

    private void refreshAllUIComponents() {
        if (locationLabel != null) updateUI(currentWeatherData);
        if (calendarGrid != null) populateCalendar(LocalDate.now().getMonthValue());
        if (hourlyCardsContainer != null) populateHourlyUI();
        if (detailTimeHeader != null) populateDetailsUI();
        if (trendsChart != null) populateTrendsUI();
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
        if (!city.isEmpty()) {
            DatabaseManager.saveSearch(city);
            performSearch(city);
            hideHistory();
        }
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
        if (data == null) return;
        String fullCountryName = new Locale("", data.getCountryCode()).getDisplayCountry();
        if (locationLabel != null) locationLabel.setText(data.getCity() + ", " + fullCountryName);
        if (timeLabel != null) timeLabel.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")));
        if (tempLabel != null) tempLabel.setText(Math.round(data.getTemp()) + "°C");
        if (conditionLabel != null) conditionLabel.setText(data.getDescription());
        if (feelsLikeLabel != null) feelsLikeLabel.setText("Feels like " + Math.round(data.getTemp()) + "°C");
        if (descriptionSentence != null) descriptionSentence.setText("The skies will be " + data.getDescription().toLowerCase() + ".");
        if (weatherIcon != null) weatherIcon.setText(getEmojiForCondition(data.getDescription().toLowerCase()));
        if (windLabel != null) windLabel.setText(data.getWindSpeed() + " m/s");
        if (humidityLabel != null) humidityLabel.setText(data.getHumidity() + "%");
        if (pressureLabel != null) pressureLabel.setText(data.getPressure() + " hPa");
        if (aqiLabel != null) aqiLabel.setText(data.getAqiText());
        updateAqiCircle(data.getAqi());

        double dp = data.getTemp() - ((100 - data.getHumidity()) / 5.0);
        if (dewPointLabel != null) dewPointLabel.setText(Math.round(dp) + "°C");

        if (mapView != null) {
            String mapUrl = "https://www.openstreetmap.org/export/embed.html?bbox=" +
                    (data.getLongitude() - 0.1) + "," + (data.getLatitude() - 0.1) + "," +
                    (data.getLongitude() + 0.1) + "," + (data.getLatitude() + 0.1) +
                    "&layer=mapnik&marker=" + data.getLatitude() + "," + data.getLongitude();
            mapView.getEngine().load(mapUrl);
        }
    }

    @FXML public void showHourlyScene(ActionEvent event) { switchScene(event, "hourly-view.fxml"); }
    @FXML public void showCurrentScene(ActionEvent event) { switchScene(event, "hello-view.fxml"); }
    @FXML public void showDetailsScene(ActionEvent event) { switchScene(event, "details-view.fxml"); }
    @FXML public void showMonthlyScene(ActionEvent event) { switchScene(event, "monthly-view.fxml"); }
    @FXML public void showTrendsScene(ActionEvent event) { switchScene(event, "trends-view.fxml"); }
    @FXML public void showMapsScene(ActionEvent event) { switchScene(event, "maps-view.fxml"); }

    private void switchScene(ActionEvent event, String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadInteractiveMap() {
        if (interactiveMapView == null || currentWeatherData == null) return;
        String script = "<html><head><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><style>body { margin: 0; } #map { height: 100vh; width: 100vw; }</style></head><body><div id='map'></div><script>var map = L.map('map').setView([" + currentWeatherData.getLatitude() + ", " + currentWeatherData.getLongitude() + "], 8);L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);var marker = L.marker([" + currentWeatherData.getLatitude() + ", " + currentWeatherData.getLongitude() + "]).addTo(map);map.on('click', function(e) { window.location.href = 'app://click?lat=' + e.latlng.lat + '&lon=' + e.latlng.lng; });</script></body></html>";
        interactiveMapView.getEngine().loadContent(script);
        interactiveMapView.getEngine().locationProperty().addListener((obs, oldLoc, newLoc) -> {
            if (newLoc != null && newLoc.startsWith("app://click")) {
                String query = newLoc.split("\\?")[1];
                double lat = Double.parseDouble(query.split("&")[0].split("=")[1]);
                double lon = Double.parseDouble(query.split("&")[1].split("=")[1]);
                fetchWeatherByCoords(lat, lon);
            }
        });
    }

    private void fetchWeatherByCoords(double lat, double lon) {
        Task<WeatherData> task = new Task<>() {
            @Override
            protected WeatherData call() throws Exception {
                return ApiService.fetchWeatherByCoords(lat, lon);
            }
        };
        task.setOnSucceeded(e -> {
            currentWeatherData = task.getValue();
            DatabaseManager.saveSearch(currentWeatherData.getCity());
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
                    Parent root = loader.load();
                    Stage stage = (Stage) interactiveMapView.getScene().getWindow();
                    stage.getScene().setRoot(root);
                } catch (Exception ex) { ex.printStackTrace(); }
            });
        });
        new Thread(task).start();
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
                currentWeatherData.getHourlyForecast().stream().limit(7).forEach(p -> series.getData().add(new XYChart.Data<>(p.time(), p.temp())));
            }
            miniTempChart.getData().add(series);
        }
        if (compassNeedle != null) compassNeedle.setRotate(45);
    }

    private void populateTrendsUI() {
        if (trendsChart == null || currentWeatherData == null) return;
        trendsChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Temperature Trend");
        double max = -100, min = 100;
        if (currentWeatherData.getHourlyForecast() != null) {
            for (WeatherData.HourlyPoint point : currentWeatherData.getHourlyForecast()) {
                series.getData().add(new XYChart.Data<>(point.time(), point.temp()));
                if (point.temp() > max) max = point.temp();
                if (point.temp() < min) min = point.temp();
            }
        }
        trendsChart.getData().add(series);
        if (highTempLabel != null) highTempLabel.setText(Math.round(max) + "°C");
        if (lowTempLabel != null) lowTempLabel.setText(Math.round(min) + "°C");
    }

    @FXML
    public void handleMonthClick(ActionEvent event) {
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
            @Override protected List<MonthlyData> call() { return ApiService.fetchMonthlyForecast(currentWeatherData.getCity()); }
        };
        task.setOnSucceeded(e -> {
            List<MonthlyData> monthData = task.getValue();
            int dayCounter = 1;
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 7; col++) {
                    int slotIndex = (row * 7) + col;
                    if (slotIndex < dayOfWeekOffset || dayCounter > daysInMonth) {
                        VBox emptyBox = new VBox();
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