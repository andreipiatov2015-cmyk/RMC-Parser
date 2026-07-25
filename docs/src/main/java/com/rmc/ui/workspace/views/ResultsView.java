package com.rmc.ui.workspace.views;

import com.rmc.export.ExportService;
import com.rmc.search.model.AnalysisResult;
import com.rmc.search.model.InstitutionAnalysis;
import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceView;
import com.rmc.ui.workspace.components.ActionButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
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
import java.util.Map;

/**
 * Results view - shows aggregated analysis totals and a per-institution breakdown.
 */
public class ResultsView extends VBox implements WorkspaceView {
    
    private final WorkspaceContainer container;
    private final Label summaryLabel;
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
        Label title = new Label("📊 Результаты анализа");
        title.getStyleClass().add("results-title");
        
        // Summary line (найдено программ / учреждений)
        summaryLabel = new Label();
        summaryLabel.getStyleClass().add("results-summary");
        
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
        scrollContent.getChildren().addAll(summaryLabel, totalsPane, new Separator(), institutionsTitle, institutionsList);
        
        ScrollPane scrollPane = new ScrollPane(scrollContent);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("filters-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        // Back + export buttons
        backButton = new ActionButton("← Назад к фильтрам", ActionButton.Style.SECONDARY);
        backButton.setOnAction(e -> container.onBackToFilters());
        
        exportButton = new ActionButton("📥 Экспорт в Excel", ActionButton.Style.PRIMARY);
        exportButton.setOnAction(e -> onExport());
        exportButton.setDisable(true);
        
        HBox buttonsRow = new HBox();
        buttonsRow.setSpacing(12);
        buttonsRow.getChildren().addAll(backButton, exportButton);
        
        getChildren().addAll(title, scrollPane, buttonsRow);
        
        showPlaceholder();
    }
    
    /**
     * Отобразить результат анализа: сводку, суммарные показатели по всем
     * учреждениям и разбивку по каждому учреждению отдельно.
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
        
        totalsPane.getChildren().clear();
        for (Map.Entry<String, Integer> entry : result.getTotals().entrySet()) {
            totalsPane.getChildren().add(createStatCard(entry.getKey(), entry.getValue()));
        }
        
        institutionsList.getChildren().clear();
        for (InstitutionAnalysis institution : result.getInstitutions()) {
            institutionsList.getChildren().add(createInstitutionRow(institution));
        }
    }
    
    private void onExport() {
        if (currentResult == null) {
            return;
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
        
        try {
            ExportService.exportToExcel(currentResult, file);
            showInfoAlert("Экспорт завершён", "Файл сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            showInfoAlert("Ошибка экспорта", "Не удалось сохранить файл:\n" + e.getMessage());
        }
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
            StringBuilder statsLine = new StringBuilder();
            for (Map.Entry<String, Integer> entry : institution.getStats().entrySet()) {
                if (statsLine.length() > 0) {
                    statsLine.append("   ");
                }
                statsLine.append(entry.getKey()).append(": ").append(entry.getValue());
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
