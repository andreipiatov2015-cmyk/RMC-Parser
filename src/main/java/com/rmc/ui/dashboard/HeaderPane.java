package com.rmc.ui.dashboard;

import com.rmc.i18n.Messages;
import com.rmc.update.UpdateService;
import com.rmc.version.VersionService;
import com.rmc.ui.DeveloperWindow;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Header pane with app title, version, and action buttons.
 */
public class HeaderPane extends HBox {
    
    private static final DeveloperWindow developerWindow = new DeveloperWindow();
    
    public HeaderPane() {
        getStyleClass().add("header-pane");
        setPadding(new Insets(12, 20, 12, 20));
        setAlignment(Pos.CENTER_LEFT);
        
        // App title and version
        Label appTitle = new Label("RMC Framework");
        appTitle.getStyleClass().add("app-title");
        
        Label versionLabel = new Label("v" + VersionService.getCurrentVersionString());
        versionLabel.getStyleClass().add("version-label");
        
        VBox titleBox = new VBox();
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.getChildren().addAll(appTitle, versionLabel);
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Action buttons
        Button updateBtn = createHeaderButton("Проверить обновления", e -> handleCheckUpdates());
        Button settingsBtn = createHeaderButton("Настройки", e -> handleSettings());
        Button diagnosticsBtn = createHeaderButton("Диагностика", e -> handleDiagnostics());
        
        // User icon (placeholder)
        Label userIcon = new Label("👤");
        userIcon.setStyle("-fx-font-size: 20px;");
        
        getChildren().addAll(titleBox, spacer, updateBtn, settingsBtn, diagnosticsBtn, userIcon);
    }
    
    private Button createHeaderButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button button = new Button(text);
        button.getStyleClass().add("header-button");
        button.setOnAction(handler);
        return button;
    }
    
    private void handleCheckUpdates() {
        Thread updateThread = new Thread(() -> {
            try {
                UpdateService updateService = new UpdateService();
                updateService.checkForUpdates();
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Ошибка");
                    alert.setHeaderText("Ошибка проверки обновлений");
                    alert.setContentText(e.getMessage());
                    alert.show();
                });
            }
        });
        updateThread.start();
    }
    
    private void handleSettings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Настройки");
        alert.setHeaderText("Настройки приложения");
        alert.setContentText("Настройки будут доступны в следующих версиях");
        alert.show();
    }
    
    private void handleDiagnostics() {
        developerWindow.show();
    }
}
