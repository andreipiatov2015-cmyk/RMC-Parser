package com.rmc.ui.workspace.views;

import com.rmc.export.ExportService;
import com.rmc.search.model.AnalysisResult;
import com.rmc.search.model.InstitutionAnalysis;
import com.rmc.search.service.StatDisplayPreferences;
import com.rmc.ui.icons.TablerIcon;
import com.rmc.ui.icons.TablerIcons;
import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceView;
import com.rmc.ui.workspace.components.ActionButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Results view - shows aggregated analysis totals and a per-institution breakdown.
 *
 * <p>Показатели считаются всегда в двух наборах — только по программам,
 * прошедшим фильтр, и по учреждениям целиком (см. {@link AnalysisResult}).
 * Какие из них реально показывать на экране, решает пользователь через
 * сворачиваемый список галочек "Показатели"; выбор сохраняется между
 * запусками программы ({@link StatDisplayPreferences}).</p>
 */
public class ResultsView extends VBox implements WorkspaceView {
    
    private final WorkspaceContainer container;
    private final StatDisplayPreferences statPrefs = new StatDisplayPreferences();
    
    private final Label summaryLabel;
    private final ActionButton statsToggleButton;
    private final FlowPane statsCheckboxPane;
    private final FlowPane totalsPane;
    private final VBox institutionsList;
    private final ActionButton backButton;
    private final ActionButton exportButton;
    private AnalysisResult currentResult;
    
    public ResultsView(WorkspaceContainer container) {
        this.container = container;
        
        getStyleClass().add("results-view");
        setSpacing(16);
        
        // Title
        Label title = new Label("Результаты анализа");
        title.setGraphic(TablerIcon.of(TablerIcons.CHART_BAR, 20));
        title.getStyleClass().add("results-title");
        
        // Summary line (найдено программ / учреждений)
        summaryLabel = new Label();
        summaryLabel.getStyleClass().add("results-summary");
        
        // Сворачиваемый список галочек: какие показатели показывать.
        statsToggleButton = new ActionButton("Показатели ▾", ActionButton.Style.SECONDARY);
        statsToggleButton.setGraphic(TablerIcon.of(TablerIcons.ADJUSTMENTS_HORIZONTAL, 14));
        statsToggleButton.setOnAction(e -> toggleStatsPanel());
        
        statsCheckboxPane = new FlowPane();
        statsCheckboxPane.getStyleClass().add("results-stats-checkbox-pane");
        statsCheckboxPane.setHgap(16);
        statsCheckboxPane.setVgap(8);
        statsCheckboxPane.setPadding(new Insets(8, 4, 8, 4));
        statsCheckboxPane.setVisible(false);
        statsCheckboxPane.setManaged(false);
        
        // Итоговые показатели по всем учреждениям
        totalsPane = new FlowPane();
        totalsPane.getStyleClass().add("results-totals-pane");
        totalsPane.setHgap(12);
        totalsPane.setVgap(12);
        
        Label institutionsTitle = new Label("По учреждениям:");
        institutionsTitle.getStyleClass().add("filter-section-title");
        
        institutionsList = new VBox();
        institutionsList.setSpacing(8);
        institutionsList.setPadding(new Insets(8, 0, 8, 0));
        
        VBox scrollContent = new VBox();
        scrollContent.setSpacing(16);
        scrollContent.setPadding(new Insets(8));
        scrollContent.getChildren().addAll(summaryLabel, statsToggleButton, statsCheckboxPane,
                totalsPane, new Separator(), institutionsTitle, institutionsList);
        
        ScrollPane scrollPane = new ScrollPane(scrollContent);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("filters-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Back + export buttons
        backButton = new ActionButton("Назад к фильтрам", ActionButton.Style.SECONDARY);
        backButton.setGraphic(TablerIcon.of(TablerIcons.ARROW_LEFT, 14));
        backButton.setOnAction(e -> container.onBackToFilters());
        
        exportButton = new ActionButton("Экспорт в Excel", ActionButton.Style.PRIMARY);
        exportButton.setGraphic(TablerIcon.onPrimary(TablerIcons.DOWNLOAD, 14));
        exportButton.setOnAction(e -> onExport());
        exportButton.setDisable(true);
        
        HBox buttonsRow = new HBox();
        buttonsRow.setSpacing(12);
        buttonsRow.getChildren().addAll(backButton, exportButton);
        
        getChildren().addAll(title, scrollPane, buttonsRow);
        
        showPlaceholder();
    }
    
    /**
     * Отобразить результат анализа: сводку, суммарные показатели (те, что
     * отмечены галочками) и разбивку по каждому учреждению отдельно.
     * Экспорт в Excel при этом всегда содержит ВСЕ посчитанные показатели,
     * независимо от галочек — они влияют только на то, что видно на экране.
     */
    public void setResult(AnalysisResult result) {
        this.currentResult = (result != null && result.isSuccess()) ? result : null;
        exportButton.setDisable(currentResult == null);
        
        if (currentResult == null) {
            showPlaceholder();
            return;
        }
        
        summaryLabel.setText("Найдено программ: " + result.getTotalPrograms()
                + "  |  Учреждений: " + result.getTotalInstitutions()
                + (result.isCancelled() ? "  —  остановлено пользователем, показаны частичные результаты" : ""));
        
        rebuildStatsCheckboxes();
        renderVisibleStats();
    }
    
    /**
     * @return суммарные показатели по фильтру и по учреждениям целиком,
     * объединённые в один список (в порядке: сначала по фильтру, потом
     * общие) — именно из этого набора строится и список галочек, и
     * "квадратики" сверху
     */
    private Map<String, Integer> combinedTotals() {
        Map<String, Integer> combined = new LinkedHashMap<>();
        if (currentResult != null) {
            combined.putAll(currentResult.getFilteredTotals());
            combined.putAll(currentResult.getOverallTotals());
        }
        return combined;
    }
    
    private void rebuildStatsCheckboxes() {
        statsCheckboxPane.getChildren().clear();
        for (String statName : combinedTotals().keySet()) {
            CheckBox checkBox = new CheckBox(statName);
            checkBox.getStyleClass().add("filter-checkbox");
            checkBox.setSelected(statPrefs.isVisible(statName));
            checkBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                statPrefs.setVisible(statName, isSelected);
                renderVisibleStats();
            });
            statsCheckboxPane.getChildren().add(checkBox);
        }
    }
    
    private void renderVisibleStats() {
        if (currentResult == null) {
            return;
        }
        
        totalsPane.getChildren().clear();
        for (Map.Entry<String, Integer> entry : combinedTotals().entrySet()) {
            if (statPrefs.isVisible(entry.getKey())) {
                totalsPane.getChildren().add(createStatCard(entry.getKey(), entry.getValue()));
            }
        }
        
        institutionsList.getChildren().clear();
        for (InstitutionAnalysis institution : currentResult.getInstitutions()) {
            institutionsList.getChildren().add(createInstitutionRow(institution));
        }
    }
    
    private void toggleStatsPanel() {
        boolean nowVisible = !statsCheckboxPane.isVisible();
        statsCheckboxPane.setVisible(nowVisible);
        statsCheckboxPane.setManaged(nowVisible);
        statsToggleButton.setText(nowVisible ? "Показатели ▴" : "Показатели ▾");
    }
    
    private void onExport() {
        if (currentResult == null) {
            return;
        }
        
        Boolean onlyVisible = askExportScope();
        if (onlyVisible == null) {
            return; // пользователь нажал "Отмена"
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить отчёт");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        fileChooser.setInitialFileName("RMC_анализ_" + timestamp + ".xlsx");
        
        Window window = getScene() != null ? getScene().getWindow() : null;
        File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            return;
        }
        
        Set<String> visibleStats = onlyVisible
                ? combinedTotals().keySet().stream()
                        .filter(statPrefs::isVisible)
                        .collect(java.util.stream.Collectors.toSet())
                : null;
        
        try {
            ExportService.exportToExcel(currentResult, file, visibleStats);
            showInfoAlert("Экспорт завершён", "Файл сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            showInfoAlert("Ошибка экспорта", "Не удалось сохранить файл:\n" + e.getMessage());
        }
    }
    
    /**
     * Спрашивает перед экспортом: выгружать все посчитанные показатели
     * (даже скрытые галочками на экране) или только отмеченные сейчас.
     *
     * @return {@code true} — только отмеченные, {@code false} — все,
     * {@code null} — пользователь нажал "Отмена" (экспорт не выполняется)
     */
    private Boolean askExportScope() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Экспорт в Excel");
        alert.setHeaderText(null);
        alert.setContentText("Какие показатели включить в файл?");
        
        ButtonType allButton = new ButtonType("Все показатели");
        ButtonType visibleButton = new ButtonType("Только отмеченные галочками");
        alert.getButtonTypes().setAll(allButton, visibleButton, ButtonType.CANCEL);
        
        return alert.showAndWait()
                .map(button -> {
                    if (button == allButton) {
                        return Boolean.FALSE;
                    } else if (button == visibleButton) {
                        return Boolean.TRUE;
                    }
                    return null;
                })
                .orElse(null);
    }
    
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showPlaceholder() {
        summaryLabel.setText("Результаты появятся после завершения анализа");
        statsCheckboxPane.getChildren().clear();
        totalsPane.getChildren().clear();
        institutionsList.getChildren().clear();
    }
    
    private Node createStatCard(String label, int value) {
        VBox card = new VBox();
        card.getStyleClass().add("results-stat-card");
        card.setAlignment(Pos.CENTER);
        card.setSpacing(4);
        card.setPadding(new Insets(12, 16, 12, 16));
        
        Label valueLabel = new Label(String.valueOf(value));
        valueLabel.getStyleClass().add("results-stat-value");
        
        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("results-stat-label");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(160);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        card.getChildren().addAll(valueLabel, nameLabel);
        return card;
    }
    
    private Node createInstitutionRow(InstitutionAnalysis institution) {
        VBox row = new VBox();
        row.getStyleClass().add("results-institution-row");
        row.setSpacing(4);
        row.setPadding(new Insets(10, 12, 10, 12));
        
        String displayName = institution.getOrganizationName() != null
                ? institution.getOrganizationName()
                : institution.getOrganizationId();
        Label nameLabel = new Label(displayName);
        nameLabel.getStyleClass().add("results-institution-name");
        row.getChildren().add(nameLabel);
        
        if (!institution.isSuccess()) {
            Label errorLabel = new Label("Ошибка: " + institution.getErrorMessage().orElse("не удалось получить данные"));
            errorLabel.getStyleClass().add("results-institution-error");
            row.getChildren().add(errorLabel);
        } else {
            Map<String, Integer> combined = new LinkedHashMap<>();
            combined.putAll(institution.getFilteredStats());
            combined.putAll(institution.getOverallStats());
            
            StringBuilder statsLine = new StringBuilder();
            for (Map.Entry<String, Integer> entry : combined.entrySet()) {
                if (!statPrefs.isVisible(entry.getKey())) {
                    continue;
                }
                if (statsLine.length() > 0) {
                    statsLine.append("   ");
                }
                statsLine.append(entry.getKey()).append(": ").append(entry.getValue());
            }
            if (statsLine.length() == 0) {
                statsLine.append("(показатели скрыты — включите нужные в разделе \"Показатели\" выше)");
            }
            
            Label statsLabel = new Label(statsLine.toString());
            statsLabel.getStyleClass().add("results-institution-stats");
            statsLabel.setWrapText(true);
            row.getChildren().add(statsLabel);
        }
        
        return row;
    }
    
    @Override
    public Pane getRoot() {
        return this;
    }
    
    @Override
    public void onEnter() {
        // Результаты остаются видимыми до следующего анализа — не сбрасываем.
    }
}
