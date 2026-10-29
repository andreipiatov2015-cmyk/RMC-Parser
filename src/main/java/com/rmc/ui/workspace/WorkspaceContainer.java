package com.rmc.ui.workspace;

import com.rmc.config.ServerConfig;
import com.rmc.filters.loader.FilterPageLoader;
import com.rmc.filters.loader.FilterPageResult;
import com.rmc.filters.model.FilterGroup;
import com.rmc.filters.parser.FilterParser;
import com.rmc.history.SearchHistoryService;
import com.rmc.params.ParamsAnalysisResult;
import com.rmc.params.ParamsAnalysisService;
import com.rmc.params.ParamsFilterMapper;
import com.rmc.params.ParamsPage;
import com.rmc.http.HttpClientService;
import com.rmc.search.model.AnalysisResult;
import com.rmc.search.model.CooperateAnalysisResult;
import com.rmc.search.service.CooperateAnalysisService;
import com.rmc.search.service.ProgramAnalysisService;
import com.rmc.search.service.ProgramSearchService;
import com.rmc.state.ApplicationState;
import com.rmc.ui.dashboard.Dashboard;
import com.rmc.ui.statusbar.StatusBar;
import com.rmc.ui.theme.ThemeService;
import com.rmc.ui.workspace.views.*;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Workspace container - manages state transitions and views.
 */
public class WorkspaceContainer extends StackPane {

    private WorkspaceState currentState = WorkspaceState.AUTH;
    private WorkspaceView currentView;
    
    // Views
    private final AccountPickerView accountPickerView;
    private final AuthView authView;
    private final LoadingView loadingView;
    private final FiltersView filtersView;
    private final ResultsView resultsView;
    private final ErrorView errorView;
    private final FavoritesView favoritesView;
    private final InstitutionDetailView institutionDetailView;
    private final RmcAiView rmcAiView;
    private final FiltersView cooperatesFiltersView;
    private final CooperatesResultsView cooperatesResultsView;
    private final FiltersView paramsFiltersView;
    private final ParamsView paramsView;

    // References
    private Dashboard dashboard;
    private StatusBar statusBar;

    // Previous state for retry
    private WorkspaceState previousState;

    // Фильтры "Соглашения" загружаются только один раз за сессию, при
    // первом переходе на этот раздел — повторный переход просто показывает
    // уже готовый экран, без повторного похода на сайт.
    private boolean cooperatesFiltersLoaded = false;

    // Раздел "Параметры РМЦ": общий набор фильтров трёх страниц сайта и
    // сопоставление его полей с полями каждой страницы. Загружаются один
    // раз за сессию, при первом переходе на раздел.
    private ParamsFilterMapper paramsMapper;
    private javafx.scene.control.Dialog<javafx.scene.control.ButtonType> paramsFiltersDialog;

    public WorkspaceContainer() {
        getStyleClass().add("workspace");
        setPadding(new Insets(16));
        
        // Create all views
        accountPickerView = new AccountPickerView(this);
        authView = new AuthView(this);
        loadingView = new LoadingView();
        filtersView = new FiltersView(this);
        resultsView = new ResultsView(this);
        errorView = new ErrorView(this);
        favoritesView = new FavoritesView(this);
        institutionDetailView = new InstitutionDetailView(this);
        rmcAiView = new RmcAiView(this);
        cooperatesFiltersView = new FiltersView("Параметры поиска — Соглашения", this::runCooperatesAnalysis);
        cooperatesResultsView = new CooperatesResultsView(this);
        paramsFiltersView = new FiltersView("Фильтры — Параметры РМЦ", "Применить", (query, summary) -> applyParamsFilters());
        paramsView = new ParamsView(this);

        // Начинаем с выбора аккаунта, если есть сохранённые, иначе — с формы входа
        if (AccountPickerView.hasSavedAccounts()) {
            currentState = WorkspaceState.ACCOUNT_PICKER;
            currentView = accountPickerView;
            accountPickerView.refresh();
            getChildren().add(accountPickerView);
        } else {
            currentState = WorkspaceState.AUTH;
            currentView = authView;
            getChildren().add(authView);
        }
    }
    
    public void setReferences(Dashboard dashboard, StatusBar statusBar) {
        this.dashboard = dashboard;
        this.statusBar = statusBar;
    }
    
    public void transitionTo(WorkspaceState newState) {
        transitionTo(newState, (Runnable) null);
    }
    
    public void transitionTo(WorkspaceState newState, Object data) {
        transitionTo(newState, () -> {
            // Handle data based on state — выполняется ПОСЛЕ onEnter(),
            // иначе onEnter() (который может сбрасывать поля вида) выполнится
            // асинхронно позже и затрёт то, что мы здесь устанавливаем.
            if (newState == WorkspaceState.ERROR && data instanceof String error) {
                ((ErrorView) currentView).setError(error);
            }
        });
    }
    
    private void transitionTo(WorkspaceState newState, Runnable afterShown) {
        if (newState == currentState) {
            if (afterShown != null) {
                afterShown.run();
            }
            return;
        }
        
        previousState = currentState;
        currentState = newState;
        
        WorkspaceView newView = getViewForState(newState);
        
        // Fade transition
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), currentView.getRoot());
        fadeOut.setToValue(0);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), newView.getRoot());
        fadeIn.setToValue(1);
        
        WorkspaceView previousView = currentView;
        fadeOut.setOnFinished(e -> {
            previousView.getRoot().setVisible(false);
            previousView.onExit();
            if (!getChildren().contains(newView.getRoot())) {
                getChildren().add(newView.getRoot());
            }
            newView.getRoot().setVisible(true);
            newView.onEnter();
            if (afterShown != null) {
                afterShown.run();
            }
            fadeIn.play();
        });
        
        fadeOut.play();
        currentView = newView;
    }
    
    private WorkspaceView getViewForState(WorkspaceState state) {
        return switch (state) {
            case ACCOUNT_PICKER -> accountPickerView;
            case AUTH -> authView;
            case LOADING_FILTERS -> loadingView;
            case FILTERS_READY -> filtersView;
            case ANALYZING -> loadingView;
            case RESULTS -> resultsView;
            case FAVORITES -> favoritesView;
            case INSTITUTION_DETAIL -> institutionDetailView;
            case RMC_AI -> rmcAiView;
            case COOPERATES_LOADING_FILTERS -> loadingView;
            case COOPERATES_FILTERS_READY -> cooperatesFiltersView;
            case COOPERATES_ANALYZING -> loadingView;
            case COOPERATES_RESULTS -> cooperatesResultsView;
            case PARAMS_ANALYZING -> loadingView;
            case PARAMS_READY -> paramsView;
            case ERROR -> errorView;
        };
    }
    
    /**
     * @return экран фильтров — используется, например, {@link FavoritesView}
     * для доступа к уже загруженному списку учреждений через
     * {@link FiltersView#getFilterSession()}.
     */
    public FiltersView getFiltersView() {
        return filtersView;
    }
    
    /**
     * Перейти на экран "Избранные учреждения".
     */
    public void showFavorites() {
        transitionTo(WorkspaceState.FAVORITES);
    }
    
    /**
     * Перейти на экран "RMCAI" — чат с нейросетью по данным сайта.
     */
    public void showRmcAi() {
        transitionTo(WorkspaceState.RMC_AI);
    }
    
    /**
     * Открыть подробную информацию по избранному учреждению — отдельный
     * экран внутри программы (не отдельное окно), в отличие от диалога
     * добавления учреждения в избранное, который так и остаётся отдельным
     * окном по своей природе (это разовое действие, а не то, что нужно
     * рассматривать долго).
     */
    public void showInstitutionDetail(com.rmc.favorites.FavoriteInstitution favorite) {
        transitionTo(WorkspaceState.INSTITUTION_DETAIL, () -> institutionDetailView.showInstitution(favorite));
    }
    
    public void onAuthStateChanged() {
        // Called when auth state changes
    }
    
    // State transition methods
    
    public void onLoginSuccess(String username) {
        transitionTo(WorkspaceState.LOADING_FILTERS);

        new Thread(() -> {
            try {
                // Используем тот же HttpClientService, в котором лежат cookies
                // (sessionid), полученные во время авторизации.
                HttpClientService httpClient = ApplicationState.getInstance().getHttpClient();

                FilterPageLoader loader = FilterPageLoader.builder()
                        .httpClient(httpClient)
                        .baseUrl(ServerConfig.BASE_URL)
                        .build();

                FilterPageResult pageResult = loader.load();

                if (!pageResult.isSuccess()) {
                    javafx.application.Platform.runLater(() ->
                            onFiltersLoadFailed(pageResult.getErrorMessage()
                                    .orElse("Не удалось загрузить страницу фильтров")));
                    return;
                }

                FilterParser.ParseResult parseResult = FilterParser.parse(pageResult.getHtml());

                if (!parseResult.isSuccess()) {
                    javafx.application.Platform.runLater(() ->
                            onFiltersLoadFailed(parseResult.getErrorMessage()
                                    .orElse("Не удалось разобрать фильтры")));
                    return;
                }

                List<FilterGroup> groups = parseResult.getGroups();

                javafx.application.Platform.runLater(() -> {
                    onFiltersLoaded(groups);
                    if (dashboard != null) {
                        dashboard.onAuthStateChanged();
                    }
                    if (statusBar != null) {
                        statusBar.setConnected(true);
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        onFiltersLoadFailed("Ошибка: " + e.getMessage()));
            }
        }).start();
    }
    
    public void onLoginFailure(String message) {
        transitionTo(WorkspaceState.ERROR,
                () -> ((ErrorView) currentView).setError("Ошибка входа: " + message));
    }
    
    public void onFiltersLoaded(List<FilterGroup> groups) {
        transitionTo(WorkspaceState.FILTERS_READY,
                () -> ((FiltersView) currentView).setFilters(groups));
    }
    
    public void onFiltersLoadFailed(String error) {
        transitionTo(WorkspaceState.ERROR,
                () -> ((ErrorView) currentView).setError("Ошибка загрузки фильтров: " + error));
    }
    
    public void onAnalysisStarted() {
        transitionTo(WorkspaceState.ANALYZING);
    }
    
    /**
     * Перейти на форму входа для повторной авторизации после "Выхода":
     * логин подставляется, пароль — всегда вводится заново. Сама учётная
     * запись при этом остаётся сохранённой (доступна через "Сменить
     * пользователя" / экран выбора аккаунта) — "Выход" её не удаляет.
     */
    public void promptReloginFor(String username) {
        transitionTo(WorkspaceState.AUTH, () -> authView.prefillUsername(username));
    }
    
    /**
     * Перейти на экран выбора учётной записи — используется кнопкой
     * "Сменить пользователя". Список карточек обновится сам — см.
     * {@link AccountPickerView#onEnter()}, вызываемый из {@link #transitionTo}.
     */
    public void showAccountPicker() {
        transitionTo(WorkspaceState.ACCOUNT_PICKER);
    }
    
    /**
     * Запускает полный цикл анализа по готовой query-строке фильтров.
     * Единая точка входа — используется и кнопкой "Начать анализ" в
     * {@link FiltersView}, и повтором прошлого запроса из истории поиска
     * на {@link Dashboard}.
     *
     * @param queryString   query-строка фильтров (без ведущего "?")
     * @param filterSummary человекочитаемая сводка применённых фильтров —
     *                      сохраняется в историю поиска вместе с итогом
     */
    public void runAnalysis(String queryString, List<String> filterSummary) {
        onAnalysisStarted();
        
        java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
        loadingView.setCancellable(true, () -> cancelled.set(true));
        loadingView.resetProgress();
        
        new Thread(() -> {
            try {
                HttpClientService httpClient = ApplicationState.getInstance().getHttpClient();
                ProgramSearchService searchService = ProgramSearchService.builder()
                        .httpClient(httpClient)
                        .build();
                ProgramAnalysisService analysisService = ProgramAnalysisService.builder()
                        .baseUrl(ServerConfig.BASE_URL)
                        .searchService(searchService)
                        .build();
                
                AnalysisResult result = analysisService.analyze(queryString,
                        (status, current, total) -> javafx.application.Platform.runLater(
                                () -> updateProgress(status, current, total)),
                        cancelled::get);
                
                javafx.application.Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        // Отменённый (частичный) результат в историю не пишем —
                        // цифры неполные, и "Повторить" на их основе введёт в заблуждение.
                        if (!result.isCancelled()) {
                            SearchHistoryService.add(queryString, filterSummary,
                                    result.getTotalPrograms(), result.getTotalInstitutions());
                            if (dashboard != null) {
                                dashboard.refreshHistory();
                            }
                        }
                        onAnalysisComplete(result);
                    } else {
                        onAnalysisFailed(result.getErrorMessage().orElse("Неизвестная ошибка анализа"));
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        onAnalysisFailed("Ошибка: " + e.getMessage()));
            }
        }).start();
    }
    
    /**
     * Обновить статус на экране загрузки во время анализа
     * (например, "Учреждение 3 из 12: МБОУ ..."). {@code current}/
     * {@code total} — для полосы прогресса; {@code total <= 0}, если
     * общее число шагов на этом этапе ещё не известно.
     */
    public void updateProgress(String status, int current, int total) {
        if (loadingView != null) {
            loadingView.setStatus(status);
            loadingView.setProgress(current, total);
        }
    }
    
    public void onAnalysisComplete(AnalysisResult result) {
        transitionTo(WorkspaceState.RESULTS,
                () -> ((ResultsView) currentView).setResult(result));
    }
    
    public void onAnalysisFailed(String error) {
        transitionTo(WorkspaceState.ERROR,
                () -> ((ErrorView) currentView).setError("Ошибка анализа: " + error));
    }
    
    public void onRetry() {
        if (previousState != null) {
            transitionTo(previousState);
        } else {
            transitionTo(WorkspaceState.AUTH);
        }
    }
    
    public void onLogout() {
        transitionTo(WorkspaceState.AUTH);
        if (dashboard != null) {
            dashboard.onAuthStateChanged();
        }
        if (statusBar != null) {
            statusBar.setConnected(false);
        }
    }
    
    public void onBackToFilters() {
        transitionTo(WorkspaceState.FILTERS_READY);
    }

    /**
     * Перейти на экран "Реестр программ" (используется кнопками "Назад к
     * фильтрам" на экранах раздела "Соглашения", и переключателем-пилюлей
     * в TopBar).
     */
    public void showRegistryHome() {
        transitionTo(WorkspaceState.FILTERS_READY);
    }

    /**
     * Перейти на раздел "Соглашения" (/cooperates/) — зеркало реестра
     * программ с собственными фильтрами. Фильтры загружаются с сайта
     * только при первом переходе за сессию; повторные переходы просто
     * показывают уже готовый экран.
     */
    public void showCooperatesSection() {
        if (cooperatesFiltersLoaded) {
            transitionTo(WorkspaceState.COOPERATES_FILTERS_READY);
            return;
        }

        transitionTo(WorkspaceState.COOPERATES_LOADING_FILTERS);

        new Thread(() -> {
            try {
                HttpClientService httpClient = ApplicationState.getInstance().getHttpClient();

                FilterPageLoader loader = FilterPageLoader.builder()
                        .httpClient(httpClient)
                        .baseUrl(ServerConfig.BASE_URL)
                        .path("cooperates/")
                        .build();

                FilterPageResult pageResult = loader.load();

                if (!pageResult.isSuccess()) {
                    javafx.application.Platform.runLater(() ->
                            onCooperatesFiltersLoadFailed(pageResult.getErrorMessage()
                                    .orElse("Не удалось загрузить страницу фильтров")));
                    return;
                }

                FilterParser.ParseResult parseResult = FilterParser.parse(pageResult.getHtml());

                if (!parseResult.isSuccess()) {
                    javafx.application.Platform.runLater(() ->
                            onCooperatesFiltersLoadFailed(parseResult.getErrorMessage()
                                    .orElse("Не удалось разобрать фильтры")));
                    return;
                }

                List<FilterGroup> groups = parseResult.getGroups();

                javafx.application.Platform.runLater(() -> onCooperatesFiltersLoaded(groups));

            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        onCooperatesFiltersLoadFailed("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    private void onCooperatesFiltersLoaded(List<FilterGroup> groups) {
        cooperatesFiltersLoaded = true;
        transitionTo(WorkspaceState.COOPERATES_FILTERS_READY,
                () -> cooperatesFiltersView.setFilters(groups));
    }

    private void onCooperatesFiltersLoadFailed(String error) {
        transitionTo(WorkspaceState.ERROR,
                () -> ((ErrorView) currentView).setError("Ошибка загрузки фильтров «Соглашения»: " + error));
    }

    /**
     * Запускает анализ раздела "Соглашения" по готовой query-строке
     * фильтров — аналог {@link #runAnalysis(String, List)} для реестра
     * программ, но проще: данные уже полностью есть в самой таблице
     * списка, без захода на отдельные страницы заявок.
     */
    public void runCooperatesAnalysis(String queryString, List<String> filterSummary) {
        transitionTo(WorkspaceState.COOPERATES_ANALYZING);

        java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
        loadingView.setCancellable(true, () -> cancelled.set(true));
        loadingView.resetProgress();

        new Thread(() -> {
            try {
                HttpClientService httpClient = ApplicationState.getInstance().getHttpClient();
                ProgramSearchService searchService = ProgramSearchService.builder()
                        .httpClient(httpClient)
                        .build();
                CooperateAnalysisService analysisService = CooperateAnalysisService.builder()
                        .baseUrl(ServerConfig.BASE_URL)
                        .searchService(searchService)
                        .build();

                CooperateAnalysisResult result = analysisService.analyze(queryString,
                        (status, current, total) -> javafx.application.Platform.runLater(
                                () -> updateProgress(status, current, total)),
                        cancelled::get);

                javafx.application.Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        onCooperatesAnalysisComplete(result);
                    } else {
                        onCooperatesAnalysisFailed(result.getErrorMessage().orElse("Неизвестная ошибка анализа"));
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        onCooperatesAnalysisFailed("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    private void onCooperatesAnalysisComplete(CooperateAnalysisResult result) {
        transitionTo(WorkspaceState.COOPERATES_RESULTS,
                () -> cooperatesResultsView.setResult(result));
    }

    private void onCooperatesAnalysisFailed(String error) {
        transitionTo(WorkspaceState.ERROR,
                () -> ((ErrorView) currentView).setError("Ошибка анализа «Соглашения»: " + error));
    }

    /**
     * Перейти на раздел "Параметры РМЦ": данные трёх страниц сайта
     * (параметры муниципалитета, групп сертификатов, потенциальный
     * баланс) по общему фильтру. При первом переходе за сессию загружает
     * формы фильтров всех трёх страниц и сразу показывает все данные без
     * фильтров; повторные переходы просто показывают готовый экран.
     */
    public void showParamsSection() {
        if (paramsMapper != null) {
            transitionTo(WorkspaceState.PARAMS_READY);
            return;
        }

        // Одно и то же состояние (и один и тот же экран загрузки) на всё
        // время первой загрузки — и фильтров, и самих данных.
        transitionTo(WorkspaceState.PARAMS_ANALYZING);
        loadingView.setCancellable(false, null);
        loadingView.resetProgress();
        loadingView.setStatus("Загрузка фильтров раздела «Параметры РМЦ»...");

        new Thread(() -> {
            try {
                HttpClientService httpClient = ApplicationState.getInstance().getHttpClient();
                Map<ParamsPage, List<FilterGroup>> groupsByPage = new EnumMap<>(ParamsPage.class);

                for (ParamsPage page : ParamsPage.values()) {
                    FilterPageResult pageResult = FilterPageLoader.builder()
                            .httpClient(httpClient)
                            .baseUrl(ServerConfig.BASE_URL)
                            .path(page.getPath())
                            .build()
                            .load();
                    if (!pageResult.isSuccess()) {
                        String message = "«" + page.getTitle() + "»: "
                                + pageResult.getErrorMessage().orElse("не удалось загрузить страницу фильтров");
                        javafx.application.Platform.runLater(() -> onParamsFailed(message));
                        return;
                    }
                    FilterParser.ParseResult parseResult = FilterParser.parse(pageResult.getHtml());
                    if (!parseResult.isSuccess()) {
                        String message = "«" + page.getTitle() + "»: "
                                + parseResult.getErrorMessage().orElse("не удалось разобрать фильтры");
                        javafx.application.Platform.runLater(() -> onParamsFailed(message));
                        return;
                    }
                    groupsByPage.put(page, parseResult.getGroups());
                }

                ParamsFilterMapper mapper = ParamsFilterMapper.build(groupsByPage);
                javafx.application.Platform.runLater(() -> {
                    paramsMapper = mapper;
                    paramsFiltersView.setFilters(mapper.getUnifiedGroups());
                    runParamsAnalysis(emptyParamsQueries(), List.of());
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> onParamsFailed("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    private static Map<ParamsPage, String> emptyParamsQueries() {
        Map<ParamsPage, String> queries = new EnumMap<>(ParamsPage.class);
        for (ParamsPage page : ParamsPage.values()) {
            queries.put(page, "");
        }
        return queries;
    }

    /**
     * Открывает окно с общими фильтрами раздела "Параметры РМЦ" (кнопка
     * "Фильтры" на экране раздела). Выбор действует сразу на все три
     * страницы сайта.
     */
    public void showParamsFilters() {
        if (paramsMapper == null || paramsFiltersDialog != null) {
            return;
        }
        javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Фильтры — Параметры РМЦ");
        if (getScene() != null) {
            dialog.initOwner(getScene().getWindow());
        }
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm());
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard-dark.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("app-dialog");
        if (ThemeService.isDarkMode()) {
            dialog.getDialogPane().getStyleClass().add("dark-theme");
        }
        dialog.getDialogPane().setPrefSize(760, 640);
        dialog.getDialogPane().setContent(paramsFiltersView);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.setOnHidden(e -> {
            dialog.getDialogPane().setContent(null);
            paramsFiltersDialog = null;
        });
        paramsFiltersDialog = dialog;
        dialog.show();
    }

    private void applyParamsFilters() {
        if (paramsMapper == null || paramsFiltersView.getFilterSession() == null) {
            return;
        }
        com.rmc.filters.session.FilterSession session = paramsFiltersView.getFilterSession();
        Map<ParamsPage, String> queries = paramsMapper.buildQueries(session);
        List<String> summary = session.getActiveFiltersSummary();
        if (paramsFiltersDialog != null) {
            paramsFiltersDialog.close();
        }
        runParamsAnalysis(queries, summary);
    }

    private void runParamsAnalysis(Map<ParamsPage, String> queries, List<String> filterSummary) {
        transitionTo(WorkspaceState.PARAMS_ANALYZING);

        java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
        loadingView.setCancellable(true, () -> cancelled.set(true));
        loadingView.resetProgress();
        loadingView.setStatus("Загрузка данных раздела «Параметры РМЦ»...");

        new Thread(() -> {
            try {
                HttpClientService httpClient = ApplicationState.getInstance().getHttpClient();
                ProgramSearchService searchService = ProgramSearchService.builder()
                        .httpClient(httpClient)
                        .build();
                ParamsAnalysisResult result = new ParamsAnalysisService(searchService).analyze(queries,
                        (status, current, total) -> javafx.application.Platform.runLater(
                                () -> updateProgress(status, current, total)),
                        cancelled::get);

                javafx.application.Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        transitionTo(WorkspaceState.PARAMS_READY,
                                () -> paramsView.setResult(result, filterSummary));
                    } else {
                        onParamsFailed(result.getErrorMessage().orElse("Неизвестная ошибка"));
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> onParamsFailed("Ошибка: " + e.getMessage()));
            }
        }).start();
    }

    private void onParamsFailed(String error) {
        transitionTo(WorkspaceState.ERROR,
                () -> ((ErrorView) currentView).setError("Ошибка раздела «Параметры РМЦ»: " + error));
    }

    public WorkspaceState getCurrentState() {
        return currentState;
    }
}
