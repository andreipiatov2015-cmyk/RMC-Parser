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
import com.rmc.ui.icons.TablerIcon;
import com.rmc.ui.icons.TablerIcons;
import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceView;
import com.rmc.ui.workspace.components.DonutChart;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Подробная информация по одному избранному учреждению — отдельный экран
 * внутри программы: общее "кольцо" зачисленных/вместимости по всем
 * программам сразу, сводные показатели учреждения (со страницы
 * {@code /org/{id}/}) карточками в один ряд, и список программ, у каждой —
 * своё маленькое кольцо и количество действующих групп.
 */
public class InstitutionDetailView extends VBox implements WorkspaceView {
    
    private static final Logger logger = AppLogger.getLogger();
    private static final String CAPACITY_LABEL = "Максимальное количество детей в группе";
    private static final double REFERENCE_ROW_WIDTH = 900;
    
    private final WorkspaceContainer container;
    private final Label titleLabel;
    
    private final VBox loadingBox;
    private final ProgressIndicator progressIndicator;
    private final Label loadingLabel;
    
    private final VBox contentBox;
    private final VBox summaryBox;
    private final HBox orgStatsRow;
    private final HBox totalsRow;
    private final VBox programsList;
    
    public InstitutionDetailView(WorkspaceContainer container) {
        this.container = container;
        
        getStyleClass().add("filters-view");
        setSpacing(14);
        setPadding(new Insets(20));
        
        Button backButton = new Button("Назад к избранному");
        backButton.setGraphic(TablerIcon.of(TablerIcons.ARROW_LEFT, 14));
        backButton.getStyleClass().add("action-button-secondary");
        backButton.setOnAction(e -> container.showFavorites());
        
        titleLabel = new Label();
        titleLabel.getStyleClass().add("filters-title");
        titleLabel.setWrapText(true);
        
        // Красивое состояние загрузки — крутящийся индикатор с живым
        // статусом ("Загрузка сводки...", "Программа 3 из 16...") вместо
        // простой надписи в углу.
        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(56, 56);
        progressIndicator.getStyleClass().add("institution-loading-spinner");
        
        Label loadingTitle = new Label("Считаем данные по учреждению");
        loadingTitle.getStyleClass().add("institution-loading-title");
        
        loadingLabel = new Label();
        loadingLabel.getStyleClass().add("filters-status");
        
        loadingBox = new VBox(10, progressIndicator, loadingTitle, loadingLabel);
        loadingBox.getStyleClass().add("institution-loading-box");
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setPadding(new Insets(50, 0, 0, 0));
        
        // Большое кольцо — общая статистика в процентах, показывается
        // ПЕРВЫМ (над карточками показателей).
        summaryBox = new VBox();
        summaryBox.setAlignment(Pos.CENTER_LEFT);
        
        // Сводные показатели учреждения (со страницы /org/{id}/) —
        // карточки в один ряд, растут вместе с шириной окна.
        orgStatsRow = new HBox(10);
        orgStatsRow.getStyleClass().add("institution-summary-row");
        
        // "Всего программ / Групп / Вместимость / Зачислено" — тоже
        // карточками, тем же стилем, что и сводка учреждения выше.
        totalsRow = new HBox(10);
        totalsRow.getStyleClass().add("institution-summary-row");
        
        programsList = new VBox(10);
        
        ScrollPane scrollPane = new ScrollPane(programsList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        contentBox = new VBox(14, summaryBox, orgStatsRow, totalsRow, scrollPane);
        VBox.setVgrow(contentBox, Priority.ALWAYS);
        contentBox.setVisible(false);
        contentBox.setManaged(false);
        
        getChildren().addAll(backButton, titleLabel, loadingBox, contentBox);
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
        
        loadingLabel.setText("Подготовка запроса...");
        progressIndicator.setVisible(true);
        loadingBox.setVisible(true);
        loadingBox.setManaged(true);
        contentBox.setVisible(false);
        contentBox.setManaged(false);
        
        orgStatsRow.getChildren().clear();
        summaryBox.getChildren().clear();
        totalsRow.getChildren().clear();
        programsList.getChildren().clear();
        
        new Thread(() -> {
            HttpClientService httpClient = ApplicationState.getInstance().getHttpClient();
            ProgramSearchService searchService = ProgramSearchService.builder()
                    .httpClient(httpClient)
                    .build();
            
            Platform.runLater(() -> loadingLabel.setText("Загрузка сводки учреждения..."));
            Map<String, Integer> orgStats = null;
            try {
                SearchResult orgPage = searchService.search(ServerConfig.BASE_URL + "/org/" + favorite.getId() + "/");
                if (orgPage.isSuccess()) {
                    OrganizationStatsParser.ParseResult stats = OrganizationStatsParser.parse(orgPage.getHtml());
                    if (stats.isSuccess()) {
                        orgStats = stats.getStats();
                    }
                }
            } catch (Exception e) {
                logger.warn("Не удалось загрузить сводку учреждения {}: {}", favorite.getId(), e.getMessage());
            }
            
            Map<String, Integer> finalOrgStats = orgStats;
            
            try {
                InstitutionProgramsService programsService = InstitutionProgramsService.builder()
                        .searchService(searchService)
                        .baseUrl(ServerConfig.BASE_URL)
                        .build();
                
                List<ProgramDetail> details = programsService.loadPrograms(favorite.getId(),
                        message -> Platform.runLater(() -> loadingLabel.setText(message)));
                
                Platform.runLater(() -> {
                    renderOrgStats(finalOrgStats);
                    renderPrograms(details);
                    showContent();
                });
            } catch (Exception e) {
                logger.error("Ошибка загрузки программ учреждения {}: {}", favorite.getId(), e.getMessage());
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    loadingLabel.setText("Ошибка: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void showContent() {
        loadingBox.setVisible(false);
        loadingBox.setManaged(false);
        contentBox.setVisible(true);
        contentBox.setManaged(true);
    }
    
    private void renderOrgStats(Map<String, Integer> stats) {
        orgStatsRow.getChildren().clear();
        if (stats == null || stats.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            orgStatsRow.getChildren().add(createSummaryCard(orgStatsRow, entry.getKey(), entry.getValue()));
        }
    }
    
    /**
     * Карточка показателя — растёт вместе с остальными в ряду, а размер
     * шрифта числа/подписи плавно увеличивается вместе с шириной ряда
     * (то есть вместе с окном программы), а не остаётся фиксированным.
     */
    private VBox createSummaryCard(HBox row, String label, int value) {
        VBox card = new VBox(2);
        card.getStyleClass().add("institution-summary-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10, 6, 10, 6));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        
        Label valueLabel = new Label(String.valueOf(value));
        valueLabel.getStyleClass().add("institution-summary-value");
        
        Label textLabel = new Label(label);
        textLabel.getStyleClass().add("institution-summary-label");
        textLabel.setWrapText(true);
        textLabel.setAlignment(Pos.CENTER);
        textLabel.setStyle("-fx-text-alignment: center;");
        
        bindResponsiveFontSize(row, valueLabel, 20, 12, 30);
        bindResponsiveFontSize(row, textLabel, 11, 9, 14);
        
        card.getChildren().addAll(valueLabel, textLabel);
        return card;
    }
    
    /**
     * Привязывает размер шрифта надписи к ширине указанного ряда карточек
     * — растёт пропорционально по мере расширения окна программы, в
     * заданных пределах (чтобы не стать нечитаемо маленьким/огромным).
     */
    private void bindResponsiveFontSize(HBox row, Label label, double baseSize, double minSize, double maxSize) {
        label.styleProperty().bind(Bindings.createStringBinding(() -> {
            double width = row.getWidth();
            double scale = width > 0 ? width / REFERENCE_ROW_WIDTH : 1.0;
            double size = Math.max(minSize, Math.min(maxSize, baseSize * scale));
            return String.format("-fx-font-size: %.1fpx;", size);
        }, row.widthProperty()));
    }
    
    private void renderPrograms(List<ProgramDetail> details) {
        programsList.getChildren().clear();
        totalsRow.getChildren().clear();
        
        if (details.isEmpty()) {
            Label emptyLabel = new Label("У этого учреждения не найдено ни одной программы.");
            emptyLabel.getStyleClass().add("filters-status");
            programsList.getChildren().add(emptyLabel);
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
        
        totalsRow.getChildren().addAll(
                createSummaryCard(totalsRow, "Всего программ", details.size()),
                createSummaryCard(totalsRow, "Групп", totalGroups),
                createSummaryCard(totalsRow, "Вместимость", totalCapacity),
                createSummaryCard(totalsRow, "Зачислено", totalEnrolled)
        );
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
            Label statsLabel = new Label("Действующих групп: " + groups
                    + "   |   Вместимость: " + capacity
                    + "   |   Зачислено: " + enrolled);
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
