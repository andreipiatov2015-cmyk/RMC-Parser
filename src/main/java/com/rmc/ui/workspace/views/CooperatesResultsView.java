package com.rmc.ui.workspace.views;

import com.rmc.export.ExportService;
import com.rmc.logging.AppLogger;
import com.rmc.parser.model.Cooperate;
import com.rmc.search.model.CooperateAnalysisResult;
import com.rmc.search.model.CooperateTerritoryStat;
import com.rmc.ui.icons.TablerIcon;
import com.rmc.ui.icons.TablerIcons;
import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceView;
import com.rmc.ui.workspace.components.ActionButton;
import com.rmc.ui.workspace.components.SelectableText;
import org.slf4j.Logger;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
 * Экран результатов раздела "Соглашения" — аналог {@link ResultsView}, но
 * проще: один плоский список заявок (без разбивки "по учреждениям" / "по
 * программам"), т.к. все данные уже есть прямо в таблице списка сайта.
 *
 * <p>Повторяет те же приёмы, что и {@link ResultsView}: весь текст на
 * экране выделяется мышью ({@link SelectableText}), есть кнопки "Назад",
 * "Экспорт в Excel" и "Скопировать всё".</p>
 */
public class CooperatesResultsView extends VBox implements WorkspaceView {

    private static final Logger logger = AppLogger.getLogger();

    private final WorkspaceContainer container;

    private final TextField summaryLabel;
    private final FlowPane totalsPane;
    private final VBox cooperatesList;
    private final VBox territoriesList;
    private final ActionButton backButton;
    private final ActionButton exportButton;
    private final ActionButton copyButton;

    private CooperateAnalysisResult currentResult;

    public CooperatesResultsView(WorkspaceContainer container) {
        this.container = container;

        getStyleClass().add("results-view");
        setSpacing(16);

        Label title = new Label("Результаты анализа — Соглашения");
        title.setGraphic(TablerIcon.of(TablerIcons.FILE_TEXT, 20));
        title.getStyleClass().add("results-title");

        summaryLabel = SelectableText.field("", "results-summary");

        totalsPane = new FlowPane();
        totalsPane.getStyleClass().add("results-totals-pane");
        totalsPane.setHgap(12);
        totalsPane.setVgap(12);

        ScrollPane totalsScroll = new ScrollPane(totalsPane);
        totalsScroll.setFitToWidth(true);
        totalsScroll.getStyleClass().add("results-totals-scroll");
        // Та же высота блока показателей, что и в Реестре программ (см.
        // ResultsView) — раньше тут была попытка динамически сжимать
        // область под малое число карточек, но из-за особенностей расчёта
        // высоты FlowPane внутри ScrollPane это давало нестабильный,
        // визуально урезанный результат (видна полоса прокрутки даже когда
        // все карточки должны помещаться). Проще и надёжнее держать
        // фиксированную высоту области одинаковой в обоих разделах.
        totalsScroll.setPrefHeight(220);
        totalsScroll.setMinHeight(220);
        totalsScroll.setMaxHeight(220);

        VBox topSection = new VBox(12, summaryLabel, totalsScroll, new Separator());

        cooperatesList = new VBox();
        cooperatesList.setSpacing(8);
        cooperatesList.setPadding(new Insets(8, 0, 8, 0));

        ScrollPane listScroll = new ScrollPane(cooperatesList);
        listScroll.setFitToWidth(true);
        listScroll.getStyleClass().add("filters-scroll");
        VBox.setVgrow(listScroll, Priority.ALWAYS);

        territoriesList = new VBox();
        territoriesList.setSpacing(8);
        territoriesList.setPadding(new Insets(8, 0, 8, 0));

        ScrollPane territoriesScroll = new ScrollPane(territoriesList);
        territoriesScroll.setFitToWidth(true);
        territoriesScroll.getStyleClass().add("filters-scroll");
        VBox.setVgrow(territoriesScroll, Priority.ALWAYS);

        // Слева — сводка по территориям (общие цифры за год), справа —
        // подробный список заявок; та же схема "две панели", что и в
        // Реестре программ.
        VBox territoriesPane = buildPane("По территориям", territoriesScroll);
        VBox cooperatesPane = buildPane("Заявки", listScroll);
        HBox panes = new HBox(12, territoriesPane, cooperatesPane);
        territoriesPane.setPrefWidth(0);
        cooperatesPane.setPrefWidth(0);
        HBox.setHgrow(territoriesPane, Priority.ALWAYS);
        HBox.setHgrow(cooperatesPane, Priority.ALWAYS);
        territoriesPane.maxWidthProperty().bind(panes.widthProperty().multiply(0.42));
        VBox.setVgrow(panes, Priority.ALWAYS);

        backButton = new ActionButton("Назад к фильтрам", ActionButton.Style.SECONDARY);
        backButton.setGraphic(TablerIcon.of(TablerIcons.ARROW_LEFT, 14));
        backButton.setOnAction(e -> container.showCooperatesSection());

        exportButton = new ActionButton("Экспорт в Excel", ActionButton.Style.PRIMARY);
        exportButton.setGraphic(TablerIcon.onPrimary(TablerIcons.DOWNLOAD, 14));
        exportButton.setOnAction(e -> onExport());
        exportButton.setDisable(true);

        copyButton = new ActionButton("Скопировать всё", ActionButton.Style.SECONDARY);
        copyButton.setGraphic(TablerIcon.of(TablerIcons.COPY, 14));
        copyButton.setOnAction(e -> onCopyAll());
        copyButton.setDisable(true);

        HBox buttonsRow = new HBox(12, backButton, exportButton, copyButton);
        buttonsRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(title, topSection, panes, buttonsRow);

        showPlaceholder();
    }

    public void setResult(CooperateAnalysisResult result) {
        this.currentResult = (result != null && result.isSuccess()) ? result : null;
        exportButton.setDisable(currentResult == null);
        copyButton.setDisable(currentResult == null);

        if (currentResult == null) {
            showPlaceholder();
            return;
        }

        summaryLabel.setText("Найдено заявок: " + currentResult.getTotalCooperates()
                + (currentResult.isCancelled() ? "  —  остановлено пользователем, показаны частичные результаты" : ""));

        totalsPane.getChildren().clear();
        for (Map.Entry<String, Integer> entry : currentResult.getStatusTotals().entrySet()) {
            totalsPane.getChildren().add(createStatCard(entry.getKey(), String.valueOf(entry.getValue())));
        }
        for (Map.Entry<String, Long> entry : currentResult.getConfirmedHoursByPeriod().entrySet()) {
            totalsPane.getChildren().add(createStatCard(
                    yearHoursLabel(entry.getKey()), String.valueOf(entry.getValue())));
        }

        territoriesList.getChildren().clear();
        for (CooperateTerritoryStat stat : currentResult.getTerritoryStats()) {
            territoriesList.getChildren().add(createTerritoryRow(stat));
        }

        cooperatesList.getChildren().clear();
        for (Cooperate cooperate : currentResult.getCooperates()) {
            cooperatesList.getChildren().add(createCooperateRow(cooperate));
        }
    }

    private void showPlaceholder() {
        summaryLabel.setText("Результаты появятся после завершения анализа");
        totalsPane.getChildren().clear();
        territoriesList.getChildren().clear();
        cooperatesList.getChildren().clear();
    }

    private VBox buildPane(String titleText, ScrollPane scroll) {
        Label header = new Label(titleText);
        header.getStyleClass().add("filter-section-title");
        VBox pane = new VBox(8, header, scroll);
        pane.getStyleClass().add("results-split-pane");
        pane.setPadding(new Insets(8));
        return pane;
    }

    private Node createTerritoryRow(CooperateTerritoryStat stat) {
        VBox row = new VBox();
        row.getStyleClass().add("results-institution-row");
        row.setSpacing(4);
        row.setPadding(new Insets(10, 12, 10, 12));

        row.getChildren().add(SelectableText.field(
                stat.getTerritory() + "  —  " + stat.getPeriod(), "results-institution-name"));
        row.getChildren().add(SelectableText.area(territoryStatsLine(stat), "results-institution-stats"));
        return row;
    }

    private static String territoryStatsLine(CooperateTerritoryStat stat) {
        StringBuilder sb = new StringBuilder();
        sb.append("Заявок всего: ").append(stat.getTotal());
        sb.append("   Подтверждено: ").append(stat.getConfirmedCount());
        sb.append("   Новых: ").append(stat.getNewCount());
        sb.append("   Отклонено: ").append(stat.getRejectedCount());
        if (stat.getUnknownCount() > 0) {
            sb.append("   Статус не определён: ").append(stat.getUnknownCount());
        }
        sb.append("   Макс. часов: ").append(stat.getMaxHours());
        sb.append("   Подтверждённых часов: ").append(stat.getConfirmedHours());
        return sb.toString();
    }

    private static String yearHoursLabel(String period) {
        return "Подтверждённых часов за " + period + (period.matches("\\d+") ? " год" : "");
    }

    private Node createStatCard(String label, String value) {
        VBox card = new VBox();
        card.getStyleClass().add("results-stat-card");
        card.setAlignment(Pos.CENTER);
        card.setSpacing(2);
        card.setPadding(new Insets(10, 14, 10, 14));

        TextField valueField = SelectableText.field(value, "results-stat-value", "results-stat-card-field");
        valueField.setAlignment(Pos.CENTER);
        valueField.setPrefColumnCount(Math.max(2, value.length()));

        TextField nameField = SelectableText.field(label, "results-stat-label", "results-stat-card-field");
        nameField.setAlignment(Pos.CENTER);
        nameField.setPrefColumnCount(Math.max(8, label.length()));

        card.getChildren().addAll(valueField, nameField);
        return card;
    }

    private Node createCooperateRow(Cooperate cooperate) {
        VBox row = new VBox();
        row.getStyleClass().add("results-institution-row");
        row.setSpacing(4);
        row.setPadding(new Insets(10, 12, 10, 12));

        String schoolName = cooperate.getSchoolName().orElse("(учреждение не указано)");
        row.getChildren().add(SelectableText.field(schoolName, "results-institution-name"));

        String payerName = cooperate.getPayerName().orElse("");
        if (!payerName.isEmpty()) {
            row.getChildren().add(SelectableText.field(payerName, "results-institution-stats"));
        }

        StringBuilder statsLine = new StringBuilder();
        appendStat(statsLine, "Дата", cooperate.getDate().orElse(null));
        appendStat(statsLine, "Период", cooperate.getPeriod().orElse(null));
        appendStat(statsLine, "Статус", statusText(cooperate));
        appendStat(statsLine, "Макс. часов", cooperate.getMaxHours().orElse(null));
        appendStat(statsLine, "Подтверждённых часов", cooperate.getConfirmedHours().orElse(null));
        row.getChildren().add(SelectableText.area(statsLine.toString(), "results-institution-stats"));

        return row;
    }

    private void appendStat(StringBuilder sb, String label, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append("   ");
        }
        sb.append(label).append(": ").append(value);
    }

    private String statusText(Cooperate cooperate) {
        if (cooperate.getStatusText().isPresent()) {
            return cooperate.getStatusText().get();
        }
        return switch (cooperate.getStatus()) {
            case NEW -> "новая заявка";
            case CONFIRMED -> "подтверждена";
            case REJECTED -> "отклонена";
            case UNKNOWN -> "не определён";
        };
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
        fileChooser.setInitialFileName("RMC_соглашения_" + timestamp + ".xlsx");

        Window window = getScene() != null ? getScene().getWindow() : null;
        File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            return;
        }

        try {
            ExportService.exportCooperatesToExcel(currentResult, file);
            showInfoAlert("Экспорт завершён", "Файл сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Ошибка экспорта соглашений: {}", e.getMessage());
            showInfoAlert("Ошибка экспорта", "Не удалось сохранить файл:\n" + e.getMessage());
        }
    }

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
        sb.append("Результаты анализа — Соглашения").append(System.lineSeparator());
        sb.append(summaryLabel.getText()).append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("Показатели:").append(System.lineSeparator());
        for (Map.Entry<String, Long> entry : currentResult.getConfirmedHoursByPeriod().entrySet()) {
            sb.append("  ").append(yearHoursLabel(entry.getKey())).append(": ").append(entry.getValue())
                    .append(System.lineSeparator());
        }
        for (Map.Entry<String, Integer> entry : currentResult.getStatusTotals().entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue())
                    .append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());

        sb.append("По территориям:").append(System.lineSeparator());
        for (CooperateTerritoryStat stat : currentResult.getTerritoryStats()) {
            sb.append("- ").append(stat.getTerritory()).append(" — ").append(stat.getPeriod())
                    .append(System.lineSeparator());
            sb.append("    ").append(territoryStatsLine(stat)).append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());

        sb.append("Заявки:").append(System.lineSeparator());
        for (Cooperate cooperate : currentResult.getCooperates()) {
            sb.append("- ").append(cooperate.getSchoolName().orElse("(учреждение не указано)"))
                    .append(System.lineSeparator());
            cooperate.getPayerName().ifPresent(name ->
                    sb.append("    ").append(name).append(System.lineSeparator()));

            StringBuilder statsLine = new StringBuilder();
            appendStat(statsLine, "Дата", cooperate.getDate().orElse(null));
            appendStat(statsLine, "Период", cooperate.getPeriod().orElse(null));
            appendStat(statsLine, "Статус", statusText(cooperate));
            appendStat(statsLine, "Макс. часов", cooperate.getMaxHours().orElse(null));
            appendStat(statsLine, "Подтверждённых часов", cooperate.getConfirmedHours().orElse(null));
            sb.append("    ").append(statsLine).append(System.lineSeparator());

            cooperate.getSchoolUrl().ifPresent(url ->
                    sb.append("    Ссылка: ").append(url).append(System.lineSeparator()));
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

    @Override
    public Pane getRoot() {
        return this;
    }

    @Override
    public void onEnter() {
        // Результаты остаются видимыми до следующего анализа — не сбрасываем.
    }
}
