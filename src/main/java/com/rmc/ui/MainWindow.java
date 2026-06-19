package com.rmc.ui;

import com.rmc.state.ApplicationState;
import com.rmc.ui.dashboard.Dashboard;
import com.rmc.ui.navigation.NavigationDrawer;
import com.rmc.ui.statusbar.StatusBar;
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
        
        // Create components
        topBar = new TopBar();
        dashboard = new Dashboard();
        workspace = new WorkspaceContainer();
        statusBar = new StatusBar();
        navigationDrawer = new NavigationDrawer(this);
        
        // Layout
        setupLayout();
        
        // Connect navigation drawer to topbar menu button
        topBar.setOnMenuClick(() -> navigationDrawer.toggle());
        
        // Listen for auth state changes
        ApplicationState.getInstance().addAuthStateListener(event -> {
            workspace.onAuthStateChanged();
            dashboard.onAuthStateChanged();
            topBar.onAuthStateChanged();
        });
        
        // Start in AUTH state
        workspace.transitionTo(com.rmc.ui.workspace.WorkspaceState.AUTH);
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
}
