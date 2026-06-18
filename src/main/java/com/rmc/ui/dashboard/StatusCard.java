package com.rmc.ui.dashboard;

import com.rmc.state.ApplicationState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

/**
 * Status card showing application status indicators.
 */
public class StatusCard extends VBox {
    
    private StatusIndicator authIndicator;
    private StatusIndicator httpIndicator;
    private StatusIndicator connectionIndicator;
    private StatusIndicator filtersIndicator;
    private StatusIndicator parserIndicator;
    private StatusIndicator excelIndicator;
    
    public StatusCard() {
        getStyleClass().add("card");
        getStyleClass().add("status-card");
        setPadding(new Insets(20));
        setSpacing(16);
        setAlignment(Pos.TOP_LEFT);
        
        Label titleLabel = new Label("Статус приложения");
        titleLabel.getStyleClass().add("card-title");
        
        VBox indicatorsBox = new VBox();
        indicatorsBox.setSpacing(12);
        
        authIndicator = new StatusIndicator("Авторизация");
        httpIndicator = new StatusIndicator("HTTP");
        connectionIndicator = new StatusIndicator("Соединение");
        filtersIndicator = new StatusIndicator("Фильтры");
        parserIndicator = new StatusIndicator("Парсер");
        excelIndicator = new StatusIndicator("Excel");
        
        indicatorsBox.getChildren().addAll(
            authIndicator,
            httpIndicator,
            connectionIndicator,
            filtersIndicator,
            parserIndicator,
            excelIndicator
        );
        
        getChildren().addAll(titleLabel, indicatorsBox);
    }
    
    public void updateStatus() {
        ApplicationState state = ApplicationState.getInstance();
        
        // Auth status
        if (state.isAuthenticated()) {
            authIndicator.setStatus(StatusLevel.GREEN);
        } else {
            authIndicator.setStatus(StatusLevel.GRAY);
        }
        
        // HTTP status (always ready if app is running)
        httpIndicator.setStatus(StatusLevel.GREEN);
        
        // Connection status (placeholder - would need actual ping)
        connectionIndicator.setStatus(StatusLevel.GREEN);
        
        // Filters status
        if (state.getCookieCount() > 0) {
            filtersIndicator.setStatus(StatusLevel.GREEN);
        } else {
            filtersIndicator.setStatus(StatusLevel.GRAY);
        }
        
        // Parser status (placeholder)
        parserIndicator.setStatus(StatusLevel.GREEN);
        
        // Excel status (placeholder)
        excelIndicator.setStatus(StatusLevel.GRAY);
    }
    
    /**
     * Status indicator with colored circle.
     */
    private static class StatusIndicator extends HBox {
        
        private final Label circle;
        private final Label text;
        
        public StatusIndicator(String label) {
            setSpacing(10);
            setAlignment(Pos.CENTER_LEFT);
            
            circle = new Label("●");
            circle.setStyle("-fx-font-size: 16px;");
            
            text = new Label(label);
            text.getStyleClass().add("status-text");
            
            getChildren().addAll(circle, text);
            
            // Default gray
            setStatus(StatusLevel.GRAY);
        }
        
        public void setStatus(StatusLevel level) {
            String color;
            switch (level) {
                case GREEN -> color = "#4CAF50";
                case YELLOW -> color = "#FFC107";
                case RED -> color = "#f44336";
                case GRAY -> color = "#9e9e9e";
                default -> color = "#9e9e9e";
            }
            circle.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 16px;");
        }
    }
    
    private enum StatusLevel {
        GREEN,
        YELLOW,
        RED,
        GRAY
    }
}
