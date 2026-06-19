package com.rmc.ui.navigation;

import com.rmc.ui.MainWindow;
import com.rmc.version.VersionService;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Navigation drawer - overlay menu that slides from left.
 */
public class NavigationDrawer extends VBox {
    
    private static final double WIDTH = 280;
    
    private final MainWindow mainWindow;
    private boolean isOpen = false;
    private Region overlay;
    private VBox menuContent;
    
    public NavigationDrawer(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        
        getStyleClass().add("navigation-drawer");
        setPrefWidth(WIDTH);
        setMaxWidth(WIDTH);
        setTranslateX(-WIDTH);
        
        setupContent();
        setupKeyboardHandler();
    }
    
    private void setupContent() {
        // Overlay (click to close)
        overlay = new Region();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.3)");
        overlay.setVisible(false);
        overlay.setOnMouseClicked(e -> close());
        
        // Menu content
        menuContent = new VBox();
        menuContent.getStyleClass().add("drawer-content");
        menuContent.setSpacing(0);
        menuContent.setAlignment(Pos.TOP_LEFT);
        
        // Header
        NavigationHeader header = new NavigationHeader();
        
        // Menu items
        VBox menuItems = new VBox();
        menuItems.getStyleClass().add("menu-items");
        menuItems.setSpacing(2);
        menuItems.setPadding(new Insets(8, 0, 8, 0));
        
        menuItems.getChildren().addAll(
            createMenuItem("🏠", "Главная", () -> handleHome()),
            createMenuItem("📋", "Профили анализа", () -> handleProfiles()),
            createMenuItem("📜", "История", () -> handleHistory()),
            createSeparator(),
            createMenuItem("🔧", "Диагностика", () -> handleDiagnostics()),
            createMenuItem("📝", "Журнал", () -> handleLog()),
            createSeparator(),
            createMenuItem("⚙️", "Настройки", () -> handleSettings()),
            createMenuItem("📤", "Экспорт", () -> handleExport()),
            createMenuItem("ℹ️", "О программе", () -> handleAbout())
        );
        
        menuContent.getChildren().addAll(header, menuItems);
        getChildren().add(menuContent);
    }
    
    private void setupKeyboardHandler() {
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE && isOpen) {
                close();
            }
        });
    }
    
    private NavigationItem createMenuItem(String icon, String text, Runnable handler) {
        return new NavigationItem(icon, text, handler);
    }
    
    private VBox createSeparator() {
        VBox separator = new VBox();
        separator.getStyleClass().add("menu-separator");
        separator.setPrefHeight(1);
        separator.setMaxHeight(1);
        separator.setPadding(new Insets(8, 16, 8, 16));
        return separator;
    }
    
    public void toggle() {
        if (isOpen) {
            close();
        } else {
            open();
        }
    }
    
    public void open() {
        if (isOpen) return;
        isOpen = true;
        
        overlay.setVisible(true);
        mainWindow.getChildren().add(0, overlay);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(200), this);
        slide.setToX(0);
        slide.play();
    }
    
    public void close() {
        if (!isOpen) return;
        isOpen = false;
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(200), this);
        slide.setToX(-WIDTH);
        slide.setOnFinished(e -> {
            overlay.setVisible(false);
            mainWindow.getChildren().remove(overlay);
        });
        slide.play();
    }
    
    private void handleHome() {
        close();
        // TODO: Implement home action
    }
    
    private void handleProfiles() {
        close();
        // TODO: Implement profiles action
    }
    
    private void handleHistory() {
        close();
        // TODO: Implement history action
    }
    
    private void handleDiagnostics() {
        close();
        // TODO: Implement diagnostics
    }
    
    private void handleLog() {
        close();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Журнал");
        alert.setHeaderText("Журнал работы приложения");
        alert.setContentText("Журнал доступен в папке логов");
        alert.show();
    }
    
    private void handleSettings() {
        close();
        // TODO: Implement settings
    }
    
    private void handleExport() {
        close();
        // TODO: Implement export
    }
    
    private void handleAbout() {
        close();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("О программе");
        alert.setHeaderText("RMC Framework");
        alert.setContentText("Версия: " + VersionService.getCurrentVersionString() + "\n\n© 2024");
        alert.show();
    }
    
    // Inner class for header
    private static class NavigationHeader extends VBox {
        public NavigationHeader() {
            getStyleClass().add("drawer-header");
            setPadding(new Insets(16, 20, 16, 20));
            
            javafx.scene.control.Label title = new javafx.scene.control.Label("Меню");
            title.getStyleClass().add("drawer-header-title");
            
            getChildren().add(title);
        }
    }
}
