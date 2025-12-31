module com.example.weatherapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;

    opens com.example.weatherapp to javafx.fxml;
    exports com.example.weatherapp;
}