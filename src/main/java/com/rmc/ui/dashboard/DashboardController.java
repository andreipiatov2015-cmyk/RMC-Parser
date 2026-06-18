package com.rmc.ui.dashboard;

import com.rmc.state.ApplicationState;
import org.slf4j.Logger;
import com.rmc.logging.AppLogger;

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
        logger.info("Контроллер инициализирован");
    }
    
    public void onAuthStateChanged() {
        ApplicationState state = ApplicationState.getInstance();
        
        if (state.isAuthenticated()) {
            logger.info("Авторизация выполнена: {}", state.getUsername());
        } else {
            logger.info("Выход выполнен");
        }
    }
    
    public void logAction(String action) {
        logger.info(action);
    }
    
    public void logError(String error) {
        logger.error(error);
    }
    
    public void logInfo(String info) {
        logger.info(info);
    }
}
