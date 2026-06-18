package com.rmc.ui.dashboard;

import com.rmc.auth.django.DjangoAuthenticationProvider;
import com.rmc.http.HttpClientService;
import com.rmc.state.ApplicationState;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Login card with authentication form.
 * Shows login form when not authenticated, user info when authenticated.
 */
public class LoginCard extends VBox {
    
    private static final String RMC_BASE_URL = "https://rmc.ruobr.ru";
    
    private Label titleLabel;
    private VBox loginForm;
    private VBox userInfo;
    private Label statusLabel;
    private TextField loginField;
    private PasswordField passwordField;
    
    public LoginCard() {
        getStyleClass().add("card");
        getStyleClass().add("login-card");
        setPadding(new Insets(20));
        setSpacing(16);
        
        titleLabel = new Label("Авторизация");
        titleLabel.getStyleClass().add("card-title");
        
        statusLabel = new Label();
        statusLabel.getStyleClass().add("login-status");
        
        loginForm = createLoginForm();
        userInfo = createUserInfo();
        
        getChildren().addAll(titleLabel, loginForm);
        
        updateView();
    }
    
    private VBox createLoginForm() {
        VBox form = new VBox();
        form.setSpacing(12);
        form.setAlignment(Pos.CENTER_LEFT);
        
        Label loginLbl = new Label("Логин:");
        loginField = new TextField();
        loginField.setPromptText("Введите логин");
        loginField.getStyleClass().add("form-field");
        loginField.setPrefWidth(200);
        
        Label passwordLbl = new Label("Пароль:");
        passwordField = new PasswordField();
        passwordField.setPromptText("Введите пароль");
        passwordField.getStyleClass().add("form-field");
        passwordField.setPrefWidth(200);
        
        Button loginBtn = new Button("Войти");
        loginBtn.getStyleClass().add("primary-button");
        loginBtn.setPrefWidth(200);
        loginBtn.setOnAction(e -> handleLogin());
        
        // Enter key support
        passwordField.setOnAction(e -> handleLogin());
        
        form.getChildren().addAll(loginLbl, loginField, passwordLbl, passwordField, loginBtn, statusLabel);
        
        return form;
    }
    
    private VBox createUserInfo() {
        VBox info = new VBox();
        info.setSpacing(12);
        info.setAlignment(Pos.CENTER_LEFT);
        
        // Auth status
        Label authStatus = new Label("✅ Авторизован");
        authStatus.getStyleClass().add("auth-status-success");
        
        // Username
        Label usernameLbl = new Label();
        usernameLbl.getStyleClass().add("user-name");
        
        // Login time
        Label loginTimeLbl = new Label();
        loginTimeLbl.getStyleClass().add("login-time");
        
        // Logout button
        Button logoutBtn = new Button("Выйти");
        logoutBtn.getStyleClass().add("secondary-button");
        logoutBtn.setPrefWidth(200);
        logoutBtn.setOnAction(e -> handleLogout());
        
        info.getChildren().addAll(authStatus, usernameLbl, loginTimeLbl, logoutBtn);
        
        return info;
    }
    
    private void handleLogin() {
        String login = loginField.getText();
        String password = passwordField.getText();
        
        if (login.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Заполните все поля");
            statusLabel.setStyle("-fx-text-fill: #f44336;");
            return;
        }
        
        statusLabel.setText("Выполняется авторизация...");
        statusLabel.setStyle("-fx-text-fill: #666;");
        loginField.setDisable(true);
        passwordField.setDisable(true);
        
        new Thread(() -> {
            try {
                HttpClientService httpClient = HttpClientService.builder().build();
                DjangoAuthenticationProvider provider = new DjangoAuthenticationProvider(httpClient, RMC_BASE_URL);
                var result = provider.authenticate(login, password);
                
                Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        ApplicationState.getInstance().login(login, result.getHttpClient().orElse(httpClient));
                        updateView();
                    } else {
                        statusLabel.setText("Ошибка: " + result.getErrorMessage().orElse("Неизвестная ошибка"));
                        statusLabel.setStyle("-fx-text-fill: #f44336;");
                        loginField.setDisable(false);
                        passwordField.setDisable(false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Ошибка: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #f44336;");
                    loginField.setDisable(false);
                    passwordField.setDisable(false);
                });
            }
        }).start();
    }
    
    private void handleLogout() {
        ApplicationState.getInstance().logout();
        updateView();
    }
    
    public void updateView() {
        ApplicationState state = ApplicationState.getInstance();
        
        if (state.isAuthenticated()) {
            // Show user info
            getChildren().clear();
            getChildren().addAll(titleLabel, userInfo);
            
            // Update user info
            ((Label) userInfo.getChildren().get(1)).setText("Пользователь: " + state.getUsername());
            
            state.getLoginTime().ifPresentOrElse(
                time -> ((Label) userInfo.getChildren().get(2)).setText("Вход: " + time),
                () -> ((Label) userInfo.getChildren().get(2)).setText("")
            );
        } else {
            // Show login form
            getChildren().clear();
            getChildren().addAll(titleLabel, loginForm);
            
            loginField.setDisable(false);
            passwordField.setDisable(false);
            loginField.clear();
            passwordField.clear();
            statusLabel.setText("");
        }
    }
}
