package com.rmc.ui.workspace.views;

import com.rmc.auth.AuthenticationService;
import com.rmc.auth.LoginRequest;
import com.rmc.logging.AppLogger;
import com.rmc.state.ApplicationState;
import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;

/**
 * Authentication view - login form.
 */
public class AuthView extends VBox implements WorkspaceView {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private final WorkspaceContainer container;
    private final TextField loginField;
    private final PasswordField passwordField;
    private final Label errorLabel;
    private final Button loginButton;
    
    public AuthView(WorkspaceContainer container) {
        this.container = container;
        
        getStyleClass().add("auth-view");
        setAlignment(Pos.CENTER);
        setSpacing(16);
        
        // Card container
        VBox card = new VBox();
        card.getStyleClass().add("auth-card");
        card.setSpacing(16);
        card.setPadding(new Insets(32));
        card.setMaxWidth(320);
        
        // Title
        Label title = new Label("🔐 Вход в систему");
        title.getStyleClass().add("auth-title");
        
        // Error label
        errorLabel = new Label();
        errorLabel.getStyleClass().add("auth-error");
        errorLabel.setVisible(false);
        
        // Login field
        loginField = new TextField();
        loginField.getStyleClass().add("auth-field");
        loginField.setPromptText("Логин");
        loginField.setPrefHeight(40);
        
        // Password field
        passwordField = new PasswordField();
        passwordField.getStyleClass().add("auth-field");
        passwordField.setPromptText("Пароль");
        passwordField.setPrefHeight(40);
        
        // Enter key handler
        passwordField.setOnAction(e -> attemptLogin());
        
        // Login button
        loginButton = new Button("Войти");
        loginButton.getStyleClass().add("auth-button");
        loginButton.setPrefHeight(40);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(e -> attemptLogin());
        
        card.getChildren().addAll(title, errorLabel, loginField, passwordField, loginButton);
        getChildren().add(card);
    }
    
    private void attemptLogin() {
        String login = loginField.getText().trim();
        String password = passwordField.getText();
        
        if (login.isEmpty() || password.isEmpty()) {
            showError("Введите логин и пароль");
            return;
        }
        
        errorLabel.setVisible(false);
        loginButton.setDisable(true);
        loginButton.setText("Вход...");
        
        new Thread(() -> {
            try {
                AuthenticationService authService = new AuthenticationService.Builder()
                    .baseUrl("https://rmc.ruobr.ru")
                    .build();
                
                LoginRequest request = new LoginRequest.Builder()
                    .username(login)
                    .password(password)
                    .build();
                
                var result = authService.login(request);
                
                javafx.application.Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        ApplicationState.getInstance().setAuthenticated(true, login);
                        container.onLoginSuccess(login);
                    } else {
                        showError(result.getErrorMessage().orElse("Ошибка входа"));
                        loginButton.setDisable(false);
                        loginButton.setText("Войти");
                    }
                });
            } catch (Exception e) {
                logger.error("Login error", e);
                javafx.application.Platform.runLater(() -> {
                    showError("Ошибка: " + e.getMessage());
                    loginButton.setDisable(false);
                    loginButton.setText("Войти");
                });
            }
        }).start();
    }
    
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
    
    @Override
    public Pane getRoot() {
        return this;
    }
    
    @Override
    public void onEnter() {
        loginField.clear();
        passwordField.clear();
        errorLabel.setVisible(false);
        loginButton.setDisable(false);
        loginButton.setText("Войти");
    }
}
