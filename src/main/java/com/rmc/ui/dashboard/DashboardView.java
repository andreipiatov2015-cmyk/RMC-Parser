package com.rmc.ui.dashboard;

import com.rmc.state.ApplicationState;
import javafx.geometry.Insets;
import javafx.scene.layout.*;

/**
 * Main Dashboard View - modern single-window layout.
 * 
 * <p>Layout:</p>
 * <pre>
 * ┌──────────────────────────────────────────────────────────┐
 * │  HEADER (☰, App title)                                    │
 * ├────────────┬─────────────────────────────────────────────┤
 * │ DASHBOARD  │  FILTER AREA (full width, compact cards)    │
 * │ - User     │  ┌─────────────────────────────────────────┐│
 * │ - Server   │  │ Filter 1  │ Filter 2  │ Filter 3      ││
 * │ - Stats... │  │ Filter 4  │ Filter 5  │ Filter 6...    ││
 * │            │  └─────────────────────────────────────────┘│
 * │            │  ┌─────────────────────────────────────────┐│
 * │            │  │ RESULTS PANEL                           ││
 * │            │  └─────────────────────────────────────────┘│
 * └────────────┴─────────────────────────────────────────────┘
 * </pre>
 */
public class DashboardView extends BorderPane {
    
    private final HeaderPane headerPane;
    private final DashboardPanel dashboardPanel;
    private final FilterContainer filterContainer;
    private final ResultsPanel resultsPanel;
    private final DashboardController controller;
    
    public DashboardView() {
        getStyleClass().add("dashboard-view");
        
        // Create components
        headerPane = new HeaderPane();
        dashboardPanel = new DashboardPanel();
        filterContainer = new FilterContainer();
        resultsPanel = new ResultsPanel();
        controller = new DashboardController(this);
        
        // Layout
        setupLayout();
        
        // Listen for auth state changes
        ApplicationState.getInstance().addAuthStateListener(event -> {
            controller.onAuthStateChanged();
        });
    }
    
    private void setupLayout() {
        // Top: Header
        setTop(headerPane);
        
        // Center: Main content area
        VBox centerContent = new VBox();
        centerContent.getStyleClass().add("center-content");
        centerContent.setSpacing(0);
        
        // Filter container - takes remaining space
        VBox filterArea = new VBox();
        filterArea.getStyleClass().add("filter-area");
        VBox.setVgrow(filterContainer, Priority.ALWAYS);
        
        // Results panel at bottom
        resultsPanel.setVisible(false);
        
        filterArea.getChildren().addAll(filterContainer, resultsPanel);
        VBox.setVgrow(filterContainer, Priority.ALWAYS);
        
        centerContent.getChildren().add(filterArea);
        
        // Main content: left panel + center
        HBox mainContent = new HBox();
        mainContent.getStyleClass().add("main-content");
        mainContent.setSpacing(16);
        mainContent.setPadding(new Insets(16));
        
        // Dashboard panel
        dashboardPanel.setPrefWidth(200);
        
        // Center content area
        VBox filterWrapper = new VBox();
        filterWrapper.getStyleClass().add("filter-wrapper");
        filterWrapper.setPadding(new Insets(0, 16, 16, 0));
        VBox.setVgrow(filterWrapper, Priority.ALWAYS);
        filterWrapper.getChildren().add(filterArea);
        
        mainContent.getChildren().addAll(dashboardPanel, filterWrapper);
        HBox.setHgrow(filterWrapper, Priority.ALWAYS);
        
        setCenter(mainContent);
    }
    
    public HeaderPane getHeaderPane() {
        return headerPane;
    }
    
    public DashboardPanel getDashboardPanel() {
        return dashboardPanel;
    }
    
    public FilterContainer getFilterContainer() {
        return filterContainer;
    }
    
    public ResultsPanel getResultsPanel() {
        return resultsPanel;
    }
    
    public DashboardController getController() {
        return controller;
    }
    
    public void showResults() {
        resultsPanel.setVisible(true);
    }
}
