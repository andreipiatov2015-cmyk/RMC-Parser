package com.rmc.ui.workspace;

import com.rmc.config.ServerConfig;
import com.rmc.filters.loader.FilterPageLoader;
import com.rmc.filters.loader.FilterPageResult;
import com.rmc.filters.model.FilterGroup;
import com.rmc.filters.parser.FilterParser;
import com.rmc.history.SearchHistoryService;
import com.rmc.http.HttpClientService;
import com.rmc.params.ParamsAnalysisService;
import com.rmc.search.service.*;
import com.rmc.state.ApplicationState;
import com.rmc.ui.dashboard.Dashboard;
import com.rmc.ui.topbar.TopBar;
import com.rmc.ui.workspace.views.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.rmc.ui.workspace.WorkspaceState.*;

/** Each section keeps its current screen and outstanding request for the authenticated session. */
public class WorkspaceContainer extends StackPane {
    private final AccountPickerView picker = new AccountPickerView(this);
    private final AuthView auth = new AuthView(this);
    private final ResultsView results = new ResultsView(this);
    private final CooperatesResultsView agreementsResults = new CooperatesResultsView(this);
    private final ParamsView paramsView = new ParamsView(this);
    private final FavoritesView favorites = new FavoritesView(this);
    private final RmcAiView ai = new RmcAiView(this);
    private final ErrorView error = new ErrorView(this);
    private final FiltersView filters = new FiltersView(this);
    private final FiltersView agreementsFilters = new FiltersView("Параметры поиска — Соглашения", this::runCooperatesAnalysis);
    private final Section registry = new Section();
    private final Section agreements = new Section();
    private final Section params = new Section();
    private Section active;
    private Section filterSection;
    private WorkspaceState currentState;
    private WorkspaceView currentView;
    private Dashboard dashboard;
    private TopBar topBar;

    private static final class Section {
        final LoadingView loading = new LoadingView();
        boolean hasFilters;
        WorkspaceState state;
        WorkspaceView view;
        String error;
        Runnable retry;
        Request request;

        void invalidate() {
            if (request != null) request.cancelled = true;
            request = null;
            state = null;
            view = null;
            retry = null;
            error = null;
        }
    }

    private static final class Request {
        final HttpClientService client = ApplicationState.getInstance().getHttpClient();
        final Section section;
        volatile boolean cancelled;

        Request(Section section) {
            this.section = section;
        }

        void progress(String message, int current, int total) {
            Platform.runLater(() -> {
                if (section.request == this) {
                    section.loading.setStatus(message);
                    section.loading.setProgress(current, total);
                }
            });
        }

        ProgramSearchService search() {
            return ProgramSearchService.builder().httpClient(client).build();
        }
    }

    public WorkspaceContainer() {
        getStyleClass().add("workspace");
        setPadding(new Insets(16));
        transitionTo(AccountPickerView.hasSavedAccounts() ? ACCOUNT_PICKER : AUTH);
    }

    public void setReferences(Dashboard dashboard, TopBar topBar) {
        this.dashboard = dashboard;
        this.topBar = topBar;
    }

    private boolean authenticated() {
        return ApplicationState.getInstance().isAuthenticated();
    }

    public void transitionTo(WorkspaceState state) {
        if (state == AUTH || state == ACCOUNT_PICKER) {
            active = null;
            show(state, state == AUTH ? auth : picker);
        }
    }

    private void show(WorkspaceState state, WorkspaceView view) {
        if (currentView != view) {
            if (currentView != null) currentView.onExit();
            getChildren().setAll(view.getRoot());
            currentView = view;
        }
        currentState = state;
        view.onEnter();
    }

    private void display(Section section) {
        show(section.state, section.view);
        if (section.state == ERROR) error.setError(section.error);
    }

    private void remember(Section section, WorkspaceState state, WorkspaceView view) {
        section.state = state;
        section.view = view;
        if (active == section) display(section);
    }

    private void select(Section section) {
        if (active == params && section != params) params.invalidate();
        active = section;
        if (section != null && topBar != null) topBar.selectSection(section == agreements, section == params);
        if (section == registry || section == agreements) filterSection = section;
    }

    public void onAuthStateChanged() {
        if (!authenticated()) {
            registry.invalidate();
            agreements.invalidate();
            params.invalidate();
            registry.hasFilters = false;
            agreements.hasFilters = false;
            filters.setFilters(List.of());
            agreementsFilters.setFilters(List.of());
            results.setResult(null);
            agreementsResults.setResult(null);
            paramsView.setResult(null);
            filterSection = registry;
            transitionTo(AUTH);
        }
    }

    public void onLoginSuccess(String username) {
        showRegistryHome();
    }

    public void promptReloginFor(String username) {
        transitionTo(AUTH);
        auth.prefillUsername(username);
    }

    public void showAccountPicker() {
        if (authenticated()) ApplicationState.getInstance().logout();
        transitionTo(ACCOUNT_PICKER);
    }

    public FiltersView getFiltersView() {
        return filters;
    }

    public void showRegistryHome() {
        openSearchSection(registry);
    }

    public void showCooperatesSection() {
        openSearchSection(agreements);
    }

    private void openSearchSection(Section section) {
        if (!authenticated()) return;
        select(section);
        if (section.state == null) loadFilters(section);
        else display(section);
    }

    private void loadFilters(Section section) {
        boolean programs = section == registry;
        FiltersView view = programs ? filters : agreementsFilters;
        start(section, programs ? LOADING_FILTERS : COOPERATES_LOADING_FILTERS, "Загрузка фильтров…", false,
                request -> {
                    FilterPageResult page = FilterPageLoader.builder().httpClient(request.client)
                            .baseUrl(ServerConfig.BASE_URL).path(programs ? "programs/" : "cooperates/")
                            .build().load();
                    if (!page.isSuccess()) throw new IllegalStateException(page.getErrorMessage().orElse("Ошибка загрузки фильтров"));
                    FilterParser.ParseResult parsed = FilterParser.parse(page.getHtml());
                    if (!parsed.isSuccess()) throw new IllegalStateException(parsed.getErrorMessage().orElse("Ошибка разбора фильтров"));
                    return parsed.getGroups();
                }, (List<FilterGroup> groups) -> {
                    section.hasFilters = true;
                    view.setFilters(groups);
                    remember(section, programs ? FILTERS_READY : COOPERATES_FILTERS_READY, view);
                });
    }

    public void onBackToFilters() {
        if (!authenticated()) return;
        if (filterSection == agreements) showCooperatesFilters();
        else back(registry, FILTERS_READY, filters);
    }

    public void showCooperatesFilters() {
        back(agreements, COOPERATES_FILTERS_READY, agreementsFilters);
    }

    private void back(Section section, WorkspaceState state, FiltersView view) {
        if (!authenticated()) return;
        select(section);
        if (section.state == null || section.state == LOADING_FILTERS || section.state == COOPERATES_LOADING_FILTERS) {
            openSearchSection(section);
        } else if (!section.hasFilters) {
            loadFilters(section);
        } else {
            section.invalidate();
            remember(section, state, view);
        }
    }

    public void showFavorites() {
        if (!authenticated()) return;
        select(null);
        show(FAVORITES, favorites);
    }

    public void showInstitutionDetail(com.rmc.favorites.FavoriteInstitution favorite) {
        if (!authenticated()) return;
        InstitutionDetailView detail = new InstitutionDetailView(this);
        select(null);
        show(INSTITUTION_DETAIL, detail);
        detail.showInstitution(favorite);
    }

    public void showRmcAi() {
        if (!authenticated()) return;
        select(null);
        show(RMC_AI, ai);
    }

    public void runAnalysis(String query, List<String> summary) {
        if (!authenticated()) return;
        select(registry);
        start(registry, ANALYZING, "Анализ программ…", true, request -> {
            var service = ProgramAnalysisService.builder().baseUrl(ServerConfig.BASE_URL).searchService(request.search()).build();
            var result = service.analyze(query, request::progress, () -> request.cancelled);
            if (!result.isSuccess()) throw new IllegalStateException(result.getErrorMessage().orElse("Ошибка анализа программ"));
            return result;
        }, result -> {
            if (!result.isCancelled()) {
                SearchHistoryService.add(query, summary, result.getTotalPrograms(), result.getTotalInstitutions());
                if (dashboard != null) dashboard.refreshHistory();
            }
            results.setResult(result);
            remember(registry, RESULTS, results);
        });
    }

    public void runCooperatesAnalysis(String query, List<String> summary) {
        if (!authenticated()) return;
        select(agreements);
        start(agreements, COOPERATES_ANALYZING, "Анализ соглашений…", true, request -> {
            var service = CooperateAnalysisService.builder().baseUrl(ServerConfig.BASE_URL).searchService(request.search()).build();
            var result = service.analyze(query, request::progress, () -> request.cancelled);
            if (!result.isSuccess()) throw new IllegalStateException(result.getErrorMessage().orElse("Ошибка анализа соглашений"));
            return result;
        }, result -> {
            agreementsResults.setResult(result);
            remember(agreements, COOPERATES_RESULTS, agreementsResults);
        });
    }

    public void showParamsSection() {
        if (!authenticated()) return;
        select(params);
        if (params.request != null) return;
        start(params, PARAMS_ANALYZING, "Загрузка параметров РМЦ…", true, request -> {
            var result = new ParamsAnalysisService(request.search()).analyze(Map.of(), request::progress, () -> request.cancelled);
            if (!result.isSuccess()) throw new IllegalStateException(result.getErrorMessage().orElse("Ошибка загрузки параметров"));
            return result;
        }, result -> {
            paramsView.setResult(result);
            remember(params, PARAMS_READY, paramsView);
        });
    }

    private <T> void start(Section section, WorkspaceState state, String message, boolean canCancel,
                           Function<Request, T> work, Consumer<T> complete) {
        section.invalidate();
        Request request = new Request(section);
        section.request = request;
        section.loading.resetProgress();
        section.loading.setStatus(message);
        section.loading.setCancellable(canCancel, () -> request.cancelled = true);
        remember(section, state, section.loading);
        Thread thread = new Thread(() -> {
            try {
                T result = work.apply(request);
                Platform.runLater(() -> {
                    if (section.request != request) return;
                    section.request = null;
                    complete.accept(result);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (section.request != request) return;
                    section.request = null;
                    section.error = ex.getMessage();
                    section.retry = () -> start(section, state, message, canCancel, work, complete);
                    remember(section, ERROR, error);
                });
            }
        }, "rmc-" + state.name());
        thread.setDaemon(true);
        thread.start();
    }

    public void onRetry() {
        if (authenticated() && active != null && active.retry != null) active.retry.run();
    }

    public WorkspaceState getCurrentState() {
        return currentState;
    }
}
