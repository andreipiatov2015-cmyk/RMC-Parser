package com.rmc.ui.dashboard;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Compact info card for dashboard panel.
 */
public class InfoCard extends VBox {
    
    private final Label titleLabel;
    private final Label valueLabel;
    
    public InfoCard(String title, String value) {
        getStyleClass().add("info-card");
        setSpacing(4);
        setPadding(new Insets(12));
        setAlignment(Pos.TOP_LEFT);
        
        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("info-card-title");
        
        valueLabel = new Label(value);
        valueLabel.getStyleClass().add("info-card-value");
        
        getChildren().addAll(titleLabel, valueLabel);
    }
    
    public void setValue(String value) {
        valueLabel.setText(value);
    }
    
    public String getValue() {
        return valueLabel.getText();
    }
    
    public void setStatus(String status) {
        valueLabel.setText(status);
        if ("Авторизован".equals(status) || "Онлайн".equals(status)) {
            valueLabel.setStyle("-fx-text-fill: #238636;");
        } else if ("Не авторизован".equals(status) || "Офлайн".equals(status)) {
            valueLabel.setStyle("-fx-text-fill: #57606A;");
        } else {
            valueLabel.setStyle("-fx-text-fill: #24292F;");
        }
    }
}
