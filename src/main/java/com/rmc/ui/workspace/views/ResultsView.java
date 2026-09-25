package com.rmc.ui.workspace.views;

import com.rmc.export.ExportService;
import com.rmc.search.model.AnalysisResult;
import com.rmc.search.model.InstitutionAnalysis;
import com.rmc.search.model.ProgramAnalysis;
import com.rmc.search.service.StatDisplayPreferences;
import com.rmc.ui.icons.TablerIcon;
import com.rmc.ui.icons.TablerIcons;
import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceView;
import com.rmc.ui.workspace.components.ActionButton;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Results view - shows aggregated analysis totals and per-institution /
 * per-program breakdowns side by side.
 *
 * <p>Показатели считаются всегда в двух наборах — только по программам,
 * прошедшим фильтр, и по учреждениям целиком (см. {@link AnalysisResult}).
 * Отображением управляют:</p>
 * <ul>
 *   <li>две "master"-галочки {@link #masterFilteredCheckBox}/
 *       {@link #masterOverallCheckBox} — включают/выключают ЦЕЛИКОМ
 *       соответствующую группу показателей;</li>
 *   <li>сворачиваемый список галочек "Показатели" — точечные галочки на
 *       каждый отдельный показатель. Пока master-галочка группы включена,
 *       показатели этой группы видны в любом случае (их галочки блокированы,
 *       это видно по неактивному виду) — точечные галочки реально что-то
 *       решают только для показателей из ВЫКЛЮЧЕННОЙ сейчас группы, то есть
 *       это способ точечно "выдернуть" один показатель из выключенной
 *       группы, не включая её целиком.</li>
 * </ul>
 *
 * <p>Ниже — область с разбивкой, поделённая на "По учреждениям" и "По
 * программам"; три кнопки в правом нижнем углу этой области переключают,
 * что показывать (только учреждения / поровну / только программы), с
 * плавной анимацией.</p>
 */
public class ResultsView extends VBox implements WorkspaceView {
    
    private enum SplitMode {
        INSTITUTIONS_ONLY, SPLIT, PROGRAMS_ONLY
    }
    
    private final WorkspaceContainer container;
    private final StatDisplayPreferences statPrefs = new StatDisplayPreferences();
    
    private final Label summaryLabel;
    private final CheckBox masterFilteredCheckBox;
    private final CheckBox masterOverallCheckBox;
    private final ActionButton statsToggleButton;
    private final FlowPane statsCheckboxPane;
    private final FlowPane totalsPane;
    private final VBox institutionsList;
    private final VBox programsList;
    private final VBox institutionsPane;
    private final VBox programsPane;
    private final ActionButton backButton;
    private final ActionButton exportButton;
    
    private final DoubleProperty splitRatio = new SimpleDoubleProperty(0.5);
    private SplitMode currentSplitMode = SplitMode.SPLIT;
    
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
        
        // Master-галочки — включают/выключают целиком группу показателей.
        masterFilteredCheckBox = new CheckBox("Показывать: по фильтру");
        masterFilteredCheckBox.getStyleClass().add("filter-checkbox");
        masterFilteredCheckBox.setSelected(statPrefs.isFilteredGroupVisible());
        masterFilteredCheckBox.selectedProperty().addListener((obs, was, isNow) -> {
            statPrefs.setFilteredGroupVisible(isNow);
            rebuildStatsCheckboxes();
            renderVisibleStats();
        });
        
        masterOverallCheckBox = new CheckBox("Показывать: общие данные по учреждению");
        masterOverallCheckBox.getStyleClass().add("filter-checkbox");
        masterOverallCheckBox.setSelected(statPrefs.isOverallGroupVisible());
        masterOverallCheckBox.selectedProperty().addListener((obs, was, isNow) -> {
            statPrefs.setOverallGroupVisible(isNow);
            rebuildStatsCheckboxes();
            renderVisibleStats();
        });
        
        HBox masterRow = new HBox(24, masterFilteredCheckBox, masterOverallCheckBox);
        masterRow.setAlignment(Pos.CENTER_LEFT);
        
        // Сворачиваемый список галочек: точечно, по каждому показателю.
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
        
        VBox topSection = new VBox(12, summaryLabel, masterRow, statsToggleButton, statsCheckboxPane,
                totalsPane, new Separator());
        
        // Область с разбивкой: "По учреждениям" слева, "По программам"
        // справа, ширина каждой панели анимированно управляется splitRatio.
        institutionsList = new VBox();
        institutionsList.setSpacing(8);
        institutionsList.setPadding(new Insets(8, 0, 8, 0));
        
        programsList = new VBox();
        programsList.setSpacing(8);
        programsList.setPadding(new Insets(8, 0, 8, 0));
        
        HBox splitContainer = new HBox();
        splitContainer.getStyleClass().add("results-split-container");
        
        institutionsPane = buildSplitPane("По учреждениям", institutionsList);
        programsPane = buildSplitPane("По программам", programsList);
        
        institutionsPane.minWidthProperty().bind(
                splitContainer.widthProperty().multiply(Bindings.subtract(1.0, splitRatio)));
        institutionsPane.prefWidthProperty().bind(institutionsPane.minWidthProperty());
        institutionsPane.maxWidthProperty().bind(institutionsPane.minWidthProperty());
        
        programsPane.minWidthProperty().bind(splitContainer.widthProperty().multiply(splitRatio));
        programsPane.prefWidthProperty().bind(programsPane.minWidthProperty());
        programsPane.maxWidthProperty().bind(programsPane.minWidthProperty());
        
        splitContainer.getChildren().addAll(institutionsPane, programsPane);
        VBox.setVgrow(splitContainer, Priority.ALWAYS);
        
        // Back + export buttons
        backButton = new ActionButton("Назад к фильтрам", ActionButton.Style.SECONDARY);
        backButton.setGraphic(TablerIcon.of(TablerIcons.ARROW_LEFT, 14));
        backButton.setOnAction(e -> container.onBackToFilters());
        
        exportButton = new ActionButton("Экспорт в Excel", ActionButton.Style.PRIMARY);
        exportButton.setGraphic(TablerIcon.onPrimary(TablerIcons.DOWNLOAD, 14));
        exportButton.setOnAction(e -> onExport());
        exportButton.setDisable(true);
        
        // Переключатель "какую область показывать" — в том же ряду, что и
        // кнопки "Назад"/"Экспорт", справа от них (их центры на одном
        // уровне), а не поверх самой области с разбивкой.
        HBox toggleRow = buildSplitToggleControls();
        Region buttonsSpacer = new Region();
        HBox.setHgrow(buttonsSpacer, Priority.ALWAYS);
        
        HBox buttonsRow = new HBox();
        buttonsRow.setAlignment(Pos.CENTER_LEFT);
        buttonsRow.setSpacing(12);
        buttonsRow.getChildren().addAll(backButton, exportButton, buttonsSpacer, toggleRow);
        
        getChildren().addAll(title, topSection, splitContainer, buttonsRow);
        
        showPlaceholder();
    }
    
    private VBox buildSplitPane(String titleText, VBox contentList) {
        Label header = new Label(titleText);
        header.getStyleClass().add("filter-section-title");
        
        ScrollPane scroll = new ScrollPane(contentList);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("filters-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        
        VBox pane = new VBox(8, header, scroll);
        pane.getStyleClass().add("results-split-pane");
        pane.setPadding(new Insets(8));
        VBox.setVgrow(pane, Priority.ALWAYS);
        return pane;
    }
    
    private HBox buildSplitToggleControls() {
        Label btnLeft = splitToggleButton(TablerIcons.CHEVRON_LEFT, 18,
                "Только «По учреждениям»", () -> animateSplitTo(SplitMode.INSTITUTIONS_ONLY));
        
        Label btnSplit = new Label("‖");
        btnSplit.getStyleClass().add("results-split-toggle-button");
        Tooltip.install(btnSplit, new Tooltip("Показать обе области поровну"));
        btnSplit.setOnMouseClicked(e -> animateSplitTo(SplitMode.SPLIT));
        
        Label btnRight = splitToggleButton(TablerIcons.CHEVRON_RIGHT, 18,
                "Только «По программам»", () -> animateSplitTo(SplitMode.PROGRAMS_ONLY));
        
        HBox row = new HBox(6, btnLeft, btnSplit, btnRight);
        row.getStyleClass().add("results-split-toggle-row");
        row.setAlignment(Pos.CENTER);
        return row;
    }
    
    private Label splitToggleButton(String iconPath, double size, String tooltipText, Runnable action) {
        Label label = new Label();
        label.setGraphic(TablerIcon.of(iconPath, size));
        label.getStyleClass().add("results-split-toggle-button");
        Tooltip.install(label, new Tooltip(tooltipText));
        label.setOnMouseClicked(e -> action.run());
        return label;
    }
    
    /**
     * Плавно переключает область между тремя состояниями — только
     * учреждения, поровну, только программы (примерно как переключение
     * рабочих столов в Windows, только не сдвигом экрана целиком, а
     * плавным перетеканием ширины панелей — так корректно работает и
     * "серединное", поделённое пополам состояние).
     */
    private void animateSplitTo(SplitMode mode) {
        if (mode == currentSplitMode) {
            return;
        }
        currentSplitMode = mode;
        
        double target = switch (mode) {
            case INSTITUTIONS_ONLY -> 0.0;
            case SPLIT -> 0.5;
            case PROGRAMS_ONLY -> 1.0;
        };
        
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(280), new KeyValue(splitRatio, target, Interpolator.EASE_BOTH))
        );
        timeline.play();
    }
    
    /**
     * Отобразить результат анализа: сводку, суммарные показатели (те, что
     * отмечены галочками) и разбивку по учреждениям и по программам.
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
     * объединённые в один список (для списка точечных галочек и подсчёта
     * "что экспортировать")
     */
    private Map<String, Integer> combinedTotals() {
        Map<String, Integer> combined = new LinkedHashMap<>();
        if (currentResult != null) {
            combined.putAll(currentResult.getFilteredTotals());
            combined.putAll(currentResult.getOverallTotals());
        }
        return combined;
    }
    
    /**
     * Видимость показателя из группы "по фильтру": видим, если включена
     * master-галочка этой группы, ИЛИ отмечен точечно.
     */
    private boolean isFilteredStatVisible(String key) {
        return statPrefs.isFilteredGroupVisible() || statPrefs.isVisible(key);
    }
    
    /**
     * То же самое для группы "по учреждению целиком".
     */
    private boolean isOverallStatVisible(String key) {
        return statPrefs.isOverallGroupVisible() || statPrefs.isVisible(key);
    }
    
    /**
     * Общая проверка видимости, когда заранее не известно, из какой
     * группы показатель (например, при экспорте) — определяет группу по
     * фактической принадлежности в {@link #currentResult}.
     */
    private boolean isStatVisible(String key) {
        if (currentResult == null) {
            return false;
        }
        boolean inFiltered = currentResult.getFilteredTotals().containsKey(key);
        boolean inOverall = currentResult.getOverallTotals().containsKey(key);
        return (inFiltered && isFilteredStatVisible(key)) || (inOverall && isOverallStatVisible(key));
    }
    
    private void rebuildStatsCheckboxes() {
        statsCheckboxPane.getChildren().clear();
        if (currentResult == null) {
            return;
        }
        
        Set<String> filteredKeys = currentResult.getFilteredTotals().keySet();
        Set<String> overallKeys = currentResult.getOverallTotals().keySet();
        
        for (String statName : combinedTotals().keySet()) {
            boolean inFiltered = filteredKeys.contains(statName);
            boolean inOverall = overallKeys.contains(statName);
            // Пока включена master-галочка группы, точечная галочка ничего
            // не решает для показателей ЭТОЙ группы — блокируем её и
            // показываем отмеченной, чтобы это было видно, а не подразумевалось.
            boolean forcedByGroup = (inFiltered && statPrefs.isFilteredGroupVisible())
                    || (inOverall && statPrefs.isOverallGroupVisible());
            
            CheckBox checkBox = new CheckBox(statName);
            checkBox.getStyleClass().add("filter-checkbox");
            checkBox.setSelected(forcedByGroup || statPrefs.isVisible(statName));
            checkBox.setDisable(forcedByGroup);
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
        for (Map.Entry<String, Integer> entry : currentResult.getFilteredTotals().entrySet()) {
            if (isFilteredStatVisible(entry.getKey())) {
                totalsPane.getChildren().add(createStatCard(entry.getKey(), entry.getValue()));
            }
        }
        for (Map.Entry<String, Integer> entry : currentResult.getOverallTotals().entrySet()) {
            if (isOverallStatVisible(entry.getKey())) {
                totalsPane.getChildren().add(createStatCard(entry.getKey(), entry.getValue()));
            }
        }
        
        institutionsList.getChildren().clear();
        for (InstitutionAnalysis institution : currentResult.getInstitutions()) {
            institutionsList.getChildren().add(createInstitutionRow(institution));
        }
        
        programsList.getChildren().clear();
        for (ProgramAnalysis program : currentResult.getPrograms()) {
            programsList.getChildren().add(createProgramRow(program));
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
                        .filter(this::isStatVisible)
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
        programsList.getChildren().clear();
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
            for (Map.Entry<String, Integer> entry : institution.getFilteredStats().entrySet()) {
                if (isFilteredStatVisible(entry.getKey())) {
                    appendStat(statsLine, entry);
                }
            }
            for (Map.Entry<String, Integer> entry : institution.getOverallStats().entrySet()) {
                if (isOverallStatVisible(entry.getKey())) {
                    appendStat(statsLine, entry);
                }
            }
            if (statsLine.length() == 0) {
                statsLine.append("(показатели скрыты — включите нужные выше)");
            }
            
            Label statsLabel = new Label(statsLine.toString());
            statsLabel.getStyleClass().add("results-institution-stats");
            statsLabel.setWrapText(true);
            row.getChildren().add(statsLabel);
        }
        
        return row;
    }
    
    private Node createProgramRow(ProgramAnalysis program) {
        VBox row = new VBox();
        row.getStyleClass().add("results-institution-row");
        row.setSpacing(4);
        row.setPadding(new Insets(10, 12, 10, 12));
        
        Label nameLabel = new Label(program.getProgramTitle());
        nameLabel.getStyleClass().add("results-institution-name");
        row.getChildren().add(nameLabel);
        
        if (program.getOrganizationName() != null) {
            Label orgLabel = new Label(program.getOrganizationName());
            orgLabel.getStyleClass().add("results-institution-stats");
            row.getChildren().add(orgLabel);
        }
        
        if (!program.isSuccess()) {
            Label errorLabel = new Label("Ошибка: " + program.getErrorMessage().orElse("не удалось получить данные"));
            errorLabel.getStyleClass().add("results-institution-error");
            row.getChildren().add(errorLabel);
            return row;
        }
        
        program.getPriceCategoryEstimate().ifPresent(category -> {
            Label priceLabel = new Label(ProgramAnalysis.PRICE_CATEGORY_LABEL + ": " + category);
            priceLabel.getStyleClass().add("results-institution-stats");
            row.getChildren().add(priceLabel);
        });
        
        StringBuilder statsLine = new StringBuilder();
        for (Map.Entry<String, Integer> entry : program.getFilteredStats().entrySet()) {
            if (isFilteredStatVisible(entry.getKey())) {
                appendStat(statsLine, entry);
            }
        }
        if (statsLine.length() == 0) {
            statsLine.append("(показатели скрыты — включите нужные выше)");
        }
        
        Label statsLabel = new Label(statsLine.toString());
        statsLabel.getStyleClass().add("results-institution-stats");
        statsLabel.setWrapText(true);
        row.getChildren().add(statsLabel);
        
        return row;
    }
    
    private void appendStat(StringBuilder statsLine, Map.Entry<String, Integer> entry) {
        if (statsLine.length() > 0) {
            statsLine.append("   ");
        }
        statsLine.append(entry.getKey()).append(": ").append(entry.getValue());
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
