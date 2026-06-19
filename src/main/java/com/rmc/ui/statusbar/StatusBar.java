package com.rmc.ui.statusbar;

import com.rmc.version.VersionService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Status bar - always visible footer.
 */
public class StatusBar extends HBox {
    
    private final Label connectionStatus;
    private final Label versionLabel;
    private final Label threadCount;
    private final Label requestCount;
    private final Label clockLabel;
    
    private int totalRequests = 0;
    
    public StatusBar() {
        getStyleClass().add("status-bar");
        setPadding(new Insets(0, 16, 0, 16));
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(24);
        
        // Connection status
        connectionStatus = new Label("○ Офлайн");
        connectionStatus.getStyleClass().add("status-item");
        
        // Separator
        Label sep1 = createSeparator();
        
        // Version
        versionLabel = new Label("v" + VersionService.getCurrentVersionString());
        versionLabel.getStyleClass().add("status-item");
        
        // Separator
        Label sep2 = createSeparator();
        
        // Thread count
        threadCount = new Label("Потоков: 0");
        threadCount.getStyleClass().add("status-item");
        
        // Separator
        Label sep3 = createSeparator();
        
        // Request count
        requestCount = new Label("Запросов: 0");
        requestCount.getStyleClass().add("status-item");
        
        // Spacer
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        // Clock
        clockLabel = new Label();
        clockLabel.getStyleClass().add("status-item");
        
        getChildren().addAll(
            connectionStatus, sep1,
            versionLabel, sep2,
            threadCount, sep3,
            requestCount, spacer, clockLabel
        );
        
        // Start clock
        startClock();
    }
    
    private Label createSeparator() {
        Label sep = new Label("│");
        sep.getStyleClass().add("status-separator");
        return sep;
    }
    
    private void startClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        
        Timeline clock = new Timeline(new KeyFrame(
            Duration.seconds(1),
            e -> {
                clockLabel.setText(LocalDateTime.now().format(formatter));
            }
        ));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
        
        // Initial update
        clockLabel.setText(LocalDateTime.now().format(formatter));
    }
    
    public void setConnected(boolean connected) {
        if (connected) {
            connectionStatus.setText("● Онлайн");
            connectionStatus.setStyle("-fx-text-fill: #238636;");
        } else {
            connectionStatus.setText("○ Офлайн");
            connectionStatus.setStyle("-fx-text-fill: #57606A;");
        }
    }
    
    public void setThreadCount(int count) {
        threadCount.setText("Потоков: " + count);
    }
    
    public void incrementRequestCount() {
        totalRequests++;
        requestCount.setText("Запросов: " + totalRequests);
    }
    
    public void resetRequestCount() {
        totalRequests = 0;
        requestCount.setText("Запросов: 0");
    }
}
