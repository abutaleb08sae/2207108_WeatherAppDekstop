package com.example.weatherapp;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController {

    @FXML
    private Label tempLabel; // This matches the fx:id in FXML

    // This is the method IntelliJ is complaining about!
    @FXML
    protected void onButtonClick() {
        System.out.println("Refresh button clicked!");
        tempLabel.setText("Updating...");

        // This is where you will later call your API/Database logic
    }
}