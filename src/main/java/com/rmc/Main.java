package com.rmc;

import com.rmc.app.ApplicationLifecycle;
import com.rmc.logging.AppLogger;
import com.rmc.ui.DeveloperWindow;
import com.rmc.version.VersionService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.slf4j.Logger;

public class Main extends Application {

    private static final Logger logger = AppLogger.getLogger();
    private DeveloperWindow developerWindow;

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

            Label versionLabel = new Label("Version " + VersionService.getCurrentVersionString());
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

            // Developer Diagnostics Button
            Button developerButton = new Button("Developer Diagnostics");
            developerButton.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
            developerButton.setPrefWidth(180);
            developerButton.setPrefHeight(30);
            developerButton.setStyle(
                "-fx-background-color: #2196F3; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand;"
            );

            developerButton.setOnAction(event -> {
                logger.info("Opening Developer Diagnostics Window");
                if (developerWindow == null) {
                    developerWindow = new DeveloperWindow();
                }
                developerWindow.show();
            });

            root.getChildren().addAll(logoLabel, versionLabel, checkUpdatesButton, developerButton);

            Scene scene = new Scene(root, 350, 300);
            primaryStage.setScene(scene);
            primaryStage.show();

            // Show info dialog on first run
            showInfoDialog();

            logger.info("Main window displayed successfully");

        } catch (Exception e) {
            logger.error("Failed to start application", e);
        }
    }

    private void showInfoDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("RMC Framework");
        alert.setHeaderText("Developer Diagnostics Available");
        alert.setContentText("Click 'Developer Diagnostics' to access:\n" +
                "• Live Log Console\n" +
                "• System Status Panel\n" +
                "• Driver Management Tools\n" +
                "• Diagnostic Reports");
        alert.getButtonTypes().setAll(new ButtonType("OK"));
        alert.show();
    }

    @Override
    public void stop() {
        logger.info("Application closing");
        if (developerWindow != null) {
            developerWindow.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}