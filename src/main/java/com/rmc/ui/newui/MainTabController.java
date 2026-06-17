package com.rmc.ui.newui;

import com.rmc.auth.AuthenticationService;
import com.rmc.auth.LoginRequest;
import com.rmc.auth.LoginResult;
import com.rmc.filters.loader.FilterPageLoader;
import com.rmc.filters.parser.FilterParser;
import com.rmc.http.HttpClientService;
import com.rmc.logging.AppLogger;
import com.rmc.parser.ProgramParser;
import com.rmc.parser.model.Program;
import com.rmc.search.SearchRequest;
import com.rmc.search.SearchRequestBuilder;
import com.rmc.search.service.ProgramSearchService;
import com.rmc.search.service.SearchResult;
import com.rmc.ui.dynamic.DynamicFilterPane;
import com.rmc.ui.dynamic.FilterControlFactory;
import com.rmc.ui.dynamic.FilterBinder;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;

import java.net.CookieManager;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Главный контроллер с вкладками:
 * - Авторизация
 * - Фильтры
 * - Результаты
 * - Журнал
 */
public class MainTabController {
    
    private static final Logger logger = AppLogger.getLogger();
    
    // UI компоненты
    private TabPane tabPane;
    private Tab authTab;
    private Tab filtersTab;
    private Tab resultsTab;
    private Tab logTab;
    
    // Авторизация
    private TextField authUrlField;
    private TextField authUsernameField;
    private PasswordField authPasswordField;
    private Button authButton;
    private Label authStatusLabel;
    
    // Фильтры
    private Button loadFiltersButton;
    private Button searchButton;
    private DynamicFilterPane dynamicFilterPane;
    private Label filtersStatusLabel;
    
    // Результаты
    private ProgramTableController programTableController;
    private TableView<Program> resultsTable;
    private Label resultsStatusLabel;
    
    // Журнал
    private TextArea logTextArea;
    
    // Сервисы
    private HttpClientService httpClient;
    private AuthenticationService authService;
    private FilterBinder filterBinder;
    private List<com.rmc.filters.parser.FilterDefinition> currentFilters;
    
    public MainTabController() {
        this.filterBinder = new FilterBinder();
    }
    
    /**
     * Создать TabPane с вкладками.
     */
    public TabPane createTabPane() {
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Создаём вкладки
        authTab = createAuthTab();
        filtersTab = createFiltersTab();
        resultsTab = createResultsTab();
        logTab = createLogTab();
        
        tabPane.getTabs().addAll(authTab, filtersTab, resultsTab, logTab);
        
        return tabPane;
    }
    
    /**
     * Вкладка "Авторизация".
     */
    private Tab createAuthTab() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.TOP_LEFT);
        
        Label titleLabel = new Label("Авторизация");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);
        
        // URL
        Label urlLabel = new Label("URL авторизации:");
        authUrlField = new TextField("https://rmc.example.com/auth/login");
        authUrlField.setPrefWidth(400);
        formGrid.addRow(0, urlLabel, authUrlField);
        
        // Username
        Label usernameLabel = new Label("Логин:");
        authUsernameField = new TextField();
        authUsernameField.setPrefWidth(400);
        formGrid.addRow(1, usernameLabel, authUsernameField);
        
        // Password
        Label passwordLabel = new Label("Пароль:");
        authPasswordField = new PasswordField();
        authPasswordField.setPrefWidth(400);
        formGrid.addRow(2, passwordLabel, authPasswordField);
        
        // Кнопка и статус
        authButton = new Button("Войти");
        authButton.setOnAction(e -> performLogin());
        
        authStatusLabel = new Label();
        authStatusLabel.setStyle("-fx-text-fill: #666;");
        
        content.getChildren().addAll(titleLabel, formGrid, authButton, authStatusLabel);
        
        Tab tab = new Tab("Авторизация", content);
        tab.setId("auth");
        return tab;
    }
    
    /**
     * Вкладка "Фильтры".
     */
    private Tab createFiltersTab() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Фильтры");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Кнопки
        HBox buttonBox = new HBox(10);
        loadFiltersButton = new Button("Получить фильтры");
        loadFiltersButton.setOnAction(e -> loadFilters());
        
        searchButton = new Button("Найти программы");
        searchButton.setOnAction(e -> performSearch());
        searchButton.setDisable(true);
        
        buttonBox.getChildren().addAll(loadFiltersButton, searchButton);
        
        // Статус
        filtersStatusLabel = new Label("Загрузите фильтры");
        filtersStatusLabel.setStyle("-fx-text-fill: #666;");
        
        // Динамическая панель фильтров
        dynamicFilterPane = new DynamicFilterPane();
        dynamicFilterPane.setPrefHeight(400);
        
        ScrollPane scrollPane = new ScrollPane(dynamicFilterPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        content.getChildren().addAll(titleLabel, buttonBox, filtersStatusLabel, scrollPane);
        
        Tab tab = new Tab("Фильтры", content);
        tab.setId("filters");
        return tab;
    }
    
    /**
     * Вкладка "Результаты".
     */
    private Tab createResultsTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Результаты поиска");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        resultsStatusLabel = new Label("Нет результатов");
        resultsStatusLabel.setStyle("-fx-text-fill: #666;");
        
        // Таблица программ
        programTableController = new ProgramTableController();
        resultsTable = programTableController.getTableView();
        resultsTable.setPrefHeight(500);
        
        ScrollPane scrollPane = new ScrollPane(resultsTable);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        content.getChildren().addAll(titleLabel, resultsStatusLabel, scrollPane);
        
        Tab tab = new Tab("Результаты", content);
        tab.setId("results");
        return tab;
    }
    
    /**
     * Вкладка "Журнал".
     */
    private Tab createLogTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Журнал");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Button clearLogButton = new Button("Очистить");
        clearLogButton.setOnAction(e -> {
            if (logTextArea != null) {
                logTextArea.clear();
            }
        });
        
        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        logTextArea.setPrefHeight(500);
        
        VBox.setVgrow(logTextArea, Priority.ALWAYS);
        
        content.getChildren().addAll(titleLabel, clearLogButton, logTextArea);
        
        Tab tab = new Tab("Журнал", content);
        tab.setId("log");
        return tab;
    }
    
    /**
     * Выполнить авторизацию.
     */
    private void performLogin() {
        String url = authUrlField.getText();
        String username = authUsernameField.getText();
        String password = authPasswordField.getText();
        
        if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
            authStatusLabel.setText("Заполните все поля");
            authStatusLabel.setStyle("-fx-text-fill: red;");
            return;
        }
        
        authButton.setDisable(true);
        authStatusLabel.setText("Выполняется авторизация...");
        authStatusLabel.setStyle("-fx-text-fill: blue;");
        
        log("=== Авторизация ===");
        log("URL: " + url);
        log("Пользователь: " + username);
        
        try {
            // Создаём HTTP клиент с cookie manager
            CookieManager cookieManager = new CookieManager();
            httpClient = HttpClientService.builder()
                    .cookieManager(cookieManager)
                    .requestTimeout(Duration.ofSeconds(30))
                    .build();
            
            // Создаём сервис авторизации
            authService = AuthenticationService.builder()
                    .httpClient(httpClient)
                    .build();
            
            // Создаём запрос на авторизацию
            LoginRequest loginRequest = LoginRequest.builder()
                    .loginUrl(url)
                    .username(username)
                    .password(password)
                    .build();
            
            // Выполняем авторизацию
            LoginResult result = authService.login(loginRequest);
            
            if (result.isSuccess()) {
                authStatusLabel.setText("Авторизация успешна!");
                authStatusLabel.setStyle("-fx-text-fill: green;");
                log("Авторизация успешна");
                log("Cookies: " + result.getCookies().size());
                
                // Активируем кнопку загрузки фильтров
                loadFiltersButton.setDisable(false);
                
                // Переключаемся на вкладку фильтров
                tabPane.getSelectionModel().select(filtersTab);
            } else {
                authStatusLabel.setText("Ошибка: " + result.getErrorMessage().orElse("Неизвестная ошибка"));
                authStatusLabel.setStyle("-fx-text-fill: red;");
                log("Ошибка авторизации: " + result.getErrorMessage().orElse(""));
            }
            
        } catch (Exception e) {
            authStatusLabel.setText("Ошибка: " + e.getMessage());
            authStatusLabel.setStyle("-fx-text-fill: red;");
            log("Ошибка: " + e.getMessage());
            logger.error("Login error", e);
        } finally {
            authButton.setDisable(false);
        }
    }
    
    /**
     * Загрузить фильтры.
     */
    private void loadFilters() {
        if (httpClient == null) {
            filtersStatusLabel.setText("Сначала выполните авторизацию");
            return;
        }
        
        loadFiltersButton.setDisable(true);
        filtersStatusLabel.setText("Загрузка фильтров...");
        
        log("=== Загрузка фильтров ===");
        
        try {
            FilterPageLoader loader = FilterPageLoader.builder()
                    .httpClient(httpClient)
                    .baseUrl("https://rmc.example.com")
                    .build();
            
            FilterPageLoader.FilterPageResult result = loader.load();
            
            if (result.isSuccess()) {
                String html = result.getHtml();
                
                // Парсим HTML
                FilterParser.ParseResult parseResult = FilterParser.parse(html);
                
                if (parseResult.isSuccess()) {
                    currentFilters = parseResult.getFilters();
                    
                    // Загружаем в динамическую панель
                    dynamicFilterPane.loadFilters(currentFilters);
                    
                    filtersStatusLabel.setText("Загружено " + currentFilters.size() + " фильтров");
                    log("Загружено " + currentFilters.size() + " фильтров");
                    log("Размер HTML: " + result.getSize() + " символов");
                    
                    // Активируем поиск
                    searchButton.setDisable(false);
                } else {
                    filtersStatusLabel.setText("Ошибка парсинга фильтров");
                    log("Ошибка парсинга: " + parseResult.getErrorMessage().orElse(""));
                }
            } else {
                filtersStatusLabel.setText("Ошибка: " + result.getErrorMessage().orElse(""));
                log("Ошибка загрузки: " + result.getErrorMessage().orElse(""));
            }
            
        } catch (Exception e) {
            filtersStatusLabel.setText("Ошибка: " + e.getMessage());
            log("Ошибка: " + e.getMessage());
            logger.error("Load filters error", e);
        } finally {
            loadFiltersButton.setDisable(false);
        }
    }
    
    /**
     * Выполнить поиск.
     */
    private void performSearch() {
        if (httpClient == null) {
            resultsStatusLabel.setText("Сначала выполните авторизацию");
            return;
        }
        
        searchButton.setDisable(true);
        resultsStatusLabel.setText("Поиск...");
        programTableController.clear();
        
        log("=== Поиск программ ===");
        
        try {
            // Строим запрос
            SearchRequest request = SearchRequestBuilder.create()
                    .baseUrl("https://rmc.example.com")
                    .addFrom(dynamicFilterPane)
                    .build();
            
            log("URL: " + request.getFullUrl());
            
            // Выполняем поиск
            ProgramSearchService searchService = ProgramSearchService.builder()
                    .httpClient(httpClient)
                    .build();
            
            SearchResult searchResult = searchService.search(request);
            
            if (searchResult.isSuccess()) {
                String html = searchResult.getHtml();
                log("HTML получен: " + searchResult.getContentLength() + " символов");
                
                // Парсим результаты
                ProgramParser.ParseResult parseResult = ProgramParser.parse(html);
                
                if (parseResult.isSuccess()) {
                    List<Program> programs = parseResult.getPrograms();
                    
                    // Отображаем в таблице
                    programTableController.setPrograms(programs);
                    
                    resultsStatusLabel.setText("Найдено программ: " + programs.size());
                    log("Найдено программ: " + programs.size());
                    
                    // Переключаемся на вкладку результатов
                    tabPane.getSelectionModel().select(resultsTab);
                } else {
                    resultsStatusLabel.setText("Ошибка парсинга");
                    log("Ошибка парсинга: " + parseResult.getErrorMessage().orElse(""));
                }
            } else {
                resultsStatusLabel.setText("Ошибка: " + searchResult.getErrorMessage().orElse(""));
                log("Ошибка поиска: " + searchResult.getErrorMessage().orElse(""));
            }
            
        } catch (Exception e) {
            resultsStatusLabel.setText("Ошибка: " + e.getMessage());
            log("Ошибка: " + e.getMessage());
            logger.error("Search error", e);
        } finally {
            searchButton.setDisable(false);
        }
    }
    
    /**
     * Добавить сообщение в журнал.
     */
    private void log(String message) {
        if (logTextArea != null) {
            String timestamp = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            logTextArea.appendText("[" + timestamp + "] " + message + "\n");
        }
        logger.info(message);
    }
    
    public TabPane getTabPane() {
        return tabPane;
    }
}
