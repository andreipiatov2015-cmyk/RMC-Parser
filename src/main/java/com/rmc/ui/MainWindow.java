package com.rmc.ui;

import com.rmc.state.ApplicationState;
import com.rmc.ui.dashboard.Dashboard;
import com.rmc.ui.navigation.NavigationDrawer;
import com.rmc.ui.statusbar.StatusBar;
import com.rmc.ui.theme.ThemeService;
import com.rmc.ui.topbar.TopBar;
import com.rmc.ui.workspace.WorkspaceContainer;
import javafx.geometry.Side;
import javafx.scene.layout.BorderPane;

/**
 * Main container for the entire application.
 * 
 * <p>Structure:</p>
 * <pre>
 * MainWindow (BorderPane)
 * ├── Top (TopBar)
 * ├── Left (Dashboard)
 * ├── Center (WorkspaceContainer)
 * └── Bottom (StatusBar)
 * </pre>
 */
public class MainWindow extends BorderPane {
    
    private final TopBar topBar;
    private final Dashboard dashboard;
    private final WorkspaceContainer workspace;
    private final StatusBar statusBar;
    private final NavigationDrawer navigationDrawer;
    
    public MainWindow() {
        getStyleClass().add("main-window");
        
        // Применяем сохранённую тему сразу при старте и подписываемся на
        // переключение (например, из меню в NavigationDrawer) — работает
        // мгновенно, без перезапуска.
        if (ThemeService.isDarkMode()) {
            getStyleClass().add("dark-theme");
        }
        ThemeService.addListener(dark -> {
            if (dark) {
                if (!getStyleClass().contains("dark-theme")) {
                    getStyleClass().add("dark-theme");
                }
            } else {
                getStyleClass().remove("dark-theme");
            }
        });
        
        // Create components
        topBar = new TopBar();
        dashboard = new Dashboard();
        workspace = new WorkspaceContainer();
        statusBar = new StatusBar();
        navigationDrawer = new NavigationDrawer(this);
        
        // Layout
        setupLayout();
        
        // Связываем компоненты друг с другом
        workspace.setReferences(dashboard, statusBar);
        dashboard.setWorkspace(workspace);
        
        // Connect navigation drawer to topbar menu button
        topBar.setOnMenuClick(() -> navigationDrawer.toggle());
        
        // Смена пользователя — на экран выбора учётной записи
        topBar.setOnSwitchUser(workspace::showAccountPicker);
        
        // Выход подтверждён в диалоге TopBar — выполняем сам выход и ведём
        // на форму входа с подставленным логином. Сохранённая учётная
        // запись НЕ удаляется (её видно на экране выбора аккаунта /
        // "Сменить пользователя"), но "Выход" — осознанное действие:
        // пароль всегда нужно набрать заново, иначе один клик по своей же
        // карточке аккаунта тут же вернул бы обратно без пароля, и весь
        // смысл "Выхода" терялся бы.
        topBar.setOnLogoutConfirmed(() -> {
            String username = ApplicationState.getInstance().getUsername();
            ApplicationState.getInstance().logout();
            workspace.promptReloginFor(username);
        });
        
        // Listen for auth state changes
        ApplicationState.getInstance().addAuthStateListener(event -> {
            workspace.onAuthStateChanged();
            dashboard.onAuthStateChanged();
            topBar.onAuthStateChanged();
        });
    }
    
    private void setupLayout() {
        // Top: TopBar (48px height)
        setTop(topBar);
        
        // Left: Dashboard (260px width)
        setLeft(dashboard);
        
        // Center: Workspace (fills remaining space)
        setCenter(workspace);
        
        // Bottom: StatusBar (24px height)
        setBottom(statusBar);
    }
    
    public void openNavigationDrawer() {
        navigationDrawer.open();
    }
    
    public void closeNavigationDrawer() {
        navigationDrawer.close();
    }
    
    public WorkspaceContainer getWorkspace() {
        return workspace;
    }
    
    public Dashboard getDashboard() {
        return dashboard;
    }
    
    public StatusBar getStatusBar() {
        return statusBar;
    }
    
    /**
     * Тихая проверка обновлений при старте — покажет диалог, только если
     * реально нашлась более новая версия.
     */
    public void checkForUpdatesOnStartup() {
        navigationDrawer.checkForUpdatesSilently();
    }
}
