package com.rmc.ui.dashboard;

import com.rmc.history.SearchHistoryService;
import com.rmc.history.model.SearchHistoryEntry;
import com.rmc.ui.workspace.WorkspaceContainer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Левая панель — только история поиска (карточки "Пользователь" и
 * "Сервер" убраны: эта информация теперь показывается один раз, в блоке
 * аватара в {@code TopBar}, и дублировать её здесь незачем).
 *
 * <p>Панель сворачивается в узкую полоску с одной кнопкой ("выдвинуть")
 * и разворачивается обратно кнопкой в заголовке ("свернуть") — чисто
 * вручную, без автоматики: раньше была идея сворачивать её автоматически
 * при переходе к результатам (плюс кнопка "закрепить", чтобы это
 * отключить), но на практике не давала предсказуемого результата —
 * оставлено только ручное управление.</p>
 */
public class Dashboard extends VBox {
    
    private static final double EXPANDED_WIDTH = 260;
    private static final double COLLAPSED_WIDTH = 44;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    
    private final VBox collapsedRail;
    private final VBox expandedContent;
    private final VBox historyList;
    private WorkspaceContainer workspace;
    
    private boolean expanded = true;
    
    public Dashboard() {
        getStyleClass().add("dashboard");
        setPadding(new Insets(0));
        setPrefWidth(EXPANDED_WIDTH);
        
        // --- Свёрнутое состояние: одна кнопка "выдвинуть" ---
        Label expandButton = new Label("▸");
        expandButton.getStyleClass().add("dashboard-rail-button");
        Tooltip.install(expandButton, new Tooltip("Показать историю поиска"));
        expandButton.setOnMouseClicked(e -> expand());
        
        collapsedRail = new VBox(expandButton);
        collapsedRail.setAlignment(Pos.TOP_CENTER);
        collapsedRail.setPadding(new Insets(12, 0, 0, 0));
        collapsedRail.getStyleClass().add("dashboard-rail");
        
        // --- Развёрнутое состояние: заголовок с кнопкой + история ---
        Label historyTitle = new Label("История поиска");
        historyTitle.getStyleClass().add("dashboard-section-title");
        HBox.setHgrow(historyTitle, Priority.ALWAYS);
        
        Label collapseButton = new Label("◂");
        collapseButton.getStyleClass().add("dashboard-header-button");
        Tooltip.install(collapseButton, new Tooltip("Свернуть"));
        collapseButton.setOnMouseClicked(e -> collapse());
        
        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        
        HBox header = new HBox(4, historyTitle, collapseButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 12, 8, 16));
        
        historyList = new VBox();
        historyList.setSpacing(8);
        
        ScrollPane historyScroll = new ScrollPane(historyList);
        historyScroll.setFitToWidth(true);
        historyScroll.getStyleClass().add("history-scroll");
        VBox.setVgrow(historyScroll, Priority.ALWAYS);
        
        expandedContent = new VBox(header, historyScroll);
        VBox.setVgrow(expandedContent, Priority.ALWAYS);
        expandedContent.setPadding(new Insets(0, 4, 12, 4));
        
        getChildren().addAll(collapsedRail, expandedContent);
        applyState();
        
        refreshHistory();
    }
    
    /**
     * Связывает панель с рабочей областью — нужно для повтора запроса
     * из истории поиска (запускает анализ напрямую, минуя экран фильтров).
     */
    public void setWorkspace(WorkspaceContainer workspace) {
        this.workspace = workspace;
    }
    
    public void onAuthStateChanged() {
        // Карточек "Пользователь"/"Сервер" здесь больше нет — эта
        // информация живёт в TopBar. Метод оставлен, чтобы не трогать
        // вызывающий код (MainWindow), на случай если сюда позже
        // вернётся что-то, зависящее от авторизации.
    }
    
    private void expand() {
        expanded = true;
        applyState();
    }
    
    private void collapse() {
        expanded = false;
        applyState();
    }
    
    private void applyState() {
        collapsedRail.setVisible(!expanded);
        collapsedRail.setManaged(!expanded);
        expandedContent.setVisible(expanded);
        expandedContent.setManaged(expanded);
        
        double width = expanded ? EXPANDED_WIDTH : COLLAPSED_WIDTH;
        setPrefWidth(width);
        setMinWidth(width);
        setMaxWidth(width);
    }
    
    /**
     * Перечитывает историю поиска с диска и перерисовывает список.
     * Вызывается после каждого успешно завершённого анализа.
     */
    public void refreshHistory() {
        List<SearchHistoryEntry> entries = SearchHistoryService.loadAll();
        historyList.getChildren().clear();
        
        if (entries.isEmpty()) {
            Label emptyLabel = new Label("Пока пусто — здесь появятся прошлые запросы");
            emptyLabel.getStyleClass().add("history-empty-label");
            emptyLabel.setWrapText(true);
            historyList.getChildren().add(emptyLabel);
            return;
        }
        
        for (SearchHistoryEntry entry : entries) {
            historyList.getChildren().add(createHistoryItem(entry));
        }
    }
    
    private Node createHistoryItem(SearchHistoryEntry entry) {
        VBox card = new VBox();
        card.getStyleClass().add("history-item");
        card.setSpacing(4);
        card.setPadding(new Insets(10));
        
        Label timeLabel = new Label(entry.getTimestamp().format(TIME_FORMAT));
        timeLabel.getStyleClass().add("history-item-time");
        
        String summaryText = entry.getFilterSummary().isEmpty()
                ? "Без фильтров"
                : String.join("; ", entry.getFilterSummary());
        Label summaryLabel = new Label(summaryText);
        summaryLabel.getStyleClass().add("history-item-summary");
        summaryLabel.setWrapText(true);
        
        StringBuilder resultText = new StringBuilder();
        if (entry.getTotalPrograms() != null) {
            resultText.append("Программ: ").append(entry.getTotalPrograms());
        }
        if (entry.getTotalInstitutions() != null) {
            if (resultText.length() > 0) {
                resultText.append("   ");
            }
            resultText.append("Учреждений: ").append(entry.getTotalInstitutions());
        }
        Label resultLabel = new Label(resultText.toString());
        resultLabel.getStyleClass().add("history-item-result");
        
        Button repeatButton = new Button("↻ Повторить");
        repeatButton.getStyleClass().add("history-item-repeat");
        repeatButton.setMaxWidth(Double.MAX_VALUE);
        repeatButton.setOnAction(e -> {
            if (workspace != null) {
                workspace.runAnalysis(entry.getQueryString(), entry.getFilterSummary());
            }
        });
        
        card.getChildren().addAll(timeLabel, summaryLabel, resultLabel, repeatButton);
        return card;
    }
}
