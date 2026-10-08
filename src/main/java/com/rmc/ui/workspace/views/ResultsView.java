package com.rmc.ui.workspace.views;

import com.rmc.export.ExportService;
import com.rmc.logging.AppLogger;
import com.rmc.search.model.AnalysisResult;
import com.rmc.search.model.InstitutionAnalysis;
import com.rmc.search.model.ProgramAnalysis;
import com.rmc.search.service.StatDisplayPreferences;
import com.rmc.ui.icons.TablerIcon;
import com.rmc.ui.icons.TablerIcons;
import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceView;
import com.rmc.ui.workspace.components.ActionButton;
import org.slf4j.Logger;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
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

    private static final Logger logger = AppLogger.getLogger();

    private enum SplitMode {
        INSTITUTIONS_ONLY, SPLIT, PROGRAMS_ONLY
    }
    
    private final WorkspaceContainer container;
    private final StatDisplayPreferences statPrefs = new StatDisplayPreferences();
    
    private final TextField summaryLabel;
    private final CheckBox masterFilteredCheckBox;
    private final CheckBox masterOverallCheckBox;
    private final Label selectionWarningLabel;
    private final ActionButton statsToggleButton;
    private final FlowPane statsCheckboxPane;
    private final FlowPane totalsPane;
    private final VBox institutionsList;
    private final VBox programsList;
    private final VBox institutionsPane;
    private final VBox programsPane;
    private final ActionButton backButton;
    private final ActionButton exportButton;
    private final ActionButton copyButton;
    
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
        
        // Summary line (найдено программ / учреждений) — TextField, а не
        // Label: клиент хочет, чтобы ЛЮБОЙ текст на экране результатов
        // можно было выделить мышью и скопировать, как на обычном сайте.
        // JavaFX Label такого не умеет, а нередактируемый TextField — умеет
        // "из коробки" (выделение, двойной клик по слову, Ctrl+C).
        summaryLabel = selectableField("", "results-summary");
        
        // Master-галочки — разовое действие "отметить/снять всё" в своей
        // группе: пишут напрямую в те же точечные предпочтения, после чего
        // точечные галочки остаются полностью обычными, кликабельными —
        // сама master-галочка ничего не "запирает" и не хранит состояние.
        masterFilteredCheckBox = new CheckBox("Показывать: по фильтру");
        masterFilteredCheckBox.getStyleClass().add("filter-checkbox");
        masterFilteredCheckBox.selectedProperty().addListener((obs, was, isNow) ->
                applyGroupBulkVisibility(true, isNow));
        
        masterOverallCheckBox = new CheckBox("Показывать: общие данные по учреждению");
        masterOverallCheckBox.getStyleClass().add("filter-checkbox");
        masterOverallCheckBox.selectedProperty().addListener((obs, was, isNow) ->
                applyGroupBulkVisibility(false, isNow));
        
        HBox masterRow = new HBox(24, masterFilteredCheckBox, masterOverallCheckBox);
        masterRow.setAlignment(Pos.CENTER_LEFT);
        
        // Красная подсказка под master-галочками: пока не отмечен ни один
        // показатель, экран результатов выглядит пустым — объясняем почему.
        selectionWarningLabel = new Label("Нужно выбрать хотя бы один фильтр");
        selectionWarningLabel.getStyleClass().add("results-selection-warning");
        selectionWarningLabel.setVisible(false);
        selectionWarningLabel.setManaged(false);
        
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
        
        VBox topSection = new VBox(12, summaryLabel, masterRow, selectionWarningLabel, statsToggleButton,
                statsCheckboxPane, totalsPane, new Separator());
        
        // Область с разбивкой: "По учреждениям" слева, "По программам"
        // справа, ширина каждой панели анимированно управляется splitRatio.
        institutionsList = new VBox();
        institutionsList.setSpacing(8);
        institutionsList.setPadding(new Insets(8, 0, 8, 0));
        
        programsList = new VBox();
        programsList.setSpacing(8);
        programsList.setPadding(new Insets(8, 0, 8, 0));
        
        institutionsPane = buildSplitPane("По учреждениям", institutionsList);
        programsPane = buildSplitPane("По программам", programsList);
        
        RatioSplitPane splitContainer = new RatioSplitPane(institutionsPane, programsPane, splitRatio);
        splitContainer.getStyleClass().add("results-split-container");
        VBox.setVgrow(splitContainer, Priority.ALWAYS);
        
        // Back + export buttons
        backButton = new ActionButton("Назад к фильтрам", ActionButton.Style.SECONDARY);
        backButton.setGraphic(TablerIcon.of(TablerIcons.ARROW_LEFT, 14));
        backButton.setOnAction(e -> container.onBackToFilters());
        
        exportButton = new ActionButton("Экспорт в Excel", ActionButton.Style.PRIMARY);
        exportButton.setGraphic(TablerIcon.onPrimary(TablerIcons.DOWNLOAD, 14));
        exportButton.setOnAction(e -> onExport());
        exportButton.setDisable(true);

        // "Скопировать всё" — весь текст результатов (сводка, показатели,
        // учреждения, программы) одним куском в буфер обмена, как если бы
        // пользователь выделил всё это на веб-странице. JavaFX-лейблы не
        // поддерживают выделение текста мышью как на сайте, поэтому это
        // единственный способ скопировать всю информацию сразу.
        copyButton = new ActionButton("Скопировать всё", ActionButton.Style.SECONDARY);
        copyButton.setGraphic(TablerIcon.of(TablerIcons.COPY, 14));
        copyButton.setOnAction(e -> onCopyAll());
        copyButton.setDisable(true);
        
        // Переключатель "какую область показывать" — в том же ряду, что и
        // кнопки "Назад"/"Экспорт", справа от них (их центры на одном
        // уровне), а не поверх самой области с разбивкой.
        HBox toggleRow = buildSplitToggleControls();
        Region buttonsSpacer = new Region();
        HBox.setHgrow(buttonsSpacer, Priority.ALWAYS);
        
        HBox buttonsRow = new HBox();
        buttonsRow.setAlignment(Pos.CENTER_LEFT);
        buttonsRow.setSpacing(12);
        buttonsRow.getChildren().addAll(backButton, exportButton, copyButton, buttonsSpacer, toggleRow);
        
        getChildren().addAll(title, topSection, splitContainer, buttonsRow);
        
        showPlaceholder();
    }
    
    /**
     * Область из двух панелей, делящих свою ширину в пропорции
     * {@code ratio} (0 — вся ширина левой, 1 — вся правой).
     *
     * <p>Раскладка считается вручную, а собственные минимальную/
     * предпочтительную ширину область объявляет нулевыми. Раньше панели
     * растягивались через привязку своей ширины к ширине контейнера, а
     * минимальная ширина самого контейнера считалась JavaFX как сумма
     * минимальных ширин панелей — замкнутый круг: при округлении вверх на
     * каждом кадре анимации контейнер "подрастал" на пиксель-другой, и после
     * нескольких нажатий на стрелки вся программа уезжала за правый край
     * экрана вместе с кнопками. Здесь ширина зависит только от того, что
     * выделил родитель, и обратной связи нет.</p>
     *
     * <p>Каждая панель и сама область обрезаются по своим границам, иначе
     * содержимое схлопнутой (нулевой ширины) панели торчало бы поверх
     * соседней.</p>
     */
    private static final class RatioSplitPane extends Region {
        
        private final Region left;
        private final Region right;
        private final DoubleProperty ratio;
        
        RatioSplitPane(Region left, Region right, DoubleProperty ratio) {
            this.left = left;
            this.right = right;
            this.ratio = ratio;
            getChildren().addAll(left, right);
            ratio.addListener((obs, oldValue, newValue) -> requestLayout());
            clipToBounds(this);
            clipToBounds(left);
            clipToBounds(right);
        }
        
        private static void clipToBounds(Region region) {
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(region.widthProperty());
            clip.heightProperty().bind(region.heightProperty());
            region.setClip(clip);
        }
        
        @Override
        protected double computeMinWidth(double height) {
            return 0;
        }
        
        @Override
        protected double computePrefWidth(double height) {
            return 0;
        }
        
        @Override
        protected double computeMinHeight(double width) {
            return 0;
        }
        
        @Override
        protected double computePrefHeight(double width) {
            return 200;
        }
        
        @Override
        protected void layoutChildren() {
            double width = getWidth();
            double height = getHeight();
            double leftWidth = Math.round(width * (1.0 - ratio.get()));
            left.resizeRelocate(0, 0, leftWidth, height);
            right.resizeRelocate(leftWidth, 0, width - leftWidth, height);
        }
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
        copyButton.setDisable(currentResult == null);
        
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
     * Разовое массовое действие master-галочки: ставит/снимает
     * предпочтение видимости у ВСЕХ показателей указанной группы сразу
     * (по факту принадлежности к {@code currentResult}). Дальше точечные
     * галочки — самые обычные, ничем не заблокированные.
     */
    private void applyGroupBulkVisibility(boolean filteredGroup, boolean visible) {
        if (currentResult == null) {
            return;
        }
        Set<String> keys = filteredGroup
                ? currentResult.getFilteredTotals().keySet()
                : currentResult.getOverallTotals().keySet();
        for (String key : keys) {
            statPrefs.setVisible(key, visible);
        }
        rebuildStatsCheckboxes();
        renderVisibleStats();
    }
    
    private void rebuildStatsCheckboxes() {
        statsCheckboxPane.getChildren().clear();
        if (currentResult == null) {
            return;
        }
        
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
    
    /** Показатели, отмеченные галочками — считываются один раз за перерисовку, а не на каждую строку. */
    private Set<String> selectedStats = Set.of();
    
    /** Названия показателей, которые вообще бывают у отдельной программы (объединение по всем программам). */
    private Set<String> programStatKeys = Set.of();
    
    private void renderVisibleStats() {
        if (currentResult == null) {
            return;
        }
        
        selectedStats = statPrefs.getVisibleStats();
        Set<String> programKeys = new java.util.LinkedHashSet<>();
        for (ProgramAnalysis program : currentResult.getPrograms()) {
            programKeys.addAll(program.getFilteredStats().keySet());
        }
        programStatKeys = programKeys;
        
        updateSelectionWarning();
        
        totalsPane.getChildren().clear();
        for (Map.Entry<String, Integer> entry : currentResult.getFilteredTotals().entrySet()) {
            if (statPrefs.isVisible(entry.getKey())) {
                totalsPane.getChildren().add(createStatCard(entry.getKey(), entry.getValue()));
            }
        }
        for (Map.Entry<String, Integer> entry : currentResult.getOverallTotals().entrySet()) {
            if (statPrefs.isVisible(entry.getKey())) {
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
    
    /**
     * Красная надпись "Нужно выбрать хотя бы один фильтр" видна, пока среди
     * показателей текущего результата не отмечен ни один; как только
     * отмечен хотя бы один (точечно или через master-галочку) — пропадает.
     * Если показателей нет вообще (выбирать нечего) — не показывается.
     */
    private void updateSelectionWarning() {
        Map<String, Integer> available = combinedTotals();
        boolean nothingSelected = !available.isEmpty()
                && available.keySet().stream().noneMatch(statPrefs::isVisible);
        selectionWarningLabel.setVisible(nothingSelected);
        selectionWarningLabel.setManaged(nothingSelected);
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
    
    /**
     * Копирует в буфер обмена весь текст текущих результатов — ровно то,
     * что сейчас видно на экране (сводку, отмеченные галочками показатели,
     * обе колонки — учреждения и программы), как если бы пользователь
     * выделил всё это на веб-странице. Если показателей не отмечено
     * галочками, в текст попадают только названия строк без значений, как
     * и на экране (ничего дополнительно "для экспорта" не подставляется).
     */
    private void onCopyAll() {
        if (currentResult == null) {
            return;
        }

        String text = buildCopyText();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);

        showInfoAlert("Скопировано", "Вся информация со страницы результатов скопирована в буфер обмена.");
    }

    private String buildCopyText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Результаты анализа").append(System.lineSeparator());
        sb.append(summaryLabel.getText()).append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("Показатели:").append(System.lineSeparator());
        for (Map.Entry<String, Integer> entry : currentResult.getFilteredTotals().entrySet()) {
            if (statPrefs.isVisible(entry.getKey())) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue())
                        .append(System.lineSeparator());
            }
        }
        for (Map.Entry<String, Integer> entry : currentResult.getOverallTotals().entrySet()) {
            if (statPrefs.isVisible(entry.getKey())) {
                sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue())
                        .append(System.lineSeparator());
            }
        }
        sb.append(System.lineSeparator());

        sb.append("По учреждениям:").append(System.lineSeparator());
        for (InstitutionAnalysis institution : currentResult.getInstitutions()) {
            String displayName = institution.getOrganizationName() != null
                    ? institution.getOrganizationName()
                    : institution.getOrganizationId();
            sb.append("- ").append(displayName).append(System.lineSeparator());

            if (!institution.isSuccess()) {
                sb.append("    Ошибка: ")
                        .append(institution.getErrorMessage().orElse("не удалось получить данные"))
                        .append(System.lineSeparator());
                continue;
            }

            StringBuilder statsLine = new StringBuilder();
            appendSelected(statsLine, currentResult.getFilteredTotals().keySet(), institution.getFilteredStats());
            if (!institution.getOverallStats().isEmpty()) {
                appendSelected(statsLine, currentResult.getOverallTotals().keySet(), institution.getOverallStats());
            }
            if (statsLine.length() == 0) {
                statsLine.append(NO_SELECTED_STATS_HINT);
            }
            sb.append("    ").append(statsLine).append(System.lineSeparator());

            institution.getOrganizationUrl().ifPresent(url ->
                    sb.append("    Ссылка: ").append(url).append(System.lineSeparator()));
        }
        sb.append(System.lineSeparator());

        sb.append("По программам:").append(System.lineSeparator());
        for (ProgramAnalysis program : currentResult.getPrograms()) {
            sb.append("- ").append(program.getProgramTitle()).append(System.lineSeparator());
            if (program.getOrganizationName() != null) {
                sb.append("    ").append(program.getOrganizationName()).append(System.lineSeparator());
            }

            if (!program.isSuccess()) {
                sb.append("    Ошибка: ")
                        .append(program.getErrorMessage().orElse("не удалось получить данные"))
                        .append(System.lineSeparator());
                continue;
            }

            program.getPriceCategoryEstimate().ifPresent(category ->
                    sb.append("    ").append(ProgramAnalysis.PRICE_CATEGORY_LABEL).append(": ").append(category)
                            .append(System.lineSeparator()));

            StringBuilder statsLine = new StringBuilder();
            appendSelected(statsLine, programStatKeys, program.getFilteredStats());
            if (statsLine.length() == 0) {
                statsLine.append(NO_SELECTED_STATS_HINT);
            }
            sb.append("    ").append(statsLine).append(System.lineSeparator());

            program.getProgramUrl().ifPresent(url ->
                    sb.append("    Ссылка на программу: ").append(url).append(System.lineSeparator()));
            program.getNavigatorUrl().ifPresent(url ->
                    sb.append("    Навигатор: ").append(url).append(System.lineSeparator()));
        }

        return sb.toString();
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
        selectionWarningLabel.setVisible(false);
        selectionWarningLabel.setManaged(false);
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

        TextField valueField = selectableField(String.valueOf(value), "results-stat-value");
        valueField.setAlignment(Pos.CENTER);

        TextArea nameArea = selectableArea(label, "results-stat-label");
        nameArea.setMaxWidth(160);
        nameArea.setPrefWidth(160);

        card.getChildren().addAll(valueField, nameArea);
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
        row.getChildren().add(selectableField(displayName, "results-institution-name"));

        if (!institution.isSuccess()) {
            String errorText = "Ошибка: " + institution.getErrorMessage().orElse("не удалось получить данные");
            row.getChildren().add(selectableArea(errorText, "results-institution-error"));
        } else {
            StringBuilder statsLine = new StringBuilder();
            // Показываем все ОТМЕЧЕННЫЕ показатели, а если у этого учреждения
            // такого показателя нет — пишем 0 (а не молча пропускаем).
            appendSelected(statsLine, currentResult.getFilteredTotals().keySet(), institution.getFilteredStats());
            // Если страница учреждения не отдала показателей вовсе, это скорее
            // сбой загрузки, а не "ноль" — тогда не выдаём нули за данные.
            if (!institution.getOverallStats().isEmpty()) {
                appendSelected(statsLine, currentResult.getOverallTotals().keySet(), institution.getOverallStats());
            }
            if (statsLine.length() == 0) {
                statsLine.append(NO_SELECTED_STATS_HINT);
            }

            row.getChildren().add(selectableArea(statsLine.toString(), "results-institution-stats"));
        }

        return row;
    }

    private Node createProgramRow(ProgramAnalysis program) {
        // Весь текстовый блок — слева, растягивается; кнопка "Навигатор"
        // (если есть ссылка) — справа от него, по центру высоты строки.
        VBox content = new VBox();
        content.setSpacing(4);

        content.getChildren().add(selectableField(program.getProgramTitle(), "results-institution-name"));

        if (program.getOrganizationName() != null) {
            content.getChildren().add(selectableField(program.getOrganizationName(), "results-institution-stats"));
        }

        HBox row = new HBox();
        row.getStyleClass().add("results-institution-row");
        row.setSpacing(12);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(content, Priority.ALWAYS);
        row.getChildren().add(content);

        if (!program.isSuccess()) {
            String errorText = "Ошибка: " + program.getErrorMessage().orElse("не удалось получить данные");
            content.getChildren().add(selectableArea(errorText, "results-institution-error"));
            return row;
        }

        program.getPriceCategoryEstimate().ifPresent(category -> {
            String text = ProgramAnalysis.PRICE_CATEGORY_LABEL + ": " + category;
            content.getChildren().add(selectableField(text, "results-institution-stats"));
        });

        StringBuilder statsLine = new StringBuilder();
        // Ключи — показатели, которые бывают у программ вообще (не путаем с
        // учрежденческими вроде "Программ по фильтру"); чего нет у этой
        // программы — пишем 0.
        appendSelected(statsLine, programStatKeys, program.getFilteredStats());
        if (statsLine.length() == 0) {
            statsLine.append(NO_SELECTED_STATS_HINT);
        }
        content.getChildren().add(selectableArea(statsLine.toString(), "results-institution-stats"));

        // Кнопка "Навигатор" — только если на странице самой программы
        // нашлась такая ссылка (не у всех программ она есть); открывает
        // её в браузере по умолчанию, не внутри приложения. Зелёная, как
        // "Экспорт в Excel" — и справа от текста, а не отдельной строкой.
        program.getNavigatorUrl().ifPresent(url -> {
            ActionButton navigatorButton = new ActionButton("Навигатор", ActionButton.Style.PRIMARY);
            navigatorButton.getStyleClass().add("results-navigator-button");
            navigatorButton.setGraphic(TablerIcon.onPrimary(TablerIcons.EXTERNAL_LINK, 12));
            navigatorButton.setOnAction(e -> openInBrowser(url));
            row.getChildren().add(navigatorButton);
        });

        return row;
    }

    /**
     * Нередактируемое однострочное поле вместо {@link Label} — в отличие от
     * Label, TextField поддерживает выделение текста мышью, двойной клик по
     * слову и Ctrl+C, как на обычной веб-странице. Визуально выглядит как
     * обычный текст благодаря классу "selectable-text" (см. CSS) — без
     * рамки и фона поля ввода.
     */
    private static TextField selectableField(String text, String... styleClasses) {
        TextField field = new TextField(text != null ? text : "");
        field.setEditable(false);
        field.setFocusTraversable(true);
        field.getStyleClass().add("selectable-text");
        field.getStyleClass().addAll(styleClasses);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    /**
     * То же самое, что {@link #selectableField}, но для текста, который
     * может переноситься на несколько строк (TextArea, а не TextField).
     * Высота подгоняется под содержимое автоматически (см.
     * {@link #bindAutoHeight}), чтобы не появлялась внутренняя прокрутка
     * там, где её не было у прежнего Label с setWrapText(true).
     */
    private static TextArea selectableArea(String text, String... styleClasses) {
        TextArea area = new TextArea(text != null ? text : "");
        area.setEditable(false);
        area.setWrapText(true);
        area.setFocusTraversable(true);
        area.getStyleClass().add("selectable-text");
        area.getStyleClass().addAll(styleClasses);
        area.setMaxWidth(Double.MAX_VALUE);
        area.setPrefRowCount(1);
        bindAutoHeight(area);
        return area;
    }

    private static void bindAutoHeight(TextArea area) {
        Text measurer = new Text();
        measurer.textProperty().bind(area.textProperty());
        measurer.fontProperty().bind(area.fontProperty());

        Runnable resize = () -> {
            double width = area.getWidth();
            if (width <= 0) {
                return;
            }
            measurer.setWrappingWidth(Math.max(0, width - 24));
            double textHeight = measurer.getLayoutBounds().getHeight();
            area.setPrefHeight(textHeight + 22);
        };

        area.widthProperty().addListener((obs, oldV, newV) -> resize.run());
        area.textProperty().addListener((obs, oldV, newV) -> Platform.runLater(resize));
        Platform.runLater(resize);
    }

    /**
     * Открывает ссылку в браузере по умолчанию на компьютере пользователя
     * (не внутри самого приложения). Выполняется в отдельном потоке —
     * {@code Desktop.browse} может ненадолго блокировать вызывающий поток
     * на запуск внешнего процесса, а делать это на потоке JavaFX не стоит.
     */
    private void openInBrowser(String url) {
        Thread thread = new Thread(() -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception e) {
                logger.error("Не удалось открыть ссылку навигатора ({}): {}", url, e.getMessage());
                Platform.runLater(() ->
                        showInfoAlert("Ошибка", "Не удалось открыть ссылку в браузере:\n" + e.getMessage()));
            }
        }, "navigator-link-opener");
        thread.setDaemon(true);
        thread.start();
    }

    private static final String NO_SELECTED_STATS_HINT = "(нет отмеченных показателей)";
    
    /**
     * Дописывает в строку значения отмеченных показателей из {@code keys};
     * если у конкретной строки такого показателя нет — выводит 0.
     */
    private void appendSelected(StringBuilder statsLine, Iterable<String> keys, Map<String, Integer> values) {
        for (String key : keys) {
            if (selectedStats.contains(key)) {
                if (statsLine.length() > 0) {
                    statsLine.append("   ");
                }
                statsLine.append(key).append(": ").append(values.getOrDefault(key, 0));
            }
        }
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
