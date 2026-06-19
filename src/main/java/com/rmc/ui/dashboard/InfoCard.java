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
    private final Label subtitleLabel;
    
    public InfoCard(String title, String value) {
        this(title, value, null);
    }
    
    public InfoCard(String title, String value, String subtitle) {
        getStyleClass().add("info-card");
        setSpacing(2);
        setPadding(new Insets(10));
        setAlignment(Pos.TOP_LEFT);
        setPrefHeight(60);
        
        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("info-card-title");
        
        valueLabel = new Label(value);
        valueLabel.getStyleClass().add("info-card-value");
        
        subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("info-card-subtitle");
        
        getChildren().addAll(titleLabel, valueLabel);
        if (subtitle != null) {
            getChildren().add(subtitleLabel);
        }
    }
    
    public void setValue(String value) {
        valueLabel.setText(value);
    }
    
    public String getValue() {
        return valueLabel.getText();
    }
    
    public void setSubtitle(String subtitle) {
        if (subtitle != null) {
            subtitleLabel.setText(subtitle);
            if (!getChildren().contains(subtitleLabel)) {
                getChildren().add(subtitleLabel);
            }
        }
    }
    
    public String getSubtitle() {
        return subtitleLabel.getText();
    }
    
    public void setStatus(String status) {
        setSubtitle(status);
        if ("Авторизован".equals(status) || "Онлайн".equals(status)) {
            subtitleLabel.setStyle("-fx-text-fill: #238636;");
        } else if ("Не авторизован".equals(status) || "Офлайн".equals(status)) {
            subtitleLabel.setStyle("-fx-text-fill: #57606A;");
        } else {
            subtitleLabel.setStyle("-fx-text-fill: #57606A;");
        }
    }
}
