package com.rmc.ui;

import com.rmc.app.ApplicationLifecycle;
import com.rmc.app.LifecycleReport;
import com.rmc.config.UpdateConfig;
import com.rmc.driver.DriverDetector;
import com.rmc.driver.DriverInfo;
import com.rmc.driver.DriverService;
import com.rmc.driver.DriverStatus;
import com.rmc.driver.EdgeDetector;
import com.rmc.driver.EdgeInfo;
import com.rmc.driver.manager.WebDriverManagerAdapter;
import com.rmc.driver.validation.DriverValidator;
import com.rmc.driver.validation.ValidationResult;
import com.rmc.driver.validation.ValidationStatus;
import com.rmc.i18n.Messages;
import com.rmc.logging.AppLogger;
import com.rmc.logging.ui.UiLogAppender;
import com.rmc.update.UpdateService;
import com.rmc.version.VersionService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.slf4j.Logger;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Developer Diagnostics Window with live log console and system status.
 */
public class DeveloperWindow {

    private static final Logger logger = AppLogger.getLogger();

    private Stage stage;
    private TextArea logConsole;
    private Label statusConfig;
    private Label statusLogging;
    private Label statusVersion;
    private Label statusUpdate;
    private Label statusDriver;
    private Label statusValidation;
    private Label statusInternet;
    private TextArea environmentInfo;
    private Label applicationStatus;
    private LifecycleReport lastReport;

    public DeveloperWindow() {
        createWindow();
    }

    private void createWindow() {
        stage = new Stage();
        stage.setTitle(Messages.APP_TITLE + " - " + Messages.BTN_DEVELOPER_DIAGNOSTICS);
        stage.setWidth(900);
        stage.setHeight(700);
        stage.setMinWidth(700);
        stage.setMinHeight(500);

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(
                createDiagnosticsTab(),
                createEnvironmentTab(),
                createAboutTab()
        );

        Scene scene = new Scene(tabPane);
        stage.setScene(scene);

        // Прикрепляем appender логов
        Platform.runLater(() -> {
            UiLogAppender.attach(logConsole);
            logger.info(Messages.LOG_DEV_WINDOW_OPEN);
        });
    }

    private Tab createDiagnosticsTab() {
        Tab tab = new Tab(Messages.TAB_DIAGNOSTICS);
        tab.setClosable(false);

        BorderPane root = new BorderPane();
        root.setTop(createButtonPanel());
        root.setCenter(createLogConsole());
        root.setBottom(createStatusBar());

        tab.setContent(root);
        return tab;
    }

    private HBox createButtonPanel() {
        HBox hbox = new HBox(10);
        hbox.setPadding(new Insets(10));
        hbox.setStyle("-fx-background-color: #f5f5f5;");

        Button btnCheckUpdates = createButton(Messages.BTN_CHECK_UPDATES_LONG, e -> checkUpdates());
        Button btnCheckDriver = createButton(Messages.BTN_CHECK_DRIVER, e -> checkDriver());
        Button btnDownloadDriver = createButton(Messages.BTN_DOWNLOAD_DRIVER, e -> downloadDriver());
        Button btnValidateDriver = createButton(Messages.BTN_VALIDATE_DRIVER, e -> validateDriver());
        Button btnRunStartup = createButton(Messages.BTN_RUN_STARTUP, e -> runStartupSequence());
        Button btnDiagnosticReport = createButton(Messages.BTN_CREATE_REPORT, e -> createDiagnosticReport());
        Button btnOpenLogFolder = createButton(Messages.BTN_OPEN_LOG_FOLDER, e -> openLogFolder());
        Button btnClearDriver = createButton(Messages.BTN_CLEAR_DRIVER, e -> clearDriver());
        Button btnClearLogs = createButton(Messages.BTN_CLEAR_LOGS, e -> clearLogs());
        Button btnExit = createButton(Messages.BTN_EXIT, e -> exit());

        hbox.getChildren().addAll(
                btnCheckUpdates, btnCheckDriver, btnDownloadDriver, btnValidateDriver,
                new Separator(),
                btnRunStartup, btnDiagnosticReport, btnOpenLogFolder,
                new Separator(),
                btnClearDriver, btnClearLogs, btnExit
        );

        return hbox;
    }

    private Button createButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button button = new Button(text);
        button.setOnAction(handler);
        button.setPrefWidth(150);
        button.setMaxWidth(150);
        return button;
    }

    private ScrollPane createLogConsole() {
        logConsole = new TextArea();
        logConsole.setEditable(false);
        logConsole.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; -fx-font-size: 11;");
        logConsole.setWrapText(false);
        
        ScrollPane scrollPane = new ScrollPane(logConsole);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        return scrollPane;
    }

    private HBox createStatusBar() {
        HBox hbox = new HBox(15);
        hbox.setPadding(new Insets(8));
        hbox.setStyle("-fx-background-color: #e0e0e0;");
        hbox.setAlignment(Pos.CENTER_LEFT);

        applicationStatus = new Label(Messages.STATUS_NOT_INITIALIZED);
        applicationStatus.setStyle("-fx-font-weight: bold;");

        Label separator = new Label("|");
        
        statusConfig = createStatusLabel(Messages.STATUS_CONFIG);
        statusLogging = createStatusLabel(Messages.STATUS_LOGGING);
        statusVersion = createStatusLabel(Messages.STATUS_VERSION);
        statusUpdate = createStatusLabel(Messages.STATUS_UPDATE);
        statusDriver = createStatusLabel(Messages.STATUS_DRIVER);
        statusValidation = createStatusLabel(Messages.STATUS_VALIDATION);
        statusInternet = createStatusLabel(Messages.STATUS_INTERNET);

        hbox.getChildren().addAll(
                applicationStatus, separator,
                statusConfig, statusLogging, statusVersion, statusUpdate,
                statusDriver, statusValidation, statusInternet
        );

        return hbox;
    }

    private Label createStatusLabel(String name) {
        Label label = new Label(name + ": --");
        label.setStyle("-fx-font-size: 11;");
        return label;
    }

    private void updateStatus(Label label, String status, boolean isOk) {
        String color = isOk ? "#4CAF50" : "#F44336";
        String statusText = isOk ? Messages.STATUS_OK : Messages.STATUS_ERROR;
        label.setText(status + ": " + statusText);
        label.setStyle("-fx-font-size: 11; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }

    private void updateStatusWarning(Label label, String status) {
        label.setText(status + ": " + Messages.STATUS_WARNING);
        label.setStyle("-fx-font-size: 11; -fx-text-fill: #FF9800; -fx-font-weight: bold;");
    }

    private Tab createEnvironmentTab() {
        Tab tab = new Tab(Messages.TAB_ENVIRONMENT);
        tab.setClosable(false);

        environmentInfo = new TextArea();
        environmentInfo.setEditable(false);
        environmentInfo.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12;");

        refreshEnvironmentInfo();

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        
        Button btnRefresh = new Button(Messages.BTN_REFRESH_ENV);
        btnRefresh.setOnAction(e -> refreshEnvironmentInfo());
        
        vbox.getChildren().addAll(btnRefresh, environmentInfo);
        VBox.setVgrow(environmentInfo, Priority.ALWAYS);

        tab.setContent(vbox);
        return tab;
    }

    private Tab createAboutTab() {
        Tab tab = new Tab(Messages.TAB_ABOUT);
        tab.setClosable(false);

        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(30));
        vbox.setAlignment(Pos.CENTER);

        Label title = new Label(Messages.ABOUT_TITLE);
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");

        Label versionLabel = new Label(Messages.VERSION_PREFIX + ": " + VersionService.getCurrentVersionString());
        versionLabel.setStyle("-fx-font-size: 14;");

        Label desc = new Label(Messages.ABOUT_DESCRIPTION);
        desc.setStyle("-fx-font-size: 12; -fx-text-fill: #666666;");

        Label copyright = new Label(Messages.ABOUT_MODE);
        copyright.setStyle("-fx-font-size: 11; -fx-text-fill: #999999;");

        vbox.getChildren().addAll(title, versionLabel, desc, copyright);

        tab.setContent(vbox);
        return tab;
    }

    private void refreshEnvironmentInfo() {
        StringBuilder sb = new StringBuilder();
        
        // Информация о Java
        sb.append(Messages.ENV_JAVA).append("\n");
        sb.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        sb.append("Java Vendor: ").append(System.getProperty("java.vendor")).append("\n");
        sb.append("Java Home: ").append(System.getProperty("java.home")).append("\n");
        sb.append("Java Arch: ").append(System.getProperty("os.arch")).append("\n");
        
        // Информация об ОС
        sb.append("\n").append(Messages.ENV_OS).append("\n");
        sb.append("Название ОС: ").append(System.getProperty("os.name")).append("\n");
        sb.append("Версия ОС: ").append(System.getProperty("os.version")).append("\n");
        sb.append("Архитектура ОС: ").append(System.getProperty("os.arch")).append("\n");
        
        // Информация о пользователе
        sb.append("\n").append(Messages.ENV_USER).append("\n");
        sb.append("Имя пользователя: ").append(System.getProperty("user.name")).append("\n");
        sb.append("Домашняя папка: ").append(System.getProperty("user.home")).append("\n");
        sb.append("Рабочая папка: ").append(System.getProperty("user.dir")).append("\n");
        sb.append("Local AppData: ").append(System.getenv("LOCALAPPDATA")).append("\n");
        
        // Информация о приложении
        sb.append("\n").append(Messages.ENV_APPLICATION).append("\n");
        sb.append("Версия приложения: ").append(VersionService.getCurrentVersionString()).append("\n");
        
        try {
            UpdateConfig config = UpdateConfig.load();
            sb.append("JSON URL: ").append(config.getJsonUrl()).append("\n");
            sb.append("Канал обновлений: ").append(config.getChannel()).append("\n");
        } catch (Exception e) {
            sb.append(Messages.ENV_CONFIG_ERROR).append("\n");
        }
        
        // Информация о Edge
        sb.append("\n").append(Messages.ENV_EDGE).append("\n");
        EdgeInfo edgeInfo = EdgeDetector.detect();
        if (edgeInfo.isInstalled()) {
            sb.append(Messages.ENV_INSTALLED_YES).append("\n");
            sb.append("Версия: ").append(edgeInfo.getVersion()).append("\n");
            sb.append("Путь: ").append(edgeInfo.getPath()).append("\n");
        } else {
            sb.append(Messages.ENV_NOT_INSTALLED).append("\n");
        }
        
        // Информация о драйвере
        sb.append("\n").append(Messages.ENV_DRIVER).append("\n");
        DriverInfo driverInfo = DriverDetector.detect();
        if (driverInfo.isInstalled()) {
            sb.append(Messages.ENV_INSTALLED_YES).append("\n");
            sb.append("Версия: ").append(driverInfo.getVersion()).append("\n");
            sb.append("Путь: ").append(driverInfo.getPath()).append("\n");
            sb.append("Статус: ").append(DriverService.detectAndCompare()).append("\n");
        } else {
            sb.append(Messages.ENV_NOT_INSTALLED).append("\n");
        }
        
        // Информация о логировании
        sb.append("\n").append(Messages.ENV_LOGGING).append("\n");
        File logDir = new File(System.getProperty("java.io.tmpdir"));
        sb.append("Временная папка: ").append(logDir.getAbsolutePath()).append("\n");
        
        // Тест сети
        sb.append("\n").append(Messages.ENV_NETWORK).append("\n");
        boolean githubReachable = testGitHubConnection();
        sb.append(githubReachable ? Messages.ENV_GITHUB_REACHABLE : Messages.ENV_GITHUB_NOT_REACHABLE).append("\n");
        
        environmentInfo.setText(sb.toString());
    }

    private boolean testGitHubConnection() {
        try {
            URL url = new URL("https://api.github.com");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private void checkUpdates() {
        logger.info(Messages.LOG_DEV_CHECK_UPDATES);
        try {
            UpdateService updateService = new UpdateService();
            com.rmc.model.UpdateCheckResult result = updateService.checkForUpdates();
            if (result != null && result.isSuccess()) {
                logger.info("Проверка обновлений успешна - HTTP статус: {}", result.getHttpStatus());
                updateStatus(statusUpdate, Messages.STATUS_UPDATE, true);
            } else {
                logger.warn("Проверка обновлений завершена с ошибками: {}", result != null ? result.getErrorMessage() : "Неизвестно");
                updateStatusWarning(statusUpdate, Messages.STATUS_UPDATE);
            }
        } catch (Exception e) {
            logger.error("Не удалось проверить обновления", e);
            updateStatus(statusUpdate, Messages.STATUS_UPDATE, false);
        }
    }

    private void checkDriver() {
        logger.info(Messages.LOG_DEV_CHECK_DRIVER);
        try {
            EdgeInfo edgeInfo = EdgeDetector.detect();
            DriverInfo driverInfo = DriverDetector.detect();
            DriverStatus status = DriverService.detectAndCompare();
            
            String edgeStatus = edgeInfo.isInstalled() ? "Установлен" : "Не установлен";
            String driverStatus = driverInfo.isInstalled() ? "Установлен" : "Не установлен";
            logger.info("Edge: {} ({})", edgeStatus, edgeInfo.getVersion());
            logger.info("Драйвер: {} ({})", driverStatus, driverInfo.getVersion());
            logger.info("Статус: {}", status);
            
            updateStatus(statusDriver, Messages.STATUS_DRIVER, driverInfo.isInstalled());
            updateStatus(statusValidation, Messages.STATUS_VALIDATION, status == DriverStatus.MATCH);
        } catch (Exception e) {
            logger.error("Не удалось проверить драйвер", e);
            updateStatus(statusDriver, Messages.STATUS_DRIVER, false);
        }
    }

    private void downloadDriver() {
        logger.info(Messages.LOG_DEV_DOWNLOAD_DRIVER);
        try {
            WebDriverManagerAdapter.DriverDownloadResult result = WebDriverManagerAdapter.downloadDriver();
            if (result.isSuccess()) {
                logger.info(Messages.LOG_WDM_SUCCESS);
            } else {
                logger.warn("Загрузка не удалась: {}", result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("Не удалось загрузить драйвер", e);
        }
    }

    private void validateDriver() {
        logger.info(Messages.LOG_DEV_VALIDATE_DRIVER);
        try {
            EdgeInfo edgeInfo = EdgeDetector.detect();
            DriverInfo driverInfo = DriverDetector.detect();
            
            if (!edgeInfo.isInstalled() || !driverInfo.isInstalled()) {
                logger.warn(Messages.LOG_CANNOT_VALIDATE);
                updateStatus(statusValidation, Messages.STATUS_VALIDATION, false);
                return;
            }
            
            ValidationResult result = DriverValidator.validate(
                    Paths.get(driverInfo.getPath()),
                    edgeInfo.getVersion(),
                    driverInfo.getVersion()
            );
            
            if (result.isValid()) {
                logger.info("Проверка УСПЕШНА: {}", result.getMessage());
                updateStatus(statusValidation, Messages.STATUS_VALIDATION, true);
            } else {
                logger.warn("Проверка ОШИБКА: {}", result.getMessage());
                updateStatus(statusValidation, Messages.STATUS_VALIDATION, false);
            }
        } catch (Exception e) {
            logger.error("Не удалось проверить драйвер", e);
            updateStatus(statusValidation, Messages.STATUS_VALIDATION, false);
        }
    }

    private void runStartupSequence() {
        logger.info(Messages.LOG_DEV_RUN_STARTUP);
        try {
            ApplicationLifecycle lifecycle = new ApplicationLifecycle();
            lastReport = lifecycle.start();
            
            // Обновляем индикаторы статуса
            updateStatus(statusConfig, Messages.STATUS_CONFIG, lastReport.isConfigLoaded());
            updateStatus(statusLogging, Messages.STATUS_LOGGING, lastReport.isLoggingInitialized());
            updateStatus(statusVersion, Messages.STATUS_VERSION, lastReport.isVersionEngineLoaded());
            updateStatus(statusUpdate, Messages.STATUS_UPDATE, lastReport.isUpdateCheckCompleted());
            updateStatus(statusDriver, Messages.STATUS_DRIVER, lastReport.isDriverDetected());
            
            if (lastReport.getValidationStatus() != null) {
                updateStatus(statusValidation, Messages.STATUS_VALIDATION, 
                        lastReport.getValidationStatus() == ValidationStatus.VALID);
            }
            
            updateStatus(statusInternet, Messages.STATUS_INTERNET, testGitHubConnection());
            
            String appStatus = lastReport.isApplicationReady() ? Messages.STATUS_READY : Messages.STATUS_NOT_READY;
            applicationStatus.setText(appStatus);
            
            logger.info(Messages.LOG_STARTUP_COMPLETE);
        } catch (Exception e) {
            logger.error(Messages.LOG_STARTUP_FAILED, e);
            applicationStatus.setText(Messages.STATUS_FAILED);
        }
    }

    private void createDiagnosticReport() {
        logger.info(Messages.LOG_DEV_CREATE_REPORT);
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(Messages.REPORT_TITLE).append("\n\n");
            sb.append(String.format(Messages.REPORT_GENERATED, java.time.LocalDateTime.now())).append("\n\n");
            
            // Версия
            sb.append("=== Версия приложения ===\n");
            sb.append("Текущая версия: ").append(VersionService.getCurrentVersionString()).append("\n");
            try {
                UpdateConfig config = UpdateConfig.load();
                sb.append("JSON URL: ").append(config.getJsonUrl()).append("\n");
                sb.append("Канал обновлений: ").append(config.getChannel()).append("\n");
            } catch (Exception e) {
                sb.append(Messages.REPORT_CONFIG_ERROR).append("\n");
            }
            
            // Edge
            sb.append("\n=== Microsoft Edge ===\n");
            EdgeInfo edgeInfo = EdgeDetector.detect();
            sb.append("Установлен: ").append(edgeInfo.isInstalled() ? "Да" : "Нет").append("\n");
            if (edgeInfo.isInstalled()) {
                sb.append("Версия: ").append(edgeInfo.getVersion()).append("\n");
                sb.append("Путь: ").append(edgeInfo.getPath()).append("\n");
            }
            
            // Драйвер
            sb.append("\n=== Edge WebDriver ===\n");
            DriverInfo driverInfo = DriverDetector.detect();
            sb.append("Установлен: ").append(driverInfo.isInstalled() ? "Да" : "Нет").append("\n");
            if (driverInfo.isInstalled()) {
                sb.append("Версия: ").append(driverInfo.getVersion()).append("\n");
                sb.append("Путь: ").append(driverInfo.getPath()).append("\n");
                sb.append("Статус: ").append(DriverService.detectAndCompare()).append("\n");
            }
            
            // Последний отчёт о запуске
            if (lastReport != null) {
                sb.append("\n").append(Messages.REPORT_SUMMARY).append("\n");
                sb.append(lastReport.toSummary());
            }
            
            // Информация о системе
            sb.append("\n").append(Messages.REPORT_SYS_INFO).append("\n");
            sb.append("Java версия: ").append(System.getProperty("java.version")).append("\n");
            sb.append("ОС: ").append(System.getProperty("os.name"))
              .append(" ").append(System.getProperty("os.version")).append("\n");
            sb.append("Архитектура: ").append(System.getProperty("os.arch")).append("\n");
            
            // Показываем в диалоге
            TextArea textArea = new TextArea(sb.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            
            Stage dialog = new Stage();
            dialog.setTitle("Диагностический отчёт");
            dialog.setWidth(600);
            dialog.setHeight(500);
            
            Button btnCopy = new Button(Messages.BTN_COPY_CLIPBOARD);
            btnCopy.setOnAction(e -> {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(sb.toString());
                clipboard.setContent(content);
                logger.info(Messages.LOG_REPORT_COPIED);
            });
            
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(10));
            vbox.getChildren().addAll(new ScrollPane(textArea), btnCopy);
            VBox.setVgrow(textArea, Priority.ALWAYS);
            
            dialog.setScene(new Scene(vbox));
            dialog.show();
            
        } catch (Exception e) {
            logger.error("Не удалось создать диагностический отчёт", e);
        }
    }

    private void openLogFolder() {
        logger.info(Messages.LOG_DEV_OPEN_LOGS);
        try {
            File logDir = new File(System.getProperty("java.io.tmpdir"));
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(logDir);
            }
        } catch (IOException e) {
            logger.error("Не удалось открыть папку логов", e);
        }
    }

    private void clearDriver() {
        logger.info(Messages.LOG_DEV_CLEAR_DRIVER);
        try {
            // Очищаем драйвер через адаптер WebDriverManager
            WebDriverManagerAdapter.clearDriverCache();
            logger.info(Messages.LOG_DRIVER_CLEARED);
        } catch (Exception e) {
            logger.error(Messages.LOG_CLEAR_DRIVER_FAILED, e);
        }
    }

    private void clearLogs() {
        logger.info(Messages.LOG_DEV_CLEAR_LOGS);
        UiLogAppender.clear();
        logger.info(Messages.LOG_LOGS_CLEARED);
    }

    private void exit() {
        logger.info(Messages.LOG_DEV_EXIT);
        UiLogAppender.detach();
        stage.close();
    }

    /**
     * Показать окно разработчика.
     */
    public void show() {
        stage.show();
    }

    /**
     * Закрыть окно разработчика.
     */
    public void close() {
        exit();
    }
}
