package com.rmc.ui.dashboard;

import com.rmc.filters.model.FilterGroup;
import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.service.FilterService;
import com.rmc.http.HttpClientService;
import com.rmc.state.ApplicationState;
import com.rmc.ui.dynamic.DynamicFilterPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Container for dynamic filters.
 * Shows placeholder when filters not loaded, or dynamic filter UI when loaded.
 */
public class FilterContainer extends VBox {
    
    private static final String RMC_BASE_URL = "https://rmc.ruobr.ru";
    
    private Label titleLabel;
    private VBox placeholderPane;
    private DynamicFilterPane dynamicFilterPane;
    private boolean filtersLoaded = false;
    private FilterService filterService;
    
    public FilterContainer() {
        getStyleClass().add("card");
        getStyleClass().add("filter-container");
        setPadding(new Insets(20));
        setSpacing(16);
        
        titleLabel = new Label("Параметры поиска");
        titleLabel.getStyleClass().add("card-title");
        
        placeholderPane = createPlaceholder();
        dynamicFilterPane = new DynamicFilterPane();
        dynamicFilterPane.setVisible(false);
        
        getChildren().addAll(titleLabel, placeholderPane, dynamicFilterPane);
    }
    
    private VBox createPlaceholder() {
        VBox placeholder = new VBox();
        placeholder.setSpacing(16);
        placeholder.setAlignment(Pos.CENTER);
        
        Label messageLabel = new Label("Фильтры еще не загружены");
        messageLabel.getStyleClass().add("placeholder-text");
        
        Button loadButton = new Button("Получить фильтры");
        loadButton.getStyleClass().add("primary-button");
        loadButton.setOnAction(e -> handleLoadFilters());
        
        Label hintLabel = new Label("Нажмите кнопку для загрузки фильтров с сервера");
        hintLabel.getStyleClass().add("hint-text");
        
        placeholder.getChildren().addAll(messageLabel, loadButton, hintLabel);
        
        return placeholder;
    }
    
    private void handleLoadFilters() {
        if (!ApplicationState.getInstance().isAuthenticated()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Внимание");
            alert.setHeaderText("Необходима авторизация");
            alert.setContentText("Для получения фильтров необходимо выполнить вход.");
            alert.show();
            return;
        }
        
        placeholderPane.setVisible(false);
        
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(50, 50);
        getChildren().add(progress);
        
        new Thread(() -> {
            try {
                HttpClientService httpClient = ApplicationState.getInstance().getHttpClient();
                filterService = new FilterService(httpClient, RMC_BASE_URL);
                
                var result = filterService.loadFilters();
                
                javafx.application.Platform.runLater(() -> {
                    getChildren().remove(progress);
                    
                    if (result.isSuccess()) {
                        // Загружаем фильтры в UI
                        if (!result.getGroups().isEmpty()) {
                            FilterGroup mainGroup = result.getGroups().get(0);
                            dynamicFilterPane.loadFilters(mainGroup.getFilters());
                        }
                        dynamicFilterPane.setVisible(true);
                        filtersLoaded = true;
                        
                        // Обновляем статус в Header
                        DashboardView view = (DashboardView) getScene().getRoot();
                        if (view != null && view.getController() != null) {
                            view.getController().logInfo("Загружено фильтров: " + result.getFilterCount());
                        }
                    } else {
                        showError("Ошибка: " + result.getErrorMessage().orElse("unknown"));
                        placeholderPane.setVisible(true);
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    getChildren().remove(progress);
                    showError("Ошибка: " + e.getMessage());
                    placeholderPane.setVisible(true);
                });
            }
        }).start();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText("Ошибка загрузки фильтров");
        alert.setContentText(message);
        alert.show();
    }
    
    public boolean isFiltersLoaded() {
        return filtersLoaded;
    }
    
    public DynamicFilterPane getDynamicFilterPane() {
        return dynamicFilterPane;
    }
}
