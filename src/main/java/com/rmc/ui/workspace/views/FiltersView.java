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
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
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
        
        filterSession = new FilterSession(groups);
        
        boolean firstVisibleGroup = true;
        for (FilterGroup group : groups) {
            List<FilterDefinition> groupFilters = new ArrayList<>();
            for (FilterDefinition filter : group.getFilters()) {
                if (filter.getType() != null && filter.getType().name().equals("HIDDEN")) {
                    continue;
                }
                groupFilters.add(filter);
            }
            if (groupFilters.isEmpty()) {
                continue;
            }
            
            // Первая секция на сайте идёт без заголовка — остальные подписаны
            // так же, как разделители на самой странице.
            if (!firstVisibleGroup) {
                filterCardsContainer.getChildren().add(createSectionHeader(group.getTitle()));
            }
            firstVisibleGroup = false;
            
            filterCardsContainer.getChildren().add(createSectionGrid(groupFilters));
        }
        
        statusLabel.setText("Загружено фильтров: " + filterCards.size());
    }
    
    /**
     * Заголовок секции — подпись по центру между двумя линиями,
     * повторяет ".ui.horizontal.divider.header" на сайте.
     */
    private Node createSectionHeader(String title) {
        HBox header = new HBox();
        header.getStyleClass().add("filter-section-header");
        header.setAlignment(Pos.CENTER);
        header.setSpacing(12);
        header.setPadding(new Insets(8, 0, 0, 0));
        
        Separator left = new Separator();
        HBox.setHgrow(left, Priority.ALWAYS);
        Separator right = new Separator();
        HBox.setHgrow(right, Priority.ALWAYS);
        
        Label label = new Label(title);
        label.getStyleClass().add("filter-section-title");
        
        header.getChildren().addAll(left, label, right);
        return header;
    }
    
    /**
     * Двухколоночная сетка карточек фильтров внутри одной секции —
     * повторяет раскладку "seven wide" колонок на сайте.
     */
    private GridPane createSectionGrid(List<FilterDefinition> groupFilters) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("filters-grid");
        grid.setHgap(12);
        grid.setVgap(12);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col1, col2);
        
        int i = 0;
        for (FilterDefinition filter : groupFilters) {
            FilterCard card = new FilterCard(filter, filterSession, this::onFilterChanged);
            card.setMaxWidth(Double.MAX_VALUE);
            filterCards.add(card);
            grid.add(card, i % 2, i / 2);
            i++;
        }
        
        return grid;
    }
    
    private void onFilterChanged(FilterSession session) {
        int activeCount = session.getActiveFilterCount();
        statusLabel.setText("Загружено фильтров: " + filterCards.size() + " | Активных: " + activeCount);
    }
    
    private void onAnalyze() {
        if (filterSession == null) {
            return;
        }
        String queryString = filterSession.buildQueryString();
        List<String> summary = filterSession.getActiveFiltersSummary();
        container.runAnalysis(queryString, summary);
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
