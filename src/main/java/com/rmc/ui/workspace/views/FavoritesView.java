package com.rmc.ui.workspace.views;

import com.rmc.favorites.FavoriteInstitution;
import com.rmc.favorites.FavoriteInstitutionsService;
import com.rmc.filters.parser.FilterOption;
import com.rmc.filters.session.FilterSession;
import com.rmc.ui.theme.ThemeService;
import com.rmc.ui.icons.TablerIcon;
import com.rmc.ui.icons.TablerIcons;
import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Экран "Избранные учреждения" — карточки добавленных учреждений плюс
 * карточка "+" для добавления нового. Список учреждений для поиска при
 * добавлении берётся из уже загруженного фильтра "Учреждение" (без
 * повторного похода на сайт).
 */
public class FavoritesView extends VBox implements WorkspaceView {
    
    private final WorkspaceContainer container;
    private final FlowPane cardsPane;
    
    public FavoritesView(WorkspaceContainer container) {
        this.container = container;
        
        getStyleClass().add("account-picker-view");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(24);
        setPadding(new Insets(24));
        
        Label title = new Label("Избранные учреждения");
        title.setGraphic(TablerIcon.of(TablerIcons.STAR, 20));
        title.getStyleClass().add("account-picker-title");
        
        cardsPane = new FlowPane();
        cardsPane.getStyleClass().add("account-picker-cards");
        cardsPane.setHgap(20);
        cardsPane.setVgap(20);
        cardsPane.setAlignment(Pos.TOP_CENTER);
        cardsPane.setMaxWidth(760);
        cardsPane.setPrefWrapLength(760);
        
        Button backButton = new Button("Назад к фильтрам");
        backButton.setGraphic(TablerIcon.of(TablerIcons.ARROW_LEFT, 14));
        backButton.getStyleClass().add("action-button-secondary");
        backButton.setOnAction(e -> container.onBackToFilters());
        
        getChildren().addAll(title, cardsPane, backButton);
    }
    
    @Override
    public Pane getRoot() {
        return this;
    }
    
    @Override
    public void onEnter() {
        refresh();
    }
    
    public void refresh() {
        cardsPane.getChildren().clear();
        
        List<FavoriteInstitution> favorites = FavoriteInstitutionsService.loadAll();
        for (FavoriteInstitution favorite : favorites) {
            cardsPane.getChildren().add(createFavoriteCard(favorite));
        }
        cardsPane.getChildren().add(createAddCard());
    }
    
    private Pane createFavoriteCard(FavoriteInstitution favorite) {
        StackPane wrapper = new StackPane();
        wrapper.getStyleClass().add("account-card-wrapper");
        wrapper.setAlignment(Pos.TOP_RIGHT);
        
        VBox card = new VBox();
        card.getStyleClass().add("account-card");
        card.setAlignment(Pos.CENTER);
        card.setSpacing(10);
        card.setPadding(new Insets(20));
        card.setPrefWidth(160);
        
        Label icon = new Label();
        icon.setGraphic(TablerIcon.of(TablerIcons.SCHOOL, 32));
        
        Label nameLabel = new Label(favorite.getName());
        nameLabel.getStyleClass().add("account-card-name");
        nameLabel.setWrapText(true);
        
        card.getChildren().addAll(icon, nameLabel);
        card.setOnMouseClicked(e -> container.showInstitutionDetail(favorite));
        
        Label removeButton = new Label("×");
        removeButton.getStyleClass().add("account-card-remove");
        removeButton.setOnMouseClicked(e -> {
            e.consume();
            FavoriteInstitutionsService.remove(favorite.getId());
            refresh();
        });
        StackPane.setMargin(removeButton, new Insets(6, 6, 0, 0));
        
        wrapper.getChildren().addAll(card, removeButton);
        return wrapper;
    }
    
    private Pane createAddCard() {
        VBox card = new VBox();
        card.getStyleClass().addAll("account-card", "account-card-add");
        card.setAlignment(Pos.CENTER);
        card.setSpacing(10);
        card.setPadding(new Insets(20));
        card.setPrefWidth(160);
        
        StackPane circle = new StackPane();
        circle.getStyleClass().add("account-add-circle");
        circle.setPrefSize(64, 64);
        circle.setMaxSize(64, 64);
        circle.setMinSize(64, 64);
        
        Label plus = new Label("+");
        plus.getStyleClass().add("account-add-plus");
        circle.getChildren().add(plus);
        
        Label label = new Label("Добавить учреждение");
        label.getStyleClass().add("account-card-name");
        label.setWrapText(true);
        
        card.getChildren().addAll(circle, label);
        card.setOnMouseClicked(e -> showAddInstitutionDialog());
        
        return card;
    }
    
    /**
     * Диалог добавления — поиск учреждения по уже загруженному фильтру
     * "Учреждение" (поле школы на сайте, {@code school_id__in}). Если
     * фильтры ещё не были загружены (пользователь не заходил на экран
     * фильтров в этом сеансе), просим сначала сделать это.
     */
    private void showAddInstitutionDialog() {
        FilterSession session = container.getFiltersView().getFilterSession();
        Optional<com.rmc.filters.parser.FilterDefinition> institutionField =
                session != null ? session.getDefinition("school_id__in") : Optional.empty();
        
        if (institutionField.isEmpty() || institutionField.get().getOptions() == null
                || institutionField.get().getOptions().isEmpty()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Избранные учреждения");
            info.setHeaderText(null);
            info.setContentText("Список учреждений ещё не загружен. Сначала откройте экран фильтров "
                    + "(он загружается автоматически после входа) и вернитесь сюда.");
            styleDialog(info);
            info.showAndWait();
            return;
        }
        
        List<FilterOption> options = institutionField.get().getOptions();
        Map<String, String> labelToId = new java.util.LinkedHashMap<>();
        ObservableList<String> allLabels = FXCollections.observableArrayList();
        for (FilterOption opt : options) {
            if (opt.getValue() == null || opt.getValue().isEmpty()) {
                continue; // пропускаем пустой "--------" вариант
            }
            allLabels.add(opt.getLabel());
            labelToId.put(opt.getLabel(), opt.getValue());
        }
        
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавить учреждение");
        styleDialog(dialog);
        dialog.getDialogPane().setPrefSize(420, 420);
        
        Label hint = new Label("Начните вводить название учреждения:");
        hint.getStyleClass().add("account-picker-status");
        
        Label selectedLabel = new Label("Учреждение не выбрано");
        selectedLabel.getStyleClass().add("account-picker-status");
        
        javafx.scene.control.TextField searchField = new javafx.scene.control.TextField();
        searchField.setPromptText("Поиск учреждения...");
        searchField.getStyleClass().add("filter-text");
        
        FilteredList<String> filtered = new FilteredList<>(allLabels, s -> true);
        javafx.scene.control.ListView<String> resultsList = new javafx.scene.control.ListView<>(filtered);
        resultsList.setPrefHeight(240);
        
        // Обычный ListView со списком результатов вместо редактируемого
        // ComboBox с "живой" фильтрацией — комбинация "редактируемое поле +
        // список, который сам себя перестраивает" в JavaFX капризная и
        // ненадёжно регистрирует клик по варианту. Клик по строке обычного
        // ListView — гарантированно рабочее, стандартное поведение.
        String[] chosen = new String[1];
        
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            String query = newText == null ? "" : newText.trim().toLowerCase();
            filtered.setPredicate(item -> query.isEmpty() || item.toLowerCase().contains(query));
        });
        
        resultsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && labelToId.containsKey(newVal)) {
                chosen[0] = newVal;
                selectedLabel.setText("Выбрано: " + newVal);
            }
        });
        
        VBox content = new VBox(10, hint, searchField, resultsList, selectedLabel);
        content.setPadding(new Insets(16));
        content.setAlignment(Pos.CENTER_LEFT);
        dialog.getDialogPane().setContent(content);
        
        ButtonType addButtonType = new ButtonType("Добавить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(addButtonType, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(button -> {
            if (button == addButtonType) {
                String chosenLabel = chosen[0];
                String id = chosenLabel != null ? labelToId.get(chosenLabel) : null;
                if (id != null) {
                    FavoriteInstitutionsService.add(id, chosenLabel);
                    refresh();
                }
            }
        });
    }
    
    private void styleDialog(Dialog<?> dialog) {
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm());
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard-dark.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("app-dialog");
        if (ThemeService.isDarkMode()) {
            dialog.getDialogPane().getStyleClass().add("dark-theme");
        }
    }
    
    private void styleDialog(Alert alert) {
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm());
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard-dark.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("app-dialog");
        if (ThemeService.isDarkMode()) {
            alert.getDialogPane().getStyleClass().add("dark-theme");
        }
    }
}
