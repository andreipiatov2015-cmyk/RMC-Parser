package com.rmc;

import com.rmc.i18n.Messages;
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
        logger.info("Инициализация главного окна");

        try {
            primaryStage.setTitle(Messages.APP_TITLE);
            primaryStage.setResizable(false);

            VBox root = new VBox();
            root.setSpacing(20);
            root.setStyle("-fx-alignment: center; -fx-padding: 40; -fx-background-color: #f5f5f5;");

            Label logoLabel = new Label(Messages.APP_TITLE);
            logoLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
            logoLabel.setStyle("-fx-text-fill: #333333;");

            Label versionLabel = new Label(Messages.VERSION_PREFIX + " " + VersionService.getCurrentVersionString());
            versionLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
            versionLabel.setStyle("-fx-text-fill: #666666;");

            Button checkUpdatesButton = new Button(Messages.BTN_CHECK_UPDATES);
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
                logger.info("Нажата кнопка 'Проверить обновления'");
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

            // Кнопка диагностики разработчика
            Button developerButton = new Button(Messages.BTN_DEVELOPER_DIAGNOSTICS);
            developerButton.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
            developerButton.setPrefWidth(200);
            developerButton.setPrefHeight(30);
            developerButton.setStyle(
                "-fx-background-color: #2196F3; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand;"
            );

            developerButton.setOnAction(event -> {
                logger.info(Messages.LOG_DEV_WINDOW_OPEN);
                if (developerWindow == null) {
                    developerWindow = new DeveloperWindow();
                }
                developerWindow.show();
            });

            root.getChildren().addAll(logoLabel, versionLabel, checkUpdatesButton, developerButton);

            Scene scene = new Scene(root, 350, 300);
            primaryStage.setScene(scene);
            primaryStage.show();

            // Показать информационный диалог при первом запуске
            showInfoDialog();

            logger.info("Главное окно успешно отображено");

        } catch (Exception e) {
            logger.error("Не удалось запустить приложение", e);
        }
    }

    private void showInfoDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(Messages.DIALOG_INFO_TITLE);
        alert.setHeaderText(Messages.DIALOG_INFO_HEADER);
        alert.setContentText(Messages.DIALOG_INFO_CONTENT);
        alert.getButtonTypes().setAll(new ButtonType("OK"));
        alert.show();
    }

    @Override
    public void stop() {
        logger.info("Приложение закрывается");
        if (developerWindow != null) {
            developerWindow.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}