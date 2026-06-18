package com.rmc.ui.dynamic;

import com.rmc.filters.model.FilterGroup;
import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.session.FilterSession;
import com.rmc.filters.ui.FilterCard;
import com.rmc.filters.ui.FilterToolbar;
import com.rmc.logging.AppLogger;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Dynamic filter pane with modern UI.
 * 
 * <p>Automatically builds UI from FilterDefinitions.</p>
 */
public class DynamicFilterPane extends VBox {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private final List<FilterCard> filterCards;
    private FilterSession session;
    private FilterToolbar toolbar;
    private VBox cardsContainer;
    private Label statusLabel;
    
    private Consumer<FilterSession> onSessionChange;
    private Runnable onApply;
    
    public DynamicFilterPane() {
        this.filterCards = new ArrayList<>();
        
        setupPane();
    }
    
    private void setupPane() {
        getStyleClass().add("dynamic-filter-pane");
        setSpacing(16);
        setPadding(new Insets(16));
        setAlignment(Pos.TOP_CENTER);
        
        // Title
        Label titleLabel = new Label("Параметры поиска");
        titleLabel.getStyleClass().add("filter-pane-title");
        
        // Cards container
        cardsContainer = new VBox();
        cardsContainer.setSpacing(12);
        cardsContainer.setPadding(new Insets(8));
        
        // Scroll pane for cards
        ScrollPane scrollPane = new ScrollPane(cardsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("filter-scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Separator
        Separator separator = new Separator();
        separator.getStyleClass().add("filter-separator");
        
        // Status
        statusLabel = new Label();
        statusLabel.getStyleClass().add("filter-pane-status");
        
        getChildren().addAll(titleLabel, scrollPane, separator, statusLabel);
    }
    
    /**
     * Load filters from FilterGroup list.
     */
    public void loadFilters(List<FilterGroup> groups) {
        filterCards.clear();
        cardsContainer.getChildren().clear();
        
        if (groups == null || groups.isEmpty()) {
            statusLabel.setText("Группы не найдены");
            return;
        }
        
        // Create session from groups
        List<FilterDefinition> allFilters = new ArrayList<>();
        for (FilterGroup group : groups) {
            allFilters.addAll(group.getFilters());
        }
        
        session = new FilterSession(groups);
        
        // Create cards
        for (FilterDefinition filter : allFilters) {
            // Skip hidden filters
            if (filter.getType() != null && filter.getType().name().equals("HIDDEN")) {
                continue;
            }
            
            FilterCard card = new FilterCard(filter, session, this::handleSessionChange);
            filterCards.add(card);
            cardsContainer.getChildren().add(card);
        }
        
        updateStatus();
    }
    
    /**
     * Load filters from FilterDefinition list.
     */
    public void loadFilters(List<FilterDefinition> filters, boolean unused) {
        List<FilterGroup> groups = List.of(
            new FilterGroup.Builder()
                .name("main")
                .title("Параметры поиска")
                .filters(filters)
                .build()
        );
        loadFilters(groups);
    }
    
    /**
     * Load filters from FilterDefinition list.
     */
    public void loadFiltersFromList(List<FilterDefinition> filters) {
        List<FilterGroup> groups = List.of(
            new FilterGroup.Builder()
                .name("main")
                .title("Параметры поиска")
                .filters(filters)
                .build()
        );
        loadFilters(groups);
    }
    
    private void handleSessionChange(FilterSession session) {
        updateStatus();
        if (onSessionChange != null) {
            onSessionChange.accept(session);
        }
    }
    
    private void updateStatus() {
        if (session != null) {
            int total = session.getTotalFilterCount();
            int active = session.getActiveFilterCount();
            
            if (active == 0) {
                statusLabel.setText("Загружено " + total + " фильтров");
            } else {
                statusLabel.setText("Загружено " + total + " фильтров | Активно: " + active);
            }
        }
    }
    
    public FilterSession getSession() {
        return session;
    }
    
    public String buildQueryString() {
        if (session == null) return "";
        return session.buildQueryString();
    }
    
    // Legacy method for compatibility
    public java.util.Map<String, String> getAllValues() {
        if (session == null) return java.util.Collections.emptyMap();
        return session.getSingleValues();
    }
    
    public void setOnApply(Runnable onApply) {
        this.onApply = onApply;
        setupToolbar();
    }
    
    public void setOnSessionChange(Consumer<FilterSession> onSessionChange) {
        this.onSessionChange = onSessionChange;
    }
    
    private void setupToolbar() {
        if (session == null || toolbar != null) return;
        
        toolbar = new FilterToolbar(session, 
            () -> {
                if (onApply != null) onApply.run();
            },
            () -> {
                // Reset - rebuild cards to clear values
                if (session != null) {
                    session.clearAll();
                    // Force UI update by rebuilding
                    List<FilterGroup> groups = session.getGroups();
                    filterCards.clear();
                    cardsContainer.getChildren().clear();
                    for (FilterGroup group : groups) {
                        for (FilterDefinition filter : group.getFilters()) {
                            if (filter.getType() == null || !filter.getType().name().equals("HIDDEN")) {
                                FilterCard card = new FilterCard(filter, session, this::handleSessionChange);
                                filterCards.add(card);
                                cardsContainer.getChildren().add(card);
                            }
                        }
                    }
                }
            }
        );
        
        // Add toolbar at the end
        getChildren().add(toolbar);
    }
    
    public List<FilterCard> getFilterCards() {
        return filterCards;
    }
    
    public int getFilterCount() {
        return filterCards.size();
    }
    
    public boolean hasFilters() {
        return !filterCards.isEmpty();
    }
    
    // Legacy methods for compatibility
    public void loadFromHtml(String html) {
        // Legacy - not used
    }
}
