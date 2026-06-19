package com.rmc.ui.topbar;

import com.rmc.state.ApplicationState;
import com.rmc.version.VersionService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Top bar - always visible header.
 */
public class TopBar extends HBox {
    
    private final Label menuButton;
    private final Label titleLabel;
    private final Label versionLabel;
    private final Label userLabel;
    private final ConnectionIndicator connectionIndicator;
    private final Label settingsButton;
    
    private Runnable onMenuClick;
    
    public TopBar() {
        getStyleClass().add("top-bar");
        setPadding(new Insets(0, 16, 0, 12));
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(48);
        
        // Menu button
        menuButton = new Label("☰");
        menuButton.getStyleClass().add("menu-button");
        menuButton.setOnMouseClicked(e -> {
            if (onMenuClick != null) {
                onMenuClick.run();
            }
        });
        
        // Title
        titleLabel = new Label("RMC Framework");
        titleLabel.getStyleClass().add("title-label");
        
        // Version
        versionLabel = new Label("v" + VersionService.getCurrentVersionString());
        versionLabel.getStyleClass().add("version-label");
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // User label
        userLabel = new Label();
        userLabel.getStyleClass().add("user-label");
        
        // Connection indicator
        connectionIndicator = new ConnectionIndicator();
        
        // Settings button
        settingsButton = new Label("⚙");
        settingsButton.getStyleClass().add("settings-button");
        
        getChildren().addAll(menuButton, titleLabel, versionLabel, spacer, userLabel, connectionIndicator, settingsButton);
        
        updateUserInfo();
        updateConnectionStatus();
    }
    
    public void setOnMenuClick(Runnable handler) {
        this.onMenuClick = handler;
    }
    
    public void onAuthStateChanged() {
        updateUserInfo();
    }
    
    private void updateUserInfo() {
        ApplicationState state = ApplicationState.getInstance();
        if (state.isAuthenticated()) {
            userLabel.setText(state.getUsername());
        } else {
            userLabel.setText("");
        }
    }
    
    private void updateConnectionStatus() {
        // In real implementation, check actual connection
        ApplicationState state = ApplicationState.getInstance();
        connectionIndicator.setConnected(state.isAuthenticated());
    }
    
    public void setConnected(boolean connected) {
        connectionIndicator.setConnected(connected);
    }
}
