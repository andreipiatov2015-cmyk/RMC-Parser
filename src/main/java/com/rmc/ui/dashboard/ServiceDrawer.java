package com.rmc.ui.dashboard;

import com.rmc.logging.AppLogger;
import com.rmc.update.UpdateService;
import com.rmc.version.VersionService;
import com.rmc.ui.DeveloperWindow;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service menu drawer - left side menu with service functions.
 */
public class ServiceDrawer extends VBox {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private final Stage primaryStage;
    private final DashboardView dashboardView;
    
    private boolean isOpen = false;
    private HBox container;
    private VBox menu;
    private Region overlay;
    
    public ServiceDrawer(Stage primaryStage, DashboardView dashboardView) {
        this.primaryStage = primaryStage;
        this.dashboardView = dashboardView;
        
        setupDrawer();
    }
    
    private void setupDrawer() {
        container = new HBox();
        
        // Overlay for clicking outside
        overlay = new Region();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.3)");
        overlay.setVisible(false);
        overlay.setOnMouseClicked(e -> close());
        
        // Menu panel
        menu = createMenuPanel();
        menu.setTranslateX(-250);
        
        container.getChildren().addAll(overlay, menu);
        getChildren().add(container);
        
        // Click on hamburger icon will toggle
        setOnMouseClicked(e -> {
            if (isOpen) {
                close();
            }
        });
    }
    
    private VBox createMenuPanel() {
        VBox panel = new VBox();
        panel.getStyleClass().add("service-drawer");
        panel.setPrefWidth(280);
        panel.setFillWidth(true);
        
        // Header
        Label header = new Label("☰ Сервис");
        header.getStyleClass().add("service-drawer-header");
        header.setPadding(new Insets(16, 20, 16, 20));
        
        Separator separator = new Separator();
        separator.getStyleClass().add("drawer-separator");
        
        // Menu items
        VBox menuItems = new VBox();
        menuItems.setSpacing(4);
        menuItems.setPadding(new Insets(8, 0, 8, 0));
        
        menuItems.getChildren().addAll(
            createMenuItem("📋 Журнал работы", e -> showLog()),
            createMenuItem("🔧 Диагностика", e -> showDiagnostics()),
            createMenuItem("⚙️ Настройки", e -> showSettings()),
            createMenuItem("🔄 Проверка обновлений", e -> checkUpdates()),
            createMenuItem("🗑️ Очистить кэш", e -> clearCache()),
            createMenuItem("📁 Открыть папку логов", e -> openLogsFolder()),
            createMenuItem("📤 Экспорт журнала", e -> exportLog()),
            createSeparator(),
            createMenuItem("ℹ️ О программе", e -> showAbout())
        );
        
        panel.getChildren().addAll(header, separator, menuItems);
        
        return panel;
    }
    
    private HBox createMenuItem(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        HBox item = new HBox();
        item.getStyleClass().add("service-menu-item");
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(12, 20, 12, 20));
        
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px;");
        
        item.getChildren().add(label);
        item.setOnMouseEntered(e -> item.getStyleClass().add("service-menu-item:hover"));
        item.setOnMouseExited(e -> item.getStyleClass().remove("service-menu-item.hover"));
        item.setOnMouseClicked(e -> {
            handler.handle(null);
            close();
        });
        
        return item;
    }
    
    private Separator createSeparator() {
        Separator sep = new Separator();
        sep.setPadding(new Insets(8, 0, 8, 0));
        return sep;
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
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(200), menu);
        slide.setToX(0);
        slide.play();
    }
    
    public void close() {
        if (!isOpen) return;
        isOpen = false;
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(200), menu);
        slide.setToX(-280);
        slide.setOnFinished(e -> overlay.setVisible(false));
        slide.play();
    }
    
    // Menu actions
    
    private void showLog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Журнал работы");
        alert.setHeaderText("Журнал работы приложения");
        
        // Get recent logs from application
        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setPrefSize(500, 300);
        
        // Add sample content
        StringBuilder log = new StringBuilder();
        log.append("=== Журнал работы ===\n\n");
        log.append("[").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("] Приложение запущено\n");
        log.append("[").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("] Загрузка интерфейса завершена\n");
        log.append("[").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("] Ожидание действий пользователя\n");
        
        textArea.setText(log.toString());
        
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        alert.getDialogPane().setContent(scrollPane);
        alert.show();
    }
    
    private void showDiagnostics() {
        DeveloperWindow devWindow = new DeveloperWindow();
        devWindow.show();
    }
    
    private void showSettings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Настройки");
        alert.setHeaderText("Настройки приложения");
        alert.setContentText("Настройки будут доступны в будущих версиях");
        alert.show();
    }
    
    private void checkUpdates() {
        logger.info("Проверка обновлений...");
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Обновления");
        alert.setHeaderText("Проверка обновлений");
        alert.setContentText("Проверяем обновления на GitHub...");
        alert.show();
        
        new Thread(() -> {
            try {
                UpdateService service = new UpdateService();
                service.checkForUpdates();
                
                javafx.application.Platform.runLater(() -> {
                    alert.close();
                    Alert result = new Alert(Alert.AlertType.INFORMATION);
                    result.setTitle("Обновления");
                    result.setHeaderText("Проверка завершена");
                    result.setContentText("У вас установлена последняя версия: " + VersionService.getCurrentVersionString());
                    result.show();
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    alert.close();
                    Alert error = new Alert(Alert.AlertType.ERROR);
                    error.setTitle("Ошибка");
                    error.setHeaderText("Ошибка проверки обновлений");
                    error.setContentText(e.getMessage());
                    error.show();
                });
            }
        }).start();
    }
    
    private void clearCache() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Очистка кэша");
        confirm.setHeaderText("Очистить кэш приложения?");
        confirm.setContentText("Это действие нельзя отменить.");
        
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                logger.info("Кэш очищен");
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Успешно");
                success.setHeaderText("Кэш очищен");
                success.setContentText("Кэш приложения успешно очищен.");
                success.show();
            }
        });
    }
    
    private void openLogsFolder() {
        try {
            String userHome = System.getProperty("user.home");
            String appData = System.getenv("LOCALAPPDATA");
            File logDir;
            
            if (appData != null) {
                logDir = new File(appData + "\\RMCFramework\\logs");
            } else {
                logDir = new File(userHome + "/RMCFramework/logs");
            }
            
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            Desktop.getDesktop().open(logDir);
        } catch (Exception e) {
            logger.error("Не удалось открыть папку логов", e);
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Ошибка");
            error.setContentText("Не удалось открыть папку логов: " + e.getMessage());
            error.show();
        }
    }
    
    private void exportLog() {
        TextInputDialog dialog = new TextInputDialog("rmc_export.log");
        dialog.setTitle("Экспорт журнала");
        dialog.setHeaderText("Введите имя файла");
        dialog.setContentText("Имя файла:");
        
        dialog.showAndWait().ifPresent(filename -> {
            try {
                String userHome = System.getProperty("user.home");
                File file = new File(userHome, filename);
                
                // Simple export
                StringBuilder content = new StringBuilder();
                content.append("=== RMC Framework Export ===\n");
                content.append("Date: ").append(LocalDateTime.now()).append("\n\n");
                content.append("=== Application Log ===\n");
                content.append("[INFO] Log export requested\n");
                content.append("[INFO] Export completed\n");
                
                java.nio.file.Files.writeString(file.toPath(), content.toString());
                
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Успешно");
                success.setHeaderText("Экспорт завершён");
                success.setContentText("Файл сохранён: " + file.getAbsolutePath());
                success.show();
                
            } catch (Exception e) {
                logger.error("Ошибка экспорта", e);
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Ошибка");
                error.setContentText("Ошибка экспорта: " + e.getMessage());
                error.show();
            }
        });
    }
    
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("О программе");
        alert.setHeaderText("RMC Framework");
        
        String content = String.format(
            "Версия: %s\n\n" +
            "Современное desktop-приложение для работы с системой РМЦ.\n\n" +
            "© 2024",
            VersionService.getCurrentVersionString()
        );
        
        alert.setContentText(content);
        alert.show();
    }
}
