package com.rmc.ui.dashboard;

import com.rmc.version.VersionService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Header pane with app title and service menu button.
 */
public class HeaderPane extends HBox {
    
    private ServiceDrawer serviceDrawer;
    private DashboardView dashboardView;
    
    public HeaderPane() {
        getStyleClass().add("header-pane");
        setPadding(new Insets(12, 20, 12, 16));
        setAlignment(Pos.CENTER_LEFT);
        
        // Service menu button (hamburger icon)
        Label menuButton = new Label("☰");
        menuButton.getStyleClass().add("menu-button");
        menuButton.setOnMouseClicked(e -> {
            if (serviceDrawer != null) {
                serviceDrawer.toggle();
            }
        });
        
        // App title
        Label appTitle = new Label("RMC Framework");
        appTitle.getStyleClass().add("app-title");
        
        // Version
        Label versionLabel = new Label("v" + VersionService.getCurrentVersionString());
        versionLabel.getStyleClass().add("version-label");
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        getChildren().addAll(menuButton, appTitle, spacer, versionLabel);
    }
    
    public void setServiceDrawer(ServiceDrawer drawer) {
        this.serviceDrawer = drawer;
    }
    
    public void setDashboardView(DashboardView view) {
        this.dashboardView = view;
    }
}
