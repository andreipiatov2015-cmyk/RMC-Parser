package com.rmc.ui.workspace;

import com.rmc.filters.model.FilterGroup;
import com.rmc.ui.dashboard.Dashboard;
import com.rmc.ui.statusbar.StatusBar;
import com.rmc.ui.workspace.views.*;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.List;
import java.util.Map;

/**
 * Workspace container - manages state transitions and views.
 */
public class WorkspaceContainer extends StackPane {
    
    private WorkspaceState currentState = WorkspaceState.AUTH;
    private WorkspaceView currentView;
    
    // Views
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
        authView = new AuthView(this);
        loadingView = new LoadingView();
        filtersView = new FiltersView(this);
        resultsView = new ResultsView(this);
        errorView = new ErrorView(this);
        
        // Start with AUTH view
        currentView = authView;
        getChildren().add(authView);
    }
    
    public void setReferences(Dashboard dashboard, StatusBar statusBar) {
        this.dashboard = dashboard;
        this.statusBar = statusBar;
    }
    
    public void transitionTo(WorkspaceState newState) {
        if (newState == currentState) return;
        
        previousState = currentState;
        currentState = newState;
        
        WorkspaceView newView = getViewForState(newState);
        
        // Fade transition
        FadeTransition fadeOut = new FadeTransition(Duration.millis(150), currentView);
        fadeOut.setToValue(0);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), newView);
        fadeIn.setToValue(1);
        
        fadeOut.setOnFinished(e -> {
            currentView.setVisible(false);
            getChildren().add(newView);
            newView.setVisible(true);
            fadeIn.play();
        });
        
        fadeOut.play();
        currentView = newView;
    }
    
    public void transitionTo(WorkspaceState newState, Object data) {
        transitionTo(newState);
        
        // Handle data based on state
        if (newState == WorkspaceState.ERROR && data instanceof String error) {
            ((ErrorView) currentView).setError(error);
        }
    }
    
    private WorkspaceView getViewForState(WorkspaceState state) {
        return switch (state) {
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
        
        // TODO: Load filters automatically
        // For now, simulate loading then showing filters
        new Thread(() -> {
            try {
                Thread.sleep(1500); // Simulate loading
                javafx.application.Platform.runLater(() -> {
                    transitionTo(WorkspaceState.FILTERS_READY);
                    if (dashboard != null) {
                        dashboard.onAuthStateChanged();
                    }
                    if (statusBar != null) {
                        statusBar.setConnected(true);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    public void onLoginFailure(String message) {
        transitionTo(WorkspaceState.ERROR);
        ((ErrorView) currentView).setError("Ошибка входа: " + message);
    }
    
    public void onFiltersLoaded(List<FilterGroup> groups) {
        transitionTo(WorkspaceState.FILTERS_READY);
        ((FiltersView) currentView).setFilters(groups);
    }
    
    public void onFiltersLoadFailed(String error) {
        transitionTo(WorkspaceState.ERROR);
        ((ErrorView) currentView).setError("Ошибка загрузки фильтров: " + error);
    }
    
    public void onAnalysisStarted() {
        transitionTo(WorkspaceState.ANALYZING);
    }
    
    public void onAnalysisComplete() {
        transitionTo(WorkspaceState.RESULTS);
    }
    
    public void onAnalysisFailed(String error) {
        transitionTo(WorkspaceState.ERROR);
        ((ErrorView) currentView).setError("Ошибка анализа: " + error);
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
    
    // Dashboard updates
    
    public void updateStats(Map<String, Integer> stats) {
        if (dashboard != null) {
            if (stats.containsKey("institutions")) {
                dashboard.updateInstitutions(stats.get("institutions"));
            }
            if (stats.containsKey("programs")) {
                dashboard.updatePrograms(stats.get("programs"));
            }
            if (stats.containsKey("checked")) {
                dashboard.updateChecked(stats.get("checked"));
            }
            if (stats.containsKey("enrollments")) {
                dashboard.updateEnrollments(stats.get("enrollments"));
            }
        }
    }
}
