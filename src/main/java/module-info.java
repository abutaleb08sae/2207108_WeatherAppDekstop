module com.example.weatherapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    opens com.example.weatherapp to javafx.fxml;
    exports com.example.weatherapp;
}