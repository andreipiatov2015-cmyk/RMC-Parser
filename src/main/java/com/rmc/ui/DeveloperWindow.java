package com.rmc.ui;

import com.rmc.app.ApplicationLifecycle;
import com.rmc.app.LifecycleReport;
import com.rmc.config.UpdateConfig;
import com.rmc.download.DownloadResult;
import com.rmc.download.DownloadService;
import com.rmc.driver.DriverDetector;
import com.rmc.driver.DriverInfo;
import com.rmc.driver.DriverService;
import com.rmc.driver.DriverStatus;
import com.rmc.driver.EdgeDetector;
import com.rmc.driver.EdgeInfo;
import com.rmc.driver.validation.DriverValidator;
import com.rmc.driver.validation.ValidationResult;
import com.rmc.driver.validation.ValidationStatus;
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
        stage.setTitle("RMC Framework - Developer Diagnostics");
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

        // Attach log appender
        Platform.runLater(() -> {
            UiLogAppender.attach(logConsole);
            logger.info("Developer Diagnostics Window opened");
        });
    }

    private Tab createDiagnosticsTab() {
        Tab tab = new Tab("Diagnostics");
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

        Button btnCheckUpdates = createButton("Check Updates", e -> checkUpdates());
        Button btnCheckDriver = createButton("Check Driver", e -> checkDriver());
        Button btnDownloadDriver = createButton("Download Driver", e -> downloadDriver());
        Button btnValidateDriver = createButton("Validate Driver", e -> validateDriver());
        Button btnRunStartup = createButton("Run Startup Sequence", e -> runStartupSequence());
        Button btnDiagnosticReport = createButton("Create Diagnostic Report", e -> createDiagnosticReport());
        Button btnOpenLogFolder = createButton("Open Log Folder", e -> openLogFolder());
        Button btnClearDriver = createButton("Clear Driver", e -> clearDriver());
        Button btnClearLogs = createButton("Clear Logs", e -> clearLogs());
        Button btnExit = createButton("Exit", e -> exit());

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
        button.setPrefWidth(130);
        button.setMaxWidth(130);
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

        applicationStatus = new Label("Status: Not Initialized");
        applicationStatus.setStyle("-fx-font-weight: bold;");

        Label separator = new Label("|");
        
        statusConfig = createStatusLabel("Config");
        statusLogging = createStatusLabel("Logging");
        statusVersion = createStatusLabel("Version");
        statusUpdate = createStatusLabel("Update");
        statusDriver = createStatusLabel("Driver");
        statusValidation = createStatusLabel("Validation");
        statusInternet = createStatusLabel("Internet");

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
        label.setText(status + ": " + (isOk ? "OK" : "ERROR"));
        label.setStyle("-fx-font-size: 11; -fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }

    private void updateStatusWarning(Label label, String status) {
        label.setText(status + ": WARNING");
        label.setStyle("-fx-font-size: 11; -fx-text-fill: #FF9800; -fx-font-weight: bold;");
    }

    private Tab createEnvironmentTab() {
        Tab tab = new Tab("Environment");
        tab.setClosable(false);

        environmentInfo = new TextArea();
        environmentInfo.setEditable(false);
        environmentInfo.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12;");

        refreshEnvironmentInfo();

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        
        Button btnRefresh = new Button("Refresh Environment Info");
        btnRefresh.setOnAction(e -> refreshEnvironmentInfo());
        
        vbox.getChildren().addAll(btnRefresh, environmentInfo);
        VBox.setVgrow(environmentInfo, Priority.ALWAYS);

        tab.setContent(vbox);
        return tab;
    }

    private Tab createAboutTab() {
        Tab tab = new Tab("About");
        tab.setClosable(false);

        VBox vbox = new VBox(20);
        vbox.setPadding(new Insets(30));
        vbox.setAlignment(Pos.CENTER);

        Label title = new Label("RMC Framework");
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");

        Label versionLabel = new Label("Version: " + VersionService.getCurrentVersionString());
        versionLabel.setStyle("-fx-font-size: 14;");

        Label desc = new Label("Microsoft Edge WebDriver Management Framework");
        desc.setStyle("-fx-font-size: 12; -fx-text-fill: #666666;");

        Label copyright = new Label("Developer Diagnostics Mode");
        copyright.setStyle("-fx-font-size: 11; -fx-text-fill: #999999;");

        vbox.getChildren().addAll(title, versionLabel, desc, copyright);

        tab.setContent(vbox);
        return tab;
    }

    private void refreshEnvironmentInfo() {
        StringBuilder sb = new StringBuilder();
        
        // Java Info
        sb.append("=== Java Environment ===\n");
        sb.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        sb.append("Java Vendor: ").append(System.getProperty("java.vendor")).append("\n");
        sb.append("Java Home: ").append(System.getProperty("java.home")).append("\n");
        sb.append("Java Arch: ").append(System.getProperty("os.arch")).append("\n");
        
        // OS Info
        sb.append("\n=== Operating System ===\n");
        sb.append("OS Name: ").append(System.getProperty("os.name")).append("\n");
        sb.append("OS Version: ").append(System.getProperty("os.version")).append("\n");
        sb.append("OS Arch: ").append(System.getProperty("os.arch")).append("\n");
        
        // User Info
        sb.append("\n=== User Environment ===\n");
        sb.append("User Name: ").append(System.getProperty("user.name")).append("\n");
        sb.append("User Home: ").append(System.getProperty("user.home")).append("\n");
        sb.append("User Dir: ").append(System.getProperty("user.dir")).append("\n");
        sb.append("Local AppData: ").append(System.getenv("LOCALAPPDATA")).append("\n");
        
        // Application Info
        sb.append("\n=== Application ===\n");
        sb.append("Application Version: ").append(VersionService.getCurrentVersionString()).append("\n");
        
        try {
            UpdateConfig config = UpdateConfig.load();
            sb.append("JSON URL: ").append(config.getJsonUrl()).append("\n");
            sb.append("Update Channel: ").append(config.getChannel()).append("\n");
        } catch (Exception e) {
            sb.append("Configuration: Error loading\n");
        }
        
        // Edge Info
        sb.append("\n=== Microsoft Edge ===\n");
        EdgeInfo edgeInfo = EdgeDetector.detect();
        if (edgeInfo.isInstalled()) {
            sb.append("Installed: Yes\n");
            sb.append("Version: ").append(edgeInfo.getVersion()).append("\n");
            sb.append("Path: ").append(edgeInfo.getPath()).append("\n");
        } else {
            sb.append("Installed: No\n");
        }
        
        // Driver Info
        sb.append("\n=== Edge WebDriver ===\n");
        DriverInfo driverInfo = DriverDetector.detect();
        if (driverInfo.isInstalled()) {
            sb.append("Installed: Yes\n");
            sb.append("Version: ").append(driverInfo.getVersion()).append("\n");
            sb.append("Path: ").append(driverInfo.getPath()).append("\n");
            sb.append("Status: ").append(DriverService.detectAndCompare()).append("\n");
        } else {
            sb.append("Installed: No\n");
        }
        
        // Log Directory
        sb.append("\n=== Logging ===\n");
        File logDir = new File(System.getProperty("java.io.tmpdir"));
        sb.append("Temp Directory: ").append(logDir.getAbsolutePath()).append("\n");
        
        // Network Test
        sb.append("\n=== Network ===\n");
        boolean githubReachable = testGitHubConnection();
        sb.append("GitHub API: ").append(githubReachable ? "Reachable" : "Not Reachable").append("\n");
        
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
        logger.info("Developer Action: Checking for updates...");
        try {
            UpdateService updateService = new UpdateService();
            com.rmc.model.UpdateCheckResult result = updateService.checkForUpdates();
            if (result != null && result.isSuccess()) {
                logger.info("Update check successful - HTTP Status: {}", result.getHttpStatus());
                updateStatus(statusUpdate, "Update", true);
            } else {
                logger.warn("Update check completed with errors: {}", result != null ? result.getErrorMessage() : "Unknown");
                updateStatusWarning(statusUpdate, "Update");
            }
        } catch (Exception e) {
            logger.error("Failed to check updates", e);
            updateStatus(statusUpdate, "Update", false);
        }
    }

    private void checkDriver() {
        logger.info("Developer Action: Checking driver...");
        try {
            EdgeInfo edgeInfo = EdgeDetector.detect();
            DriverInfo driverInfo = DriverDetector.detect();
            DriverStatus status = DriverService.detectAndCompare();
            
            logger.info("Edge: {} ({})", edgeInfo.isInstalled() ? "Installed" : "Not Installed", edgeInfo.getVersion());
            logger.info("Driver: {} ({})", driverInfo.isInstalled() ? "Installed" : "Not Installed", driverInfo.getVersion());
            logger.info("Status: {}", status);
            
            updateStatus(statusDriver, "Driver", driverInfo.isInstalled());
            updateStatus(statusValidation, "Validation", status == DriverStatus.MATCH);
        } catch (Exception e) {
            logger.error("Failed to check driver", e);
            updateStatus(statusDriver, "Driver", false);
        }
    }

    private void downloadDriver() {
        logger.info("Developer Action: Downloading driver...");
        try {
            DownloadResult result = DownloadService.downloadDriver();
            if (result.isSuccess()) {
                logger.info("Download successful: {}", result.getDriverPath());
            } else {
                logger.warn("Download failed: {}", result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error("Failed to download driver", e);
        }
    }

    private void validateDriver() {
        logger.info("Developer Action: Validating driver...");
        try {
            EdgeInfo edgeInfo = EdgeDetector.detect();
            DriverInfo driverInfo = DriverDetector.detect();
            
            if (!edgeInfo.isInstalled() || !driverInfo.isInstalled()) {
                logger.warn("Cannot validate: Edge or Driver not installed");
                updateStatus(statusValidation, "Validation", false);
                return;
            }
            
            ValidationResult result = DriverValidator.validate(
                    Paths.get(driverInfo.getPath()),
                    edgeInfo.getVersion(),
                    driverInfo.getVersion()
            );
            
            if (result.isValid()) {
                logger.info("Validation PASSED: {}", result.getMessage());
                updateStatus(statusValidation, "Validation", true);
            } else {
                logger.warn("Validation FAILED: {}", result.getMessage());
                updateStatus(statusValidation, "Validation", false);
            }
        } catch (Exception e) {
            logger.error("Failed to validate driver", e);
            updateStatus(statusValidation, "Validation", false);
        }
    }

    private void runStartupSequence() {
        logger.info("Developer Action: Running startup sequence...");
        try {
            ApplicationLifecycle lifecycle = new ApplicationLifecycle();
            lastReport = lifecycle.start();
            
            // Update status indicators
            updateStatus(statusConfig, "Config", lastReport.isConfigLoaded());
            updateStatus(statusLogging, "Logging", lastReport.isLoggingInitialized());
            updateStatus(statusVersion, "Version", lastReport.isVersionEngineLoaded());
            updateStatus(statusUpdate, "Update", lastReport.isUpdateCheckCompleted());
            updateStatus(statusDriver, "Driver", lastReport.isDriverDetected());
            
            if (lastReport.getValidationStatus() != null) {
                updateStatus(statusValidation, "Validation", 
                        lastReport.getValidationStatus() == ValidationStatus.VALID);
            }
            
            updateStatus(statusInternet, "Internet", testGitHubConnection());
            
            applicationStatus.setText("Status: " + (lastReport.isApplicationReady() ? "Ready" : "Not Ready"));
            
            logger.info("Startup sequence completed");
        } catch (Exception e) {
            logger.error("Startup sequence failed", e);
            applicationStatus.setText("Status: Failed");
        }
    }

    private void createDiagnosticReport() {
        logger.info("Developer Action: Creating diagnostic report...");
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("=== RMC Framework Diagnostic Report ===\n\n");
            sb.append("Generated: ").append(java.time.LocalDateTime.now()).append("\n\n");
            
            // Version
            sb.append("=== Application Version ===\n");
            sb.append("Current Version: ").append(VersionService.getCurrentVersionString()).append("\n");
            try {
                UpdateConfig config = UpdateConfig.load();
                sb.append("JSON URL: ").append(config.getJsonUrl()).append("\n");
                sb.append("Update Channel: ").append(config.getChannel()).append("\n");
            } catch (Exception e) {
                sb.append("Configuration: Error\n");
            }
            
            // Edge
            sb.append("\n=== Microsoft Edge ===\n");
            EdgeInfo edgeInfo = EdgeDetector.detect();
            sb.append("Installed: ").append(edgeInfo.isInstalled()).append("\n");
            if (edgeInfo.isInstalled()) {
                sb.append("Version: ").append(edgeInfo.getVersion()).append("\n");
                sb.append("Path: ").append(edgeInfo.getPath()).append("\n");
            }
            
            // Driver
            sb.append("\n=== Edge WebDriver ===\n");
            DriverInfo driverInfo = DriverDetector.detect();
            sb.append("Installed: ").append(driverInfo.isInstalled()).append("\n");
            if (driverInfo.isInstalled()) {
                sb.append("Version: ").append(driverInfo.getVersion()).append("\n");
                sb.append("Path: ").append(driverInfo.getPath()).append("\n");
                sb.append("Status: ").append(DriverService.detectAndCompare()).append("\n");
            }
            
            // Last Report
            if (lastReport != null) {
                sb.append("\n=== Last Startup Report ===\n");
                sb.append(lastReport.toSummary());
            }
            
            // System Info
            sb.append("\n=== System Information ===\n");
            sb.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
            sb.append("OS: ").append(System.getProperty("os.name"))
              .append(" ").append(System.getProperty("os.version")).append("\n");
            sb.append("Architecture: ").append(System.getProperty("os.arch")).append("\n");
            
            // Show in dialog
            TextArea textArea = new TextArea(sb.toString());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            
            Stage dialog = new Stage();
            dialog.setTitle("Diagnostic Report");
            dialog.setWidth(600);
            dialog.setHeight(500);
            
            Button btnCopy = new Button("Copy to Clipboard");
            btnCopy.setOnAction(e -> {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(sb.toString());
                clipboard.setContent(content);
                logger.info("Report copied to clipboard");
            });
            
            VBox vbox = new VBox(10);
            vbox.setPadding(new Insets(10));
            vbox.getChildren().addAll(new ScrollPane(textArea), btnCopy);
            VBox.setVgrow(textArea, Priority.ALWAYS);
            
            dialog.setScene(new Scene(vbox));
            dialog.show();
            
        } catch (Exception e) {
            logger.error("Failed to create diagnostic report", e);
        }
    }

    private void openLogFolder() {
        logger.info("Developer Action: Opening log folder...");
        try {
            File logDir = new File(System.getProperty("java.io.tmpdir"));
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(logDir);
            }
        } catch (IOException e) {
            logger.error("Failed to open log folder", e);
        }
    }

    private void clearDriver() {
        logger.info("Developer Action: Clearing driver...");
        try {
            Path driverDir = Paths.get(
                    System.getenv("LOCALAPPDATA") != null ? System.getenv("LOCALAPPDATA") : System.getProperty("user.home"),
                    "RMCParser", "drivers", "edge"
            );
            
            if (Files.exists(driverDir)) {
                Files.walk(driverDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
                logger.info("Driver cleared successfully");
            } else {
                logger.info("No driver to clear");
            }
        } catch (Exception e) {
            logger.error("Failed to clear driver", e);
        }
    }

    private void clearLogs() {
        logger.info("Developer Action: Clearing logs...");
        UiLogAppender.clear();
        logger.info("Logs cleared");
    }

    private void exit() {
        logger.info("Developer Diagnostics Window closed");
        UiLogAppender.detach();
        stage.close();
    }

    /**
     * Show the developer window.
     */
    public void show() {
        stage.show();
    }

    /**
     * Close the developer window.
     */
    public void close() {
        exit();
    }
}
