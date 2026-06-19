package com.rmc.ui.workspace.views;

import com.rmc.filters.model.FilterGroup;
import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.session.FilterSession;
import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceView;
import com.rmc.ui.workspace.components.FilterCard;
import com.rmc.ui.workspace.components.ActionButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Filters view - shows filter cards.
 */
public class FiltersView extends VBox implements WorkspaceView {
    
    private final WorkspaceContainer container;
    private final VBox filterCardsContainer;
    private final Label statusLabel;
    private final ActionButton analyzeButton;
    private FilterSession filterSession;
    private List<FilterCard> filterCards = new ArrayList<>();
    
    public FiltersView(WorkspaceContainer container) {
        this.container = container;
        
        getStyleClass().add("filters-view");
        setSpacing(16);
        
        // Title
        Label title = new Label("Параметры поиска");
        title.getStyleClass().add("filters-title");
        
        // Filter cards scroll pane
        filterCardsContainer = new VBox();
        filterCardsContainer.setSpacing(8);
        filterCardsContainer.setPadding(new Insets(8));
        
        ScrollPane scrollPane = new ScrollPane(filterCardsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("filters-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Separator
        Separator separator = new Separator();
        
        // Status
        statusLabel = new Label("Загружено фильтров: 0");
        statusLabel.getStyleClass().add("filters-status");
        
        // Analyze button
        analyzeButton = new ActionButton("Начать анализ", ActionButton.Style.PRIMARY);
        analyzeButton.setOnAction(e -> onAnalyze());
        analyzeButton.setMaxWidth(Double.MAX_VALUE);
        analyzeButton.setPrefHeight(44);
        
        getChildren().addAll(title, scrollPane, separator, statusLabel, analyzeButton);
    }
    
    public void setFilters(List<FilterGroup> groups) {
        filterCards.clear();
        filterCardsContainer.getChildren().clear();
        
        // Create session
        List<FilterDefinition> allFilters = new ArrayList<>();
        for (FilterGroup group : groups) {
            allFilters.addAll(group.getFilters());
        }
        
        filterSession = new FilterSession(groups);
        
        // Create filter cards
        for (FilterDefinition filter : allFilters) {
            if (filter.getType() != null && filter.getType().name().equals("HIDDEN")) {
                continue;
            }
            
            FilterCard card = new FilterCard(filter, filterSession, this::onFilterChanged);
            filterCards.add(card);
            filterCardsContainer.getChildren().add(card);
        }
        
        statusLabel.setText("Загружено фильтров: " + filterCards.size());
    }
    
    private void onFilterChanged(FilterSession session) {
        int activeCount = session.getActiveFilterCount();
        statusLabel.setText("Загружено фильтров: " + filterCards.size() + " | Активных: " + activeCount);
    }
    
    private void onAnalyze() {
        container.onAnalysisStarted();
        
        // TODO: Actually perform analysis
        // For now, simulate and go to results
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                javafx.application.Platform.runLater(() -> {
                    container.onAnalysisComplete();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    @Override
    public Pane getRoot() {
        return this;
    }
    
    @Override
    public void onEnter() {
        // Reset if needed
    }
}
