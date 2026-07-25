package com.rmc.ui.topbar;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * Connection status indicator (green/red dot).
 */
public class ConnectionIndicator extends HBox {
    
    private final Label dot;
    private boolean connected = false;
    
    public ConnectionIndicator() {
        setSpacing(6);
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(0, 0, 0, 12));
        
        dot = new Label("●");
        dot.getStyleClass().add("connection-dot");
        
        Label statusText = new Label();
        statusText.getStyleClass().add("connection-text");
        
        getChildren().addAll(dot, statusText);
        
        updateDisplay();
    }
    
    public void setConnected(boolean connected) {
        this.connected = connected;
        updateDisplay();
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    private void updateDisplay() {
        if (connected) {
            dot.setStyle("-fx-text-fill: #238636;");
        } else {
            dot.setStyle("-fx-text-fill: #57606A;");
        }
    }
}
