package com.rmc;

import com.rmc.logging.AppLogger;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.slf4j.Logger;

public class Main extends Application {

    private static final Logger logger = AppLogger.getLogger();

    @Override
    public void start(Stage primaryStage) {
        AppLogger.logStartupInfo();
        logger.info("Initializing main window");

        try {
            primaryStage.setTitle("RMC Framework");
            primaryStage.setResizable(false);

            VBox root = new VBox();
            root.setSpacing(20);
            root.setStyle("-fx-alignment: center; -fx-padding: 40; -fx-background-color: #f5f5f5;");

            Label logoLabel = new Label("RMC Framework");
            logoLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
            logoLabel.setStyle("-fx-text-fill: #333333;");

            Label versionLabel = new Label("Version 0.1.0");
            versionLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
            versionLabel.setStyle("-fx-text-fill: #666666;");

            Button checkUpdatesButton = new Button("Check for Updates");
            checkUpdatesButton.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
            checkUpdatesButton.setPrefWidth(180);
            checkUpdatesButton.setPrefHeight(40);
            checkUpdatesButton.setStyle(
                "-fx-background-color: #4CAF50; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand;"
            );

            checkUpdatesButton.setOnAction(event -> {
                logger.info("Check for Updates button clicked");
                checkUpdatesButton.setDisable(true);

                new Thread(() -> {
                    try {
                        com.rmc.update.UpdateService updateService = new com.rmc.update.UpdateService();
                        updateService.checkForUpdates();
                    } finally {
                        javafx.application.Platform.runLater(() -> checkUpdatesButton.setDisable(false));
                    }
                }).start();
            });

            root.getChildren().addAll(logoLabel, versionLabel, checkUpdatesButton);

            Scene scene = new Scene(root, 350, 250);
            primaryStage.setScene(scene);
            primaryStage.show();

            logger.info("Main window displayed successfully");

        } catch (Exception e) {
            logger.error("Failed to start application", e);
        }
    }

    @Override
    public void stop() {
        logger.info("Application closing");
    }

    public static void main(String[] args) {
        launch(args);
    }
}