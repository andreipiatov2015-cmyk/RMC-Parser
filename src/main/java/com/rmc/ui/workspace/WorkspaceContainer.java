package com.rmc.ui.workspace;

import com.rmc.config.ServerConfig;
import com.rmc.filters.loader.FilterPageLoader;
import com.rmc.filters.loader.FilterPageResult;
import com.rmc.filters.model.FilterGroup;
import com.rmc.filters.parser.FilterParser;
import com.rmc.history.SearchHistoryService;
import com.rmc.http.HttpClientService;
import com.rmc.search.model.AnalysisResult;
import com.rmc.search.service.ProgramAnalysisService;
import com.rmc.search.service.ProgramSearchService;
import com.rmc.state.ApplicationState;
import com.rmc.ui.dashboard.Dashboard;
import com.rmc.ui.statusbar.StatusBar;
import com.rmc.ui.workspace.views.*;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.List;

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
    
    // References
    private Dashboard dashboard;
    private StatusBar statusBar;
    
    // Previous state for retry
    private WorkspaceState previousState;
    
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
            case ERROR -> errorView;
        };
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
                        status -> javafx.application.Platform.runLater(() -> updateProgress(status)),
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
     * (например, "Учреждение 3 из 12: МБОУ ...").
     */
    public void updateProgress(String status) {
        if (loadingView != null) {
            loadingView.setStatus(status);
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
    
    public WorkspaceState getCurrentState() {
        return currentState;
    }
}
