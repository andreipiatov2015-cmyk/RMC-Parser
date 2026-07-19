package com.rmc.ui.navigation;

import com.rmc.ui.MainWindow;
import com.rmc.ui.theme.ThemeService;
import com.rmc.update.UpdateCheckResult;
import com.rmc.update.UpdateCheckService;
import com.rmc.version.VersionService;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Navigation drawer - overlay menu that slides from left.
 */
public class NavigationDrawer extends VBox {
    
    private static final double WIDTH = 280;
    
    private final MainWindow mainWindow;
    private boolean isOpen = false;
    private Region overlay;
    private VBox menuContent;
    private NavigationItem themeItem;
    
    public NavigationDrawer(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        
        getStyleClass().add("navigation-drawer");
        setPrefWidth(WIDTH);
        setMaxWidth(WIDTH);
        setTranslateX(-WIDTH);
        
        // ВАЖНО: BorderPane управляет размером/позицией только детей,
        // назначенных через setTop/setLeft/setCenter/setRight/setBottom.
        // Панель и оверлей добавляются как "лишние" дети напрямую в
        // getChildren() — без этой явной привязки они сворачиваются под
        // размер своего содержимого вместо того, чтобы растянуться на всю
        // высоту окна.
        prefHeightProperty().bind(mainWindow.heightProperty());
        maxHeightProperty().bind(mainWindow.heightProperty());
        
        setupContent();
        setupKeyboardHandler();
    }
    
    private void setupContent() {
        // Overlay (click to close)
        overlay = new Region();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.3)");
        overlay.setVisible(false);
        overlay.setOnMouseClicked(e -> close());
        // Та же причина, что и у самой панели выше — без явной привязки
        // размер оверлея сворачивается почти до нуля, и клик "мимо панели,
        // чтобы закрыть" физически не по чему было делать.
        overlay.prefWidthProperty().bind(mainWindow.widthProperty());
        overlay.prefHeightProperty().bind(mainWindow.heightProperty());
        
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
        
        themeItem = createMenuItem(themeIcon(), themeLabel(), this::handleThemeToggle);
        
        menuItems.getChildren().addAll(
            themeItem,
            createMenuItem(null, "Обновление программы", () -> handleUpdate()),
            createMenuItem(null, "О программе", () -> handleAbout())
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
        
        if (!mainWindow.getChildren().contains(overlay)) {
            // Добавляем в конец (а не в начало) списка — оверлей должен
            // быть НАД обычным содержимым окна (топбар/дашборд/рабочая
            // область) по z-порядку, иначе клик "мимо панели" попадает на
            // эти элементы раньше, чем на оверлей, и закрытие не работает.
            mainWindow.getChildren().add(overlay);
        }
        if (!mainWindow.getChildren().contains(this)) {
            mainWindow.getChildren().add(this);
        }
        overlay.setVisible(true);
        
        // Подстраховка к привязкам prefWidth/prefHeight выше: BorderPane не
        // вызывает resize() для "лишних" детей сам, явно задаём размер.
        overlay.resize(mainWindow.getWidth(), mainWindow.getHeight());
        resize(WIDTH, mainWindow.getHeight());
        
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
            mainWindow.getChildren().remove(this);
        });
        slide.play();
    }
    
    /**
     * Переключить тему и сразу обновить иконку/подпись пункта меню —
     * панель при этом НЕ закрывается, чтобы сразу было видно результат.
     */
    private void handleThemeToggle() {
        ThemeService.toggle();
        themeItem.setIcon(themeIcon());
        themeItem.setText(themeLabel());
    }
    
    private String themeIcon() {
        return ThemeService.isDarkMode() ? "☀" : "🌙";
    }
    
    private String themeLabel() {
        return ThemeService.isDarkMode() ? "Светлая тема" : "Тёмная тема";
    }
    
    private void handleUpdate() {
        close();
        
        new Thread(() -> {
            UpdateCheckService service = new UpdateCheckService();
            UpdateCheckResult result = service.checkForUpdates();
            javafx.application.Platform.runLater(() -> showUpdateResult(result));
        }).start();
    }
    
    /**
     * Тихая проверка обновлений при старте приложения — в отличие от
     * {@link #handleUpdate()}, ничего не показывает, если обновлений нет
     * или проверка не удалась (например, нет сети); не должна мешать
     * запуску назойливыми диалогами.
     */
    public void checkForUpdatesSilently() {
        new Thread(() -> {
            UpdateCheckService service = new UpdateCheckService();
            UpdateCheckResult result = service.checkForUpdates();
            if (result.isSuccess() && result.isUpdateAvailable()) {
                javafx.application.Platform.runLater(() -> showUpdateResult(result));
            }
        }).start();
    }
    
    /**
     * Единое окно ошибки обновления с кнопкой "Повторить попытку" —
     * используется на любом шаге (проверка версии, скачивание, запуск
     * установщика). {@code onRetry} — что именно повторить.
     */
    private void showUpdateError(String headerText, String message, Runnable onRetry) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка обновления");
        alert.setHeaderText(headerText);
        alert.setContentText(message);
        styleDialog(alert);
        
        ButtonType retryButtonType = new ButtonType("Повторить попытку");
        alert.getButtonTypes().setAll(retryButtonType, ButtonType.CLOSE);
        
        alert.showAndWait().ifPresent(button -> {
            if (button == retryButtonType && onRetry != null) {
                onRetry.run();
            }
        });
    }
    
    private void showUpdateResult(UpdateCheckResult result) {
        if (!result.isSuccess()) {
            showUpdateError(
                    "Не удалось проверить обновления",
                    "Текущая версия: " + result.getCurrentVersion() + "\n\n"
                            + result.getErrorMessage().orElse("Неизвестная ошибка"),
                    this::handleUpdate
            );
            return;
        }
        
        if (!result.isUpdateAvailable()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Обновление");
            alert.setHeaderText("У вас последняя версия");
            alert.setContentText("Текущая версия: " + result.getCurrentVersion());
            styleDialog(alert);
            alert.showAndWait();
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Обновление");
        alert.setHeaderText("Доступна новая версия: " + result.getLatestVersion().orElse("?"));
        
        StringBuilder content = new StringBuilder();
        content.append("Текущая версия: ").append(result.getCurrentVersion()).append("\n");
        content.append("Новая версия: ").append(result.getLatestVersion().orElse("?"));
        result.getReleaseNotes()
                .filter(notes -> !notes.isBlank())
                .ifPresent(notes -> content.append("\n\n").append(notes));
        alert.setContentText(content.toString());
        styleDialog(alert);
        
        if (result.getDownloadUrl().isEmpty()) {
            alert.getButtonTypes().setAll(ButtonType.CLOSE);
            alert.showAndWait();
            return;
        }
        
        ButtonType downloadButtonType = new ButtonType("Скачать");
        alert.getButtonTypes().setAll(downloadButtonType, ButtonType.CANCEL);
        
        alert.showAndWait().ifPresent(button -> {
            if (button == downloadButtonType) {
                downloadUpdate(result);
            }
        });
    }
    
    /**
     * Скачивает обновление максимально незаметно для пользователя: без
     * диалога "Сохранить как" (файл сохраняется во временную папку сам),
     * с прогресс-баром скачивания, и по завершении сам запускает
     * установщик в режиме "/passive" (WiX Burn понимает этот флаг —
     * показывает только прогресс-бар установки, без единого клика) и
     * закрывает текущую программу, чтобы установщик мог её обновить.
     */
    private void downloadUpdate(UpdateCheckResult result) {
        String assetName = result.getAssetName().orElse("RMC-Framework-update.exe");
        Path destination = Paths.get(System.getProperty("java.io.tmpdir"), assetName);
        
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(280);
        Label progressLabel = new Label("Загрузка обновления...");
        progressLabel.getStyleClass().add("drawer-header-title");
        
        VBox progressContent = new VBox(12, progressLabel, progressBar);
        progressContent.setPadding(new Insets(20));
        progressContent.setAlignment(Pos.CENTER);
        progressContent.getStyleClass().add("app-dialog");
        
        Stage progressStage = new Stage(StageStyle.UTILITY);
        progressStage.setTitle("Обновление");
        progressStage.setResizable(false);
        progressStage.initModality(Modality.APPLICATION_MODAL);
        if (mainWindow.getScene() != null) {
            progressStage.initOwner(mainWindow.getScene().getWindow());
        }
        Scene progressScene = new Scene(progressContent, 320, 120);
        progressScene.getStylesheets().add(getClass().getResource("/styles/dashboard.css").toExternalForm());
        progressScene.getStylesheets().add(getClass().getResource("/styles/dashboard-dark.css").toExternalForm());
        if (ThemeService.isDarkMode()) {
            progressContent.getStyleClass().add("dark-theme");
        }
        progressStage.setScene(progressScene);
        progressStage.show();
        
        new Thread(() -> {
            try {
                UpdateCheckService service = new UpdateCheckService();
                service.download(result.getDownloadUrl().orElseThrow(), destination, (bytesRead, totalBytes) -> {
                    if (totalBytes > 0) {
                        double fraction = (double) bytesRead / totalBytes;
                        javafx.application.Platform.runLater(() -> progressBar.setProgress(fraction));
                    }
                });
                
                javafx.application.Platform.runLater(() -> {
                    progressStage.close();
                    launchInstallerAndClose(destination);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    progressStage.close();
                    showUpdateError("Не удалось скачать обновление", e.getMessage(),
                            () -> downloadUpdate(result));
                });
            }
        }).start();
    }
    
    /**
     * Запускает скачанный установщик в режиме "/passive" (только
     * прогресс-бар, без взаимодействия) и закрывает текущую программу,
     * чтобы установщик мог заменить её файлы. Пользователь уже подтвердил
     * желание обновиться кнопкой "Скачать" на предыдущем шаге — здесь
     * ничего больше не спрашиваем, иначе весь смысл "незаметного"
     * обновления теряется.
     */
    private void launchInstallerAndClose(Path installerPath) {
        try {
            new ProcessBuilder(installerPath.toString(), "/passive").start();
            javafx.application.Platform.exit();
            System.exit(0);
        } catch (IOException e) {
            showUpdateError("Не удалось запустить установщик",
                    "Файл сохранён: " + installerPath + "\n\nЗапустите его вручную.\n\n" + e.getMessage(),
                    () -> launchInstallerAndClose(installerPath));
        }
    }
    
    private void handleAbout() {
        close();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("О программе");
        alert.setHeaderText("RMC Framework");
        alert.setContentText(
                "Версия: " + VersionService.getCurrentVersionString() + "\n\n"
                        + "Приложение для работы с реестром образовательных программ\n"
                        + "через HTTP — без браузера и Selenium."
        );
        styleDialog(alert);
        alert.showAndWait();
    }
    
    /**
     * Подключает основную таблицу стилей приложения к диалогу и помечает
     * его классом "app-dialog", чтобы он выглядел так же, как остальной
     * интерфейс, а не стандартным системным окном.
     */
    private void styleDialog(Alert alert) {
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm());
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard-dark.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("app-dialog");
        if (com.rmc.ui.theme.ThemeService.isDarkMode()) {
            alert.getDialogPane().getStyleClass().add("dark-theme");
        }
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
