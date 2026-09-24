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
import javafx.scene.control.TitledPane;
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
            
            // Каждая секция — отдельный сворачиваемый блок, чтобы длинный
            // список фильтров не приходилось листать целиком. Первая
            // секция на сайте идёт без заголовка — даём ей своё название,
            // раз сворачиваемому блоку заголовок нужен в любом случае.
            String title = (group.getTitle() != null && !group.getTitle().isBlank())
                    ? group.getTitle()
                    : (firstVisibleGroup ? "Основные" : "Фильтры");
            firstVisibleGroup = false;
            
            filterCardsContainer.getChildren().add(createSectionPane(title, groupFilters));
        }
        
        statusLabel.setText("Загружено фильтров: " + filterCards.size());
    }
    
    /**
     * Сворачиваемая секция фильтров — заголовок со стрелкой сворачивания
     * (встроено в {@link TitledPane}) и сетка карточек внутри. Развёрнута
     * по умолчанию, чтобы поведение не менялось для тех, кто уже привык
     * видеть все фильтры сразу — сворачивать теперь можно по желанию.
     */
    private TitledPane createSectionPane(String title, List<FilterDefinition> groupFilters) {
        TitledPane pane = new TitledPane(title, createSectionGrid(groupFilters));
        pane.getStyleClass().add("filter-section-pane");
        pane.setExpanded(true);
        pane.setAnimated(true);
        return pane;
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
    
    /**
     * @return текущая сессия фильтров (уже разобранных с сайта), либо
     * {@code null}, если фильтры ещё не были загружены. Используется,
     * например, экраном "Избранные учреждения" для поиска по уже
     * готовому списку учреждений — без повторного похода на сайт.
     */
    public FilterSession getFilterSession() {
        return filterSession;
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
