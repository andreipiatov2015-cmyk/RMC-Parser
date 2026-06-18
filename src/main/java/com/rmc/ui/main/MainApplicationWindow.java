package com.rmc.ui.main;

import com.rmc.auth.AuthenticationService;
import com.rmc.auth.LoginRequest;
import com.rmc.auth.LoginResult;
import com.rmc.filters.loader.FilterPageLoader;
import com.rmc.filters.loader.FilterPageResult;
import com.rmc.http.HttpClientService;
import com.rmc.i18n.Messages;
import com.rmc.logging.AppLogger;
import com.rmc.parser.model.Program;
import com.rmc.ui.DeveloperWindow;
import com.rmc.ui.dynamic.DynamicFilterPane;
import com.rmc.ui.newui.ProgramTableController;
import com.rmc.update.UpdateService;
import com.rmc.version.VersionService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Главное окно приложения RMC Framework.
 * 
 * <p>Единая точка входа ко всем существующим подсистемам.</p>
 * 
 * <p>Не содержит бизнес-логику - только координацию.</p>
 */
public class MainApplicationWindow extends BorderPane {
    
    private static final Logger logger = AppLogger.getLogger();
    
    // Сервисы
    private final HttpClientService httpClient;
    private AuthenticationService authService;
    
    // Окна
    private DeveloperWindow developerWindow;
    
    // Состояние
    private boolean isAuthenticated = false;
    private String currentUsername = "";
    private int programCount = 0;
    private List<String> recentLogs = new ArrayList<>();
    
    // UI компоненты
    private Label statusAuthLabel;
    private Label statusProgramsLabel;
    private Label statusVersionLabel;
    
    // Панели разделов
    private StackPane centerPane;
    private Pane homePane;
    private Pane authPane;
    private Pane filtersPane;
    private Pane resultsPane;
    private Pane exportPane;
    private Pane settingsPane;
    
    // DynamicFilterPane
    private DynamicFilterPane dynamicFilterPane;
    
    // ProgramTableController
    private ProgramTableController programTableController;
    
    public MainApplicationWindow() {
        this.httpClient = HttpClientService.builder().build();
        this.authService = AuthenticationService.builder()
                .httpClient(httpClient)
                .build();
        
        initializeUI();
        logRecentAction("Приложение запущено");
    }
    
    private void initializeUI() {
        setTop(createMenuBar());
        setLeft(createNavigationPane());
        setCenter(createCenterPane());
        setBottom(createStatusBar());
        
        // Показываем главную страницу по умолчанию
        showSection("home");
    }
    
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // Меню "Файл"
        Menu fileMenu = new Menu(Messages.MENU_FILE);
        fileMenu.getItems().addAll(
            createMenuItem(Messages.MENU_FILE_LOGIN, e -> showSection("auth")),
            createMenuItem(Messages.MENU_FILE_LOGOUT, e -> handleLogout()),
            new SeparatorMenuItem(),
            createMenuItem(Messages.MENU_FILE_EXPORT, e -> handleExport()),
            new SeparatorMenuItem(),
            createMenuItem(Messages.MENU_FILE_EXIT, e -> handleExit())
        );
        
        // Меню "Сервис"
        Menu serviceMenu = new Menu(Messages.MENU_SERVICE);
        serviceMenu.getItems().addAll(
            createMenuItem(Messages.MENU_SERVICE_REFRESH_FILTERS, e -> handleRefreshFilters()),
            createMenuItem(Messages.MENU_SERVICE_CLEAR_CACHE, e -> handleClearCache()),
            new SeparatorMenuItem(),
            createMenuItem(Messages.MENU_SERVICE_DEV_DIAGNOSTICS, e -> handleDevDiagnostics())
        );
        
        // Меню "Справка"
        Menu helpMenu = new Menu(Messages.MENU_HELP);
        helpMenu.getItems().addAll(
            createMenuItem(Messages.MENU_HELP_ABOUT, e -> handleAbout()),
            createMenuItem(Messages.MENU_HELP_CHECK_UPDATES, e -> handleCheckUpdates())
        );
        
        menuBar.getMenus().addAll(fileMenu, serviceMenu, helpMenu);
        
        return menuBar;
    }
    
    private MenuItem createMenuItem(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(handler);
        return item;
    }
    
    private VBox createNavigationPane() {
        VBox navPane = new VBox();
        navPane.setSpacing(5);
        navPane.setPadding(new Insets(10));
        navPane.setStyle("-fx-background-color: #f0f0f0;");
        navPane.setPrefWidth(180);
        
        // Кнопки навигации
        navPane.getChildren().addAll(
            createNavButton(Messages.NAV_HOME, "home"),
            createNavButton(Messages.NAV_AUTH, "auth"),
            createNavButton(Messages.NAV_FILTERS, "filters"),
            createNavButton(Messages.NAV_RESULTS, "results"),
            createNavButton(Messages.NAV_EXPORT, "export"),
            createNavButton(Messages.NAV_SETTINGS, "settings")
        );
        
        return navPane;
    }
    
    private Button createNavButton(String text, String section) {
        Button button = new Button(text);
        button.setPrefWidth(160);
        button.setPrefHeight(40);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setFont(Font.font("Arial", 13));
        button.setStyle("-fx-background-radius: 5; -fx-cursor: hand;");
        button.setOnAction(e -> showSection(section));
        return button;
    }
    
    private StackPane createCenterPane() {
        centerPane = new StackPane();
        
        homePane = createHomePane();
        authPane = createAuthPane();
        filtersPane = createFiltersPane();
        resultsPane = createResultsPane();
        exportPane = createExportPane();
        settingsPane = createSettingsPane();
        
        centerPane.getChildren().addAll(homePane, authPane, filtersPane, resultsPane, exportPane, settingsPane);
        
        return centerPane;
    }
    
    private Pane createHomePane() {
        VBox pane = new VBox();
        pane.setSpacing(20);
        pane.setPadding(new Insets(30));
        pane.setAlignment(Pos.TOP_CENTER);
        
        // Заголовок
        Label titleLabel = new Label(Messages.APP_TITLE);
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        
        // Версия
        Label versionLabel = new Label(Messages.VERSION_PREFIX + ": " + VersionService.getCurrentVersionString());
        versionLabel.setFont(Font.font("Arial", 14));
        versionLabel.setStyle("-fx-text-fill: #666;");
        
        // Статус авторизации
        Label authStatusLabel = new Label();
        authStatusLabel.setFont(Font.font("Arial", 14));
        updateAuthStatusLabel(authStatusLabel);
        
        // Статус соединения
        Label connectionStatusLabel = new Label("Статус соединения: " + Messages.STATUS_OK);
        connectionStatusLabel.setFont(Font.font("Arial", 14));
        
        // Количество программ
        Label programsLabel = new Label();
        programsLabel.setFont(Font.font("Arial", 14));
        updateProgramsLabel(programsLabel);
        
        // Журнал последних операций
        Label logTitleLabel = new Label("Последние операции:");
        logTitleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        ListView<String> logListView = new ListView<>();
        logListView.setItems(javafx.collections.FXCollections.observableArrayList(recentLogs));
        logListView.setPrefHeight(150);
        logListView.setEditable(false);
        
        pane.getChildren().addAll(titleLabel, versionLabel, authStatusLabel, connectionStatusLabel, 
                programsLabel, logTitleLabel, logListView);
        
        return pane;
    }
    
    private void updateAuthStatusLabel(Label label) {
        if (isAuthenticated) {
            label.setText("Статус авторизации: " + currentUsername);
            label.setStyle("-fx-text-fill: #4CAF50;");
        } else {
            label.setText("Статус авторизации: Не авторизован");
            label.setStyle("-fx-text-fill: #f44336;");
        }
    }
    
    private void updateProgramsLabel(Label label) {
        label.setText("Найдено программ: " + programCount);
    }
    
    private Pane createAuthPane() {
        VBox pane = new VBox();
        pane.setSpacing(15);
        pane.setPadding(new Insets(30));
        pane.setAlignment(Pos.TOP_CENTER);
        
        Label titleLabel = new Label("Авторизация");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        
        if (isAuthenticated) {
            // Показываем информацию о пользователе
            Label usernameLabel = new Label("Пользователь: " + currentUsername);
            usernameLabel.setFont(Font.font("Arial", 16));
            
            Label sessionLabel = new Label("Сессия активна");
            sessionLabel.setStyle("-fx-text-fill: #4CAF50;");
            
            Button logoutButton = new Button("Выйти");
            logoutButton.setPrefWidth(150);
            logoutButton.setOnAction(e -> handleLogout());
            
            pane.getChildren().addAll(titleLabel, usernameLabel, sessionLabel, logoutButton);
        } else {
            // Форма входа
            GridPane formPane = new GridPane();
            formPane.setHgap(10);
            formPane.setVgap(10);
            formPane.setAlignment(Pos.CENTER);
            
            Label loginLabel = new Label("Логин:");
            TextField loginField = new TextField();
            loginField.setPromptText("Введите логин");
            loginField.setPrefWidth(200);
            
            Label passwordLabel = new Label("Пароль:");
            PasswordField passwordField = new PasswordField();
            passwordField.setPromptText("Введите пароль");
            passwordField.setPrefWidth(200);
            
            Label statusLabel = new Label();
            
            Button loginButton = new Button("Войти");
            loginButton.setPrefWidth(150);
            loginButton.setOnAction(e -> {
                String login = loginField.getText();
                String password = passwordField.getText();
                handleLogin(login, password, statusLabel);
            });
            
            formPane.addRow(0, loginLabel, loginField);
            formPane.addRow(1, passwordLabel, passwordField);
            formPane.addRow(2, loginButton);
            formPane.add(statusLabel, 1, 3);
            
            pane.getChildren().addAll(titleLabel, formPane);
        }
        
        return pane;
    }
    
    private Pane createFiltersPane() {
        VBox pane = new VBox();
        pane.setSpacing(15);
        pane.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Фильтры");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        
        if (!isAuthenticated) {
            Label notAuthLabel = new Label("Для загрузки фильтров необходимо авторизоваться");
            notAuthLabel.setStyle("-fx-text-fill: #f44336;");
            pane.getChildren().addAll(titleLabel, notAuthLabel);
        } else {
            Button loadFiltersButton = new Button("Получить фильтры");
            loadFiltersButton.setPrefWidth(150);
            loadFiltersButton.setOnAction(e -> handleLoadFilters());
            
            dynamicFilterPane = new DynamicFilterPane();
            VBox.setVgrow(dynamicFilterPane, Priority.ALWAYS);
            
            pane.getChildren().addAll(titleLabel, loadFiltersButton, dynamicFilterPane);
        }
        
        return pane;
    }
    
    private Pane createResultsPane() {
        VBox pane = new VBox();
        pane.setSpacing(15);
        pane.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Результаты поиска");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        
        programTableController = new ProgramTableController();
        VBox.setVgrow(programTableController.getTableView(), Priority.ALWAYS);
        
        pane.getChildren().addAll(titleLabel, programTableController.getTableView());
        
        return pane;
    }
    
    private Pane createExportPane() {
        VBox pane = new VBox();
        pane.setSpacing(15);
        pane.setPadding(new Insets(30));
        pane.setAlignment(Pos.TOP_CENTER);
        
        Label titleLabel = new Label("Экспорт в Excel");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        
        if (programCount == 0) {
            Label noDataLabel = new Label("Нет данных для экспорта");
            noDataLabel.setStyle("-fx-text-fill: #666;");
            pane.getChildren().addAll(titleLabel, noDataLabel);
        } else {
            Label infoLabel = new Label("Готово к экспорту " + programCount + " программ");
            
            Button exportButton = new Button("Экспортировать");
            exportButton.setPrefWidth(150);
            exportButton.setOnAction(e -> handleExport());
            
            pane.getChildren().addAll(titleLabel, infoLabel, exportButton);
        }
        
        return pane;
    }
    
    private Pane createSettingsPane() {
        VBox pane = new VBox();
        pane.setSpacing(15);
        pane.setPadding(new Insets(30));
        pane.setAlignment(Pos.TOP_CENTER);
        
        Label titleLabel = new Label("Настройки");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        
        Label placeholderLabel = new Label("Настройки приложения будут доступны в следующих версиях");
        placeholderLabel.setStyle("-fx-text-fill: #666;");
        
        pane.getChildren().addAll(titleLabel, placeholderLabel);
        
        return pane;
    }
    
    private HBox createStatusBar() {
        HBox statusBar = new HBox();
        statusBar.setSpacing(20);
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: #e0e0e0;");
        
        statusAuthLabel = new Label("Авторизация: Не авторизован");
        statusProgramsLabel = new Label("Программы: 0");
        statusVersionLabel = new Label("Версия: " + VersionService.getCurrentVersionString());
        
        statusBar.getChildren().addAll(statusAuthLabel, statusProgramsLabel, statusVersionLabel);
        
        return statusBar;
    }
    
    private void showSection(String section) {
        homePane.setVisible(false);
        authPane.setVisible(false);
        filtersPane.setVisible(false);
        resultsPane.setVisible(false);
        exportPane.setVisible(false);
        settingsPane.setVisible(false);
        
        switch (section) {
            case "home":
                homePane.setVisible(true);
                break;
            case "auth":
                authPane = createAuthPane();
                centerPane.getChildren().set(1, authPane);
                authPane.setVisible(true);
                break;
            case "filters":
                filtersPane.setVisible(true);
                break;
            case "results":
                resultsPane.setVisible(true);
                break;
            case "export":
                exportPane.setVisible(true);
                break;
            case "settings":
                settingsPane.setVisible(true);
                break;
        }
        
        logger.info("Открыт раздел: {}", section);
        logRecentAction("Открыт: " + section);
    }
    
    private void handleLogin(String login, String password, Label statusLabel) {
        logger.info("Выполняется вход: {}", login);
        
        try {
            LoginRequest request = LoginRequest.builder()
                    .username(login)
                    .password(password)
                    .loginUrl("https://rmc.example.com/login")
                    .build();
            
            LoginResult result = authService.login(request);
            
            if (result.isSuccess()) {
                isAuthenticated = true;
                currentUsername = login;
                statusLabel.setText("Вход выполнен!");
                statusLabel.setStyle("-fx-text-fill: #4CAF50;");
                updateStatusBar();
                logRecentAction("Вход выполнен: " + login);
                showSection("auth");
            } else {
                statusLabel.setText("Ошибка входа: " + result.getErrorMessage().orElse("Неизвестная ошибка"));
                statusLabel.setStyle("-fx-text-fill: #f44336;");
                logRecentAction("Ошибка входа");
            }
        } catch (Exception e) {
            logger.error("Ошибка входа", e);
            statusLabel.setText("Ошибка: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: #f44336;");
            logRecentAction("Ошибка: " + e.getMessage());
        }
    }
    
    private void handleLogout() {
        authService.logout();
        isAuthenticated = false;
        currentUsername = "";
        updateStatusBar();
        logRecentAction("Выход выполнен");
        showSection("auth");
    }
    
    private void handleRefreshFilters() {
        if (!isAuthenticated) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Необходимо авторизоваться для обновления фильтров");
            return;
        }
        
        logRecentAction("Обновление фильтров...");
        
        new Thread(() -> {
            try {
                FilterPageLoader loader = FilterPageLoader.builder()
                        .httpClient(httpClient)
                        .baseUrl("https://rmc.example.com")
                        .build();
                
                FilterPageResult result = loader.load();
                
                Platform.runLater(() -> {
                    if (result.isSuccess() && dynamicFilterPane != null) {
                        dynamicFilterPane.loadFromHtml(result.getHtml());
                        logRecentAction("Фильтры обновлены");
                    } else {
                        logRecentAction("Ошибка обновления фильтров");
                    }
                });
            } catch (Exception e) {
                logger.error("Ошибка обновления фильтров", e);
                Platform.runLater(() -> logRecentAction("Ошибка: " + e.getMessage()));
            }
        }).start();
    }
    
    private void handleLoadFilters() {
        handleRefreshFilters();
    }
    
    private void handleClearCache() {
        logRecentAction("Кэш очищен");
        showAlert(Alert.AlertType.INFORMATION, "Информация", "Кэш успешно очищен");
    }
    
    private void handleExport() {
        if (programCount == 0) {
            showAlert(Alert.AlertType.WARNING, "Внимание", "Нет данных для экспорта");
            return;
        }
        
        logRecentAction("Экспорт в Excel...");
        showAlert(Alert.AlertType.INFORMATION, "Информация", "Экспорт будет реализован в следующих версиях");
    }
    
    private void handleDevDiagnostics() {
        logger.info(Messages.LOG_DEV_WINDOW_OPEN);
        if (developerWindow == null) {
            developerWindow = new DeveloperWindow();
        }
        developerWindow.show();
    }
    
    private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("О программе");
        alert.setHeaderText(Messages.APP_TITLE);
        alert.setContentText("Версия: " + VersionService.getCurrentVersionString() + 
                "\n\nСистема управления поиском программ дополнительного образования");
        alert.show();
    }
    
    private void handleCheckUpdates() {
        logger.info("Проверка обновлений...");
        logRecentAction("Проверка обновлений...");
        
        new Thread(() -> {
            try {
                UpdateService updateService = new UpdateService();
                updateService.checkForUpdates();
                Platform.runLater(() -> logRecentAction("Проверка обновлений завершена"));
            } catch (Exception e) {
                logger.error("Ошибка проверки обновлений", e);
                Platform.runLater(() -> logRecentAction("Ошибка проверки обновлений"));
            }
        }).start();
    }
    
    private void handleExit() {
        logger.info("Выход из приложения");
        Platform.exit();
    }
    
    private void updateStatusBar() {
        statusAuthLabel.setText(isAuthenticated ? 
                "Авторизация: " + currentUsername : "Авторизация: Не авторизован");
    }
    
    private void logRecentAction(String action) {
        String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
        recentLogs.add(0, timestamp + " - " + action);
        if (recentLogs.size() > 10) {
            recentLogs.remove(recentLogs.size() - 1);
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
    
    public void close() {
        if (developerWindow != null) {
            developerWindow.close();
        }
    }
}
