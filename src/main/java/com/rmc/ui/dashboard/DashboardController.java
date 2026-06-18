package com.rmc.ui.dashboard;

import com.rmc.state.ApplicationState;
import org.slf4j.Logger;
import com.rmc.logging.AppLogger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for DashboardView.
 * Handles user interactions and coordinates between components.
 */
public class DashboardController {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private final DashboardView view;
    
    public DashboardController(DashboardView view) {
        this.view = view;
        setupEventHandlers();
    }
    
    private void setupEventHandlers() {
        // Controller is ready
        view.getLogPane().addLog("Контроллер инициализирован");
    }
    
    public void onAuthStateChanged() {
        ApplicationState state = ApplicationState.getInstance();
        
        if (state.isAuthenticated()) {
            view.getLoginCard().updateView();
            view.getLogPane().addLog("Авторизация выполнена: " + state.getUsername());
        } else {
            view.getLoginCard().updateView();
            view.getLogPane().addLog("Выход выполнен");
        }
        
        view.updateStatus();
    }
    
    public void logAction(String action) {
        logger.info(action);
        view.getLogPane().addLog(action);
    }
    
    public void logError(String error) {
        logger.error(error);
        view.getLogPane().addLog("ОШИБКА: " + error);
    }
    
    public void logInfo(String info) {
        logger.info(info);
        view.getLogPane().addLog(info);
    }
}
