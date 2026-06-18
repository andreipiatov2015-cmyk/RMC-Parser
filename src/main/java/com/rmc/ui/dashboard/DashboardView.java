package com.rmc.ui.dashboard;

import com.rmc.i18n.Messages;
import com.rmc.state.ApplicationState;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Main Dashboard View - modern single-window layout.
 * 
 * <p>Layout:</p>
 * <pre>
 * ┌─────────────────────────────────────────────────────┐
 * │  HEADER (App title, buttons)                        │
 * ├──────────────┬──────────────────────────────────────┤
 * │  LEFT PANEL │  CENTER PANEL                       │
 * │  - LoginCard│  - FilterContainer                  │
 * │  - StatusCard                                      │
 * ├──────────────┴──────────────────────────────────────┤
 * │  BOTTOM PANEL - LogPane                            │
 * └─────────────────────────────────────────────────────┘
 * </pre>
 */
public class DashboardView extends BorderPane {
    
    private final HeaderPane headerPane;
    private final LoginCard loginCard;
    private final StatusCard statusCard;
    private final FilterContainer filterContainer;
    private final LogPane logPane;
    private final DashboardController controller;
    
    public DashboardView() {
        getStyleClass().add("dashboard-view");
        
        // Create components
        headerPane = new HeaderPane();
        loginCard = new LoginCard();
        statusCard = new StatusCard();
        filterContainer = new FilterContainer();
        logPane = new LogPane();
        controller = new DashboardController(this);
        
        // Layout
        setupLayout();
        
        // Initial status update
        updateStatus();
        
        // Listen for auth state changes
        ApplicationState.getInstance().addAuthStateListener(event -> {
            controller.onAuthStateChanged();
            updateStatus();
        });
    }
    
    private void setupLayout() {
        // Top: Header
        setTop(headerPane);
        
        // Left panel with cards
        VBox leftPanel = new VBox();
        leftPanel.setSpacing(16);
        leftPanel.setPadding(new Insets(0, 16, 0, 0));
        leftPanel.getStyleClass().add("left-panel");
        
        VBox.setVgrow(loginCard, Priority.NEVER);
        VBox.setVgrow(statusCard, Priority.ALWAYS);
        
        leftPanel.getChildren().addAll(loginCard, statusCard);
        
        // Center panel
        VBox centerPanel = new VBox();
        centerPanel.getStyleClass().add("center-panel");
        VBox.setVgrow(filterContainer, Priority.ALWAYS);
        centerPanel.getChildren().add(filterContainer);
        
        // Main content: left + center
        HBox mainContent = new HBox();
        mainContent.getStyleClass().add("main-content");
        mainContent.getChildren().addAll(leftPanel, centerPanel);
        HBox.setHgrow(centerPanel, Priority.ALWAYS);
        
        // Bottom: Log pane
        VBox bottomPanel = new VBox();
        bottomPanel.getStyleClass().add("bottom-panel");
        bottomPanel.getChildren().add(logPane);
        VBox.setVgrow(logPane, Priority.NEVER);
        
        // Container for main content and bottom
        VBox contentArea = new VBox();
        contentArea.getStyleClass().add("content-area");
        contentArea.getChildren().addAll(mainContent, bottomPanel);
        VBox.setVgrow(mainContent, Priority.ALWAYS);
        
        setCenter(contentArea);
    }
    
    public void updateStatus() {
        statusCard.updateStatus();
    }
    
    public HeaderPane getHeaderPane() {
        return headerPane;
    }
    
    public LoginCard getLoginCard() {
        return loginCard;
    }
    
    public StatusCard getStatusCard() {
        return statusCard;
    }
    
    public FilterContainer getFilterContainer() {
        return filterContainer;
    }
    
    public LogPane getLogPane() {
        return logPane;
    }
    
    public DashboardController getController() {
        return controller;
    }
}
