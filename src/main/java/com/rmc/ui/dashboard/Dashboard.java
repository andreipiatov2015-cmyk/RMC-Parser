package com.rmc.ui.dashboard;

import com.rmc.history.SearchHistoryService;
import com.rmc.history.model.SearchHistoryEntry;
import com.rmc.state.ApplicationState;
import com.rmc.ui.workspace.WorkspaceContainer;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Left panel with info cards and search history - always visible.
 */
public class Dashboard extends VBox {
    
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    
    private final InfoCard userCard;
    private final InfoCard serverCard;
    private final VBox historyList;
    private WorkspaceContainer workspace;
    
    public Dashboard() {
        getStyleClass().add("dashboard");
        setSpacing(10);
        setPadding(new Insets(12, 16, 12, 12));
        setPrefWidth(260);
        
        // Title
        Label title = new Label("Обзор");
        title.getStyleClass().add("dashboard-title");
        
        // User card
        userCard = new InfoCard("Пользователь", "—");
        
        // Server card
        serverCard = new InfoCard("Сервер", "Офлайн");
        serverCard.setSubtitle("Не подключено");
        
        // История поиска
        Label historyTitle = new Label("История поиска");
        historyTitle.getStyleClass().add("dashboard-section-title");
        
        historyList = new VBox();
        historyList.setSpacing(8);
        
        ScrollPane historyScroll = new ScrollPane(historyList);
        historyScroll.setFitToWidth(true);
        historyScroll.getStyleClass().add("history-scroll");
        VBox.setVgrow(historyScroll, Priority.ALWAYS);
        
        getChildren().addAll(
            title,
            userCard,
            serverCard,
            historyTitle,
            historyScroll
        );
        
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
        ApplicationState state = ApplicationState.getInstance();
        
        if (state.isAuthenticated()) {
            userCard.setValue(state.getUsername());
            userCard.setSubtitle("Авторизован");
            serverCard.setValue("rmc.ruobr.ru");
            serverCard.setSubtitle("Онлайн");
        } else {
            userCard.setValue("—");
            userCard.setSubtitle("Не авторизован");
            serverCard.setValue("—");
            serverCard.setSubtitle("Офлайн");
        }
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
