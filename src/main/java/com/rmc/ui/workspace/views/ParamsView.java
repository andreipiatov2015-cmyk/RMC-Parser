package com.rmc.ui.workspace.views;

import com.rmc.config.ServerConfig;
import com.rmc.logging.AppLogger;
import com.rmc.export.ParamsExportService;
import com.rmc.params.ParamsAnalysisResult;
import com.rmc.params.ParamsCard;
import com.rmc.params.ParamsPage;
import com.rmc.params.ParamsTable;
import com.rmc.ui.icons.TablerIcon;
import com.rmc.ui.icons.TablerIcons;
import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceView;
import com.rmc.ui.workspace.components.ActionButton;
import com.rmc.ui.workspace.components.RatioSplitPane;
import com.rmc.ui.workspace.components.SelectableText;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Экран раздела "Параметры РМЦ": данные трёх страниц сайта (параметры
 * муниципалитета, параметры групп сертификатов, потенциальный баланс),
 * собранные вместе по общему фильтру. Вместо карточек с цифрами сверху —
 * кнопка "Фильтры" (как на самом сайте).
 *
 * <p>Слева — список "территория + год", справа — все карточки выбранной
 * территории с трёх страниц.</p>
 */
public class ParamsView extends VBox implements WorkspaceView {

    private static final Logger logger = AppLogger.getLogger();

    private static final String SELECTED_STYLE = "-fx-border-color: #2563eb; -fx-border-width: 2; -fx-border-radius: 6;";

    private final WorkspaceContainer container;

    private final ActionButton filtersButton;
    private final ActionButton modeButton;
    private final TableView<ParamsTable.Row> tableView = new TableView<>();
    private final StackPane contentHolder = new StackPane();
    private final RatioSplitPane panes;
    private final HBox buttonsRow;
    private final HBox toggleRow;
    private boolean tableMode = true;
    private final TextField summaryLabel;
    private final Label activeFiltersLabel;
    private final VBox territoriesList;
    private final VBox detailsList;
    private final ActionButton exportButton;
    private final ActionButton copyButton;

    private ParamsAnalysisResult currentResult;
    private List<String> currentFilterSummary = List.of();
    private final List<VBox> territoryRows = new ArrayList<>();

    public ParamsView(WorkspaceContainer container) {
        this.container = container;

        getStyleClass().add("results-view");
        setSpacing(16);

        Label title = new Label("Параметры РМЦ");
        title.setGraphic(TablerIcon.of(TablerIcons.CLIPBOARD_LIST, 20));
        title.getStyleClass().add("results-title");

        filtersButton = new ActionButton("Фильтры", ActionButton.Style.PRIMARY);
        filtersButton.setGraphic(TablerIcon.onPrimary(TablerIcons.FILTER, 14));
        filtersButton.setOnAction(e -> container.showParamsFilters());

        modeButton = new ActionButton("", ActionButton.Style.SECONDARY);
        modeButton.setOnAction(e -> setTableMode(!tableMode));

        summaryLabel = SelectableText.field("", "results-summary");
        HBox.setHgrow(summaryLabel, Priority.ALWAYS);

        HBox topRow = new HBox(16, filtersButton, modeButton, summaryLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        activeFiltersLabel = new Label();
        activeFiltersLabel.getStyleClass().add("filters-status");
        activeFiltersLabel.setWrapText(true);

        VBox topSection = new VBox(8, topRow, activeFiltersLabel, new Separator());

        territoriesList = new VBox();
        territoriesList.setSpacing(8);
        territoriesList.setPadding(new Insets(8, 0, 8, 0));
        ScrollPane territoriesScroll = new ScrollPane(territoriesList);
        territoriesScroll.setFitToWidth(true);
        territoriesScroll.getStyleClass().add("filters-scroll");
        VBox.setVgrow(territoriesScroll, Priority.ALWAYS);

        detailsList = new VBox();
        detailsList.setSpacing(8);
        detailsList.setPadding(new Insets(8, 0, 8, 0));
        ScrollPane detailsScroll = new ScrollPane(detailsList);
        detailsScroll.setFitToWidth(true);
        detailsScroll.getStyleClass().add("filters-scroll");
        VBox.setVgrow(detailsScroll, Priority.ALWAYS);

        VBox territoriesPane = buildPane("Территории", territoriesScroll);
        VBox detailsPane = buildPane("Параметры", detailsScroll);
        panes = new RatioSplitPane(territoriesPane, detailsPane);
        panes.getStyleClass().add("results-split-container");

        buildTable();
        contentHolder.getChildren().addAll(tableView, panes);
        VBox.setVgrow(contentHolder, Priority.ALWAYS);

        exportButton = new ActionButton("Экспорт в Excel", ActionButton.Style.PRIMARY);
        exportButton.setGraphic(TablerIcon.onPrimary(TablerIcons.DOWNLOAD, 14));
        exportButton.setOnAction(e -> onExport());
        exportButton.setDisable(true);

        copyButton = new ActionButton("Скопировать всё", ActionButton.Style.SECONDARY);
        copyButton.setGraphic(TablerIcon.of(TablerIcons.COPY, 14));
        copyButton.setOnAction(e -> onCopyAll());
        copyButton.setDisable(true);

        toggleRow = panes.createToggleControls("Только «Территории»", "Только «Параметры»");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonsRow = new HBox(12, exportButton, copyButton, spacer, toggleRow);
        buttonsRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(title, topSection, contentHolder, buttonsRow);
        setTableMode(true);
        showPlaceholder();
    }

    private void setTableMode(boolean table) {
        this.tableMode = table;
        tableView.setVisible(table);
        tableView.setManaged(table);
        panes.setVisible(!table);
        panes.setManaged(!table);
        toggleRow.setVisible(!table);
        toggleRow.setManaged(!table);
        // Кнопка предлагает переключиться на ДРУГОЙ режим.
        modeButton.setText(table ? "Режим 2: карточки" : "Режим 1: таблица");
        modeButton.setGraphic(TablerIcon.of(table ? TablerIcons.LIST : TablerIcons.TABLE, 14));
    }

    private void buildTable() {
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        tableView.setPlaceholder(new Label("Данные появятся после загрузки"));
        tableView.getStyleClass().add("params-table");

        tableView.getColumns().add(textColumn("Муниципалитет", 260, ParamsTable.Row::territory));
        tableView.getColumns().add(textColumn("Год", 60, ParamsTable.Row::year));
        tableView.getColumns().add(textColumn("Группа сертификата", 150, ParamsTable.Row::groupName));

        String currentGroup = null;
        TableColumn<ParamsTable.Row, String> groupColumn = null;
        for (int i = 0; i < ParamsTable.COLUMNS.size(); i++) {
            ParamsTable.Column def = ParamsTable.COLUMNS.get(i);
            if (!def.group().equals(currentGroup)) {
                currentGroup = def.group();
                groupColumn = new TableColumn<>(currentGroup);
                tableView.getColumns().add(groupColumn);
            }
            final int index = i;
            groupColumn.getColumns().add(textColumn(def.label(), 140, r -> r.values().get(index)));
        }
    }

    private static TableColumn<ParamsTable.Row, String> textColumn(
            String title, double width, java.util.function.Function<ParamsTable.Row, String> getter) {
        TableColumn<ParamsTable.Row, String> col = new TableColumn<>(title);
        col.setPrefWidth(width);
        col.setCellValueFactory(cd -> new ReadOnlyStringWrapper(getter.apply(cd.getValue())));
        return col;
    }

    private VBox buildPane(String titleText, ScrollPane scroll) {
        Label header = new Label(titleText);
        header.getStyleClass().add("filter-section-title");
        VBox pane = new VBox(8, header, scroll);
        pane.getStyleClass().add("results-split-pane");
        pane.setPadding(new Insets(8));
        return pane;
    }

    public void setResult(ParamsAnalysisResult result, List<String> filterSummary) {
        this.currentResult = (result != null && result.isSuccess()) ? result : null;
        this.currentFilterSummary = filterSummary == null ? List.of() : filterSummary;
        exportButton.setDisable(currentResult == null);
        copyButton.setDisable(currentResult == null);

        if (currentResult == null) {
            showPlaceholder();
            return;
        }
        tableView.getItems().setAll(ParamsTable.from(currentResult).getRows());

        int active = currentFilterSummary.size();
        filtersButton.setText(active > 0 ? "Фильтры (" + active + ")" : "Фильтры");
        activeFiltersLabel.setText(active > 0
                ? "Фильтры: " + String.join("; ", currentFilterSummary)
                : "Фильтры не выбраны — показаны все данные");

        summaryLabel.setText("Территорий (с годом): " + currentResult.getTerritoryYears().size()
                + "  |  Карточек: " + currentResult.getTotalCards()
                + "  (муниципалитет: " + currentResult.getCards(ParamsPage.TERR).size()
                + ", группы: " + currentResult.getCards(ParamsPage.CERT_GROUP).size()
                + ", баланс: " + currentResult.getCards(ParamsPage.CERT_BALANCE).size() + ")"
                + (currentResult.isCancelled() ? "  —  остановлено, показаны частичные результаты" : ""));

        territoriesList.getChildren().clear();
        territoryRows.clear();
        for (ParamsAnalysisResult.TerritoryYear ty : currentResult.getTerritoryYears()) {
            VBox row = createTerritoryRow(ty);
            territoryRows.add(row);
            territoriesList.getChildren().add(row);
        }

        if (currentResult.getTerritoryYears().isEmpty()) {
            detailsList.getChildren().setAll(
                    SelectableText.field("По выбранным фильтрам данных нет", "results-institution-stats"));
        } else {
            select(0);
        }
    }

    private void showPlaceholder() {
        summaryLabel.setText("Данные появятся после загрузки");
        activeFiltersLabel.setText("");
        territoriesList.getChildren().clear();
        detailsList.getChildren().clear();
        territoryRows.clear();
        tableView.getItems().clear();
    }

    private VBox createTerritoryRow(ParamsAnalysisResult.TerritoryYear ty) {
        VBox row = new VBox();
        row.getStyleClass().add("results-institution-row");
        row.setSpacing(4);
        row.setPadding(new Insets(10, 12, 10, 12));

        row.getChildren().add(SelectableText.field(territoryTitle(ty), "results-institution-name"));
        row.getChildren().add(SelectableText.field(
                "Муниципалитет: " + countText(ty, ParamsPage.TERR)
                        + "   Группы сертификатов: " + countText(ty, ParamsPage.CERT_GROUP)
                        + "   Потенц. баланс: " + countText(ty, ParamsPage.CERT_BALANCE),
                "results-institution-stats"));

        int index = territoryRows.size();
        row.setOnMouseClicked(e -> select(index));
        return row;
    }

    private static String countText(ParamsAnalysisResult.TerritoryYear ty, ParamsPage page) {
        int count = ty.getCards(page).size();
        return count == 0 ? "нет" : String.valueOf(count);
    }

    private static String territoryTitle(ParamsAnalysisResult.TerritoryYear ty) {
        return ty.getYear().isEmpty() ? ty.getTerritory() : ty.getTerritory() + "  —  " + ty.getYear();
    }

    private void select(int index) {
        for (int i = 0; i < territoryRows.size(); i++) {
            territoryRows.get(i).setStyle(i == index ? SELECTED_STYLE : "");
        }
        ParamsAnalysisResult.TerritoryYear ty = currentResult.getTerritoryYears().get(index);
        detailsList.getChildren().clear();

        Label header = new Label(territoryTitle(ty));
        header.getStyleClass().add("results-title");
        detailsList.getChildren().add(header);

        for (ParamsPage page : ParamsPage.values()) {
            Label pageHeader = new Label(page.getTitle());
            pageHeader.getStyleClass().add("filter-section-title");
            detailsList.getChildren().add(pageHeader);

            List<ParamsCard> cards = ty.getCards(page);
            if (cards.isEmpty()) {
                detailsList.getChildren().add(SelectableText.field("Нет данных", "results-institution-stats"));
                continue;
            }
            for (ParamsCard card : cards) {
                detailsList.getChildren().add(createCardNode(card));
            }
        }
    }

    private Node createCardNode(ParamsCard card) {
        VBox box = new VBox();
        box.getStyleClass().add("results-institution-row");
        box.setSpacing(4);
        box.setPadding(new Insets(10, 12, 10, 12));

        String name = card.getGroupName().map(g -> "Группа сертификата: " + g).orElse(card.getTitle());
        VBox texts = new VBox(4);
        texts.getChildren().add(SelectableText.field(name, "results-institution-name"));
        texts.getChildren().add(SelectableText.field("Статус: " + card.getStatusText(), "results-institution-stats"));
        HBox top = new HBox(12, texts);
        HBox.setHgrow(texts, Priority.ALWAYS);
        top.setAlignment(Pos.CENTER_LEFT);
        card.getUrl().ifPresent(url -> {
            ActionButton open = new ActionButton("Открыть", ActionButton.Style.SECONDARY);
            open.setGraphic(TablerIcon.of(TablerIcons.EXTERNAL_LINK, 12));
            open.setOnAction(e -> openInBrowser(absoluteUrl(url)));
            top.getChildren().add(open);
        });
        box.getChildren().add(top);

        for (ParamsCard.Section section : card.getSections()) {
            if (!section.title().isEmpty()) {
                box.getChildren().add(SelectableText.field(section.title(), "results-institution-name"));
            }
            StringBuilder sb = new StringBuilder();
            for (ParamsCard.Row row : section.rows()) {
                if (sb.length() > 0) {
                    sb.append(System.lineSeparator());
                }
                sb.append(row.label()).append(": ").append(row.value().isEmpty() ? "—" : row.value());
            }
            box.getChildren().add(SelectableText.area(sb.toString(), "results-institution-stats"));
        }
        return box;
    }

    private static String absoluteUrl(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        return ServerConfig.BASE_URL + (url.startsWith("/") ? url : "/" + url);
    }

    private void openInBrowser(String url) {
        Thread thread = new Thread(() -> {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } catch (Exception e) {
                logger.error("Не удалось открыть ссылку ({}): {}", url, e.getMessage());
                Platform.runLater(() -> showInfoAlert("Ошибка", "Не удалось открыть ссылку в браузере:\n" + e.getMessage()));
            }
        }, "params-link-opener");
        thread.setDaemon(true);
        thread.start();
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
        fileChooser.setInitialFileName("RMC_параметры_" + timestamp + ".xlsx");

        Window window = getScene() != null ? getScene().getWindow() : null;
        File file = fileChooser.showSaveDialog(window);
        if (file == null) {
            return;
        }
        try {
            ParamsExportService.export(currentResult, file);
            showInfoAlert("Экспорт завершён", "Файл сохранён:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Ошибка экспорта параметров: {}", e.getMessage());
            showInfoAlert("Ошибка экспорта", "Не удалось сохранить файл:\n" + e.getMessage());
        }
    }

    private void onCopyAll() {
        if (currentResult == null) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(buildCopyText());
        Clipboard.getSystemClipboard().setContent(content);
        showInfoAlert("Скопировано", "Вся информация со страницы скопирована в буфер обмена.");
    }

    private String buildCopyText() {
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder();
        sb.append("Параметры РМЦ").append(nl);
        sb.append(summaryLabel.getText()).append(nl);
        sb.append(activeFiltersLabel.getText()).append(nl).append(nl);

        for (ParamsAnalysisResult.TerritoryYear ty : currentResult.getTerritoryYears()) {
            sb.append("== ").append(territoryTitle(ty)).append(" ==").append(nl);
            for (ParamsPage page : ParamsPage.values()) {
                sb.append(page.getTitle()).append(":").append(nl);
                List<ParamsCard> cards = ty.getCards(page);
                if (cards.isEmpty()) {
                    sb.append("  нет данных").append(nl);
                }
                for (ParamsCard card : cards) {
                    sb.append("  ").append(card.getGroupName().map(g -> "Группа: " + g).orElse(card.getTitle()))
                            .append(" [").append(card.getStatusText()).append("]").append(nl);
                    for (ParamsCard.Section section : card.getSections()) {
                        if (!section.title().isEmpty()) {
                            sb.append("    ").append(section.title()).append(nl);
                        }
                        for (ParamsCard.Row row : section.rows()) {
                            sb.append("      ").append(row.label()).append(": ").append(row.value()).append(nl);
                        }
                    }
                }
            }
            sb.append(nl);
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
        // Результаты остаются видимыми до следующей загрузки — не сбрасываем.
    }
}
