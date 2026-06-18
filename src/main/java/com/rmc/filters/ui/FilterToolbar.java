package com.rmc.filters.ui;

import com.rmc.filters.session.FilterSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Toolbar for filter actions.
 */
public class FilterToolbar extends HBox {
    
    private final FilterSession session;
    private final Runnable onApply;
    private final Runnable onReset;
    
    private Label statusLabel;
    private Button applyButton;
    private Button resetButton;
    
    public FilterToolbar(FilterSession session, Runnable onApply, Runnable onReset) {
        this.session = session;
        this.onApply = onApply;
        this.onReset = onReset;
        
        setupToolbar();
    }
    
    private void setupToolbar() {
        getStyleClass().add("filter-toolbar");
        setSpacing(16);
        setPadding(new Insets(16, 0, 0, 0));
        setAlignment(Pos.CENTER);
        
        // Status label
        statusLabel = new Label();
        statusLabel.getStyleClass().add("filter-status");
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Reset button
        resetButton = new Button("Сбросить");
        resetButton.getStyleClass().add("filter-reset-button");
        resetButton.setOnAction(e -> handleReset());
        
        // Apply button
        applyButton = new Button("Применить фильтры");
        applyButton.getStyleClass().add("filter-apply-button");
        applyButton.setOnAction(e -> handleApply());
        
        getChildren().addAll(statusLabel, spacer, resetButton, applyButton);
        
        // Update status
        updateStatus();
        
        // Listen for changes
        session.addListener(s -> updateStatus());
    }
    
    private void handleApply() {
        if (onApply != null) {
            onApply.run();
        }
    }
    
    private void handleReset() {
        session.clearAll();
        updateStatus();
        
        if (onReset != null) {
            onReset.run();
        }
    }
    
    public void updateStatus() {
        int total = session.getTotalFilterCount();
        int active = session.getActiveFilterCount();
        
        if (active == 0) {
            statusLabel.setText("Загружено " + total + " фильтров");
        } else {
            statusLabel.setText("Загружено " + total + " фильтров | Активно: " + active);
        }
    }
    
    public Label getStatusLabel() {
        return statusLabel;
    }
    
    public Button getApplyButton() {
        return applyButton;
    }
    
    public Button getResetButton() {
        return resetButton;
    }
}
