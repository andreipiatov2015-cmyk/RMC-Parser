package com.rmc.ui.workspace.views;

import com.rmc.config.ServerConfig;
import com.rmc.favorites.FavoriteInstitution;
import com.rmc.http.HttpClientService;
import com.rmc.logging.AppLogger;
import com.rmc.parser.OrganizationStatsParser;
import com.rmc.parser.model.ProgramDetail;
import com.rmc.parser.model.ProgramGroup;
import com.rmc.search.service.InstitutionProgramsService;
import com.rmc.search.service.ProgramSearchService;
import com.rmc.search.service.SearchResult;
import com.rmc.state.ApplicationState;
import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceView;
import com.rmc.ui.workspace.components.DonutChart;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Подробная информация по одному избранному учреждению — отдельный экран
 * внутри программы (не отдельное окно): сводные показатели учреждения
 * (со страницы {@code /org/{id}/}), общее "кольцо" зачисленных/вместимости
 * по всем программам сразу, и список программ, у каждой — своё маленькое
 * кольцо и количество действующих групп.
 */
public class InstitutionDetailView extends VBox implements WorkspaceView {
    
    private static final Logger logger = AppLogger.getLogger();
    private static final String CAPACITY_LABEL = "Максимальное количество детей в группе";
    
    private final WorkspaceContainer container;
    private final Label titleLabel;
    private final Label statusLabel;
    private final FlowPane orgStatsRow;
    private final VBox summaryBox;
    private final VBox programsList;
    
    public InstitutionDetailView(WorkspaceContainer container) {
        this.container = container;
        
        getStyleClass().add("filters-view");
        setSpacing(14);
        setPadding(new Insets(20));
        
        Button backButton = new Button("← Назад к избранному");
        backButton.getStyleClass().add("action-button-secondary");
        backButton.setOnAction(e -> container.showFavorites());
        
        titleLabel = new Label();
        titleLabel.getStyleClass().add("filters-title");
        titleLabel.setWrapText(true);
        
        statusLabel = new Label();
        statusLabel.getStyleClass().add("filters-status");
        
        // Сводные показатели учреждения (со страницы /org/{id}/) — компактно,
        // в одну строку, мельче обычных карточек.
        orgStatsRow = new FlowPane();
        orgStatsRow.getStyleClass().add("institution-summary-row");
        orgStatsRow.setHgap(8);
        orgStatsRow.setVgap(8);
        
        // Большое кольцо: зачислено/вместимость по учреждению целиком.
        summaryBox = new VBox();
        summaryBox.setAlignment(Pos.CENTER_LEFT);
        
        programsList = new VBox(10);
        
        ScrollPane scrollPane = new ScrollPane(programsList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        getChildren().addAll(backButton, titleLabel, statusLabel, orgStatsRow, summaryBox, scrollPane);
    }
    
    @Override
    public Pane getRoot() {
        return this;
    }
    
    /**
     * Запустить загрузку и показ данных по учреждению — вызывается сразу
     * после перехода на этот экран (см. {@code WorkspaceContainer.showInstitutionDetail}).
     */
    public void showInstitution(FavoriteInstitution favorite) {
        titleLabel.setText(favorite.getName());
        statusLabel.setText("Загрузка данных учреждения...");
        orgStatsRow.getChildren().clear();
        summaryBox.getChildren().clear();
        programsList.getChildren().clear();
        
        new Thread(() -> {
            HttpClientService httpClient = ApplicationState.getInstance().getHttpClient();
            ProgramSearchService searchService = ProgramSearchService.builder()
                    .httpClient(httpClient)
                    .build();
            
            // Сводка по учреждению - один быстрый запрос, показываем сразу,
            // не дожидаясь обхода всех программ.
            try {
                SearchResult orgPage = searchService.search(ServerConfig.BASE_URL + "/org/" + favorite.getId() + "/");
                if (orgPage.isSuccess()) {
                    OrganizationStatsParser.ParseResult orgStats = OrganizationStatsParser.parse(orgPage.getHtml());
                    if (orgStats.isSuccess()) {
                        Platform.runLater(() -> renderOrgStats(orgStats.getStats()));
                    }
                }
            } catch (Exception e) {
                logger.warn("Не удалось загрузить сводку учреждения {}: {}", favorite.getId(), e.getMessage());
            }
            
            try {
                InstitutionProgramsService programsService = InstitutionProgramsService.builder()
                        .searchService(searchService)
                        .baseUrl(ServerConfig.BASE_URL)
                        .build();
                
                List<ProgramDetail> details = programsService.loadPrograms(favorite.getId(),
                        message -> Platform.runLater(() -> statusLabel.setText(message)));
                
                Platform.runLater(() -> renderPrograms(details));
            } catch (Exception e) {
                logger.error("Ошибка загрузки программ учреждения {}: {}", favorite.getId(), e.getMessage());
                Platform.runLater(() -> statusLabel.setText("Ошибка: " + e.getMessage()));
            }
        }).start();
    }
    
    private void renderOrgStats(Map<String, Integer> stats) {
        orgStatsRow.getChildren().clear();
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            Label item = new Label(entry.getKey() + ": " + entry.getValue());
            item.getStyleClass().add("institution-summary-item");
            orgStatsRow.getChildren().add(item);
        }
    }
    
    private void renderPrograms(List<ProgramDetail> details) {
        programsList.getChildren().clear();
        
        if (details.isEmpty()) {
            statusLabel.setText("У этого учреждения не найдено ни одной программы.");
            summaryBox.getChildren().clear();
            return;
        }
        
        int totalGroups = 0;
        int totalEnrolled = 0;
        int totalCapacity = 0;
        
        for (ProgramDetail detail : details) {
            int programCapacity = totalCapacity(detail);
            int programEnrolled = detail.getTotalCurrentlyEnrolled();
            
            if (detail.isSuccess()) {
                totalGroups += detail.getActiveGroupsCount().orElse(0);
                totalEnrolled += programEnrolled;
                totalCapacity += programCapacity;
            }
            
            programsList.getChildren().add(createProgramCard(detail, programCapacity, programEnrolled));
        }
        
        // Большое кольцо — вместимость/зачислено по учреждению целиком.
        summaryBox.getChildren().setAll(new DonutChart(160, 14, totalEnrolled, totalCapacity));
        
        statusLabel.setText("Всего программ: " + details.size()
                + "   |   Групп: " + totalGroups
                + "   |   Вместимость: " + totalCapacity
                + "   |   Зачислено: " + totalEnrolled);
    }
    
    private Pane createProgramCard(ProgramDetail detail, int capacity, int enrolled) {
        HBox card = new HBox(14);
        card.getStyleClass().add("filter-card");
        card.setPadding(new Insets(10));
        card.setAlignment(Pos.CENTER_LEFT);
        
        DonutChart donut = new DonutChart(60, 6, enrolled, capacity);
        
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        
        Label programTitle = new Label(detail.getTitle());
        programTitle.getStyleClass().add("results-institution-name");
        programTitle.setWrapText(true);
        info.getChildren().add(programTitle);
        
        if (!detail.isSuccess()) {
            Label errorLabel = new Label("Не удалось загрузить: "
                    + detail.getErrorMessage().orElse("неизвестная ошибка"));
            errorLabel.getStyleClass().add("results-institution-stats");
            info.getChildren().add(errorLabel);
        } else {
            int groups = detail.getActiveGroupsCount().orElse(0);
            Label statsLabel = new Label(
                    "Действующих групп: " + groups + "   |   Вместимость: " + capacity);
            statsLabel.getStyleClass().add("results-institution-stats");
            info.getChildren().add(statsLabel);
        }
        
        card.getChildren().addAll(donut, info);
        return card;
    }
    
    /**
     * Суммарная вместимость программы — сумма "Максимальное количество
     * детей в группе" по всем её действующим группам.
     */
    private int totalCapacity(ProgramDetail detail) {
        int total = 0;
        for (ProgramGroup group : detail.getGroups()) {
            total += group.getDetailAsInt(CAPACITY_LABEL).orElse(0);
        }
        return total;
    }
}
