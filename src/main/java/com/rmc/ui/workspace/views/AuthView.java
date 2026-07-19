package com.rmc.ui.workspace.views;

import com.rmc.auth.account.AccountStorageService;
import com.rmc.auth.django.DjangoAuthenticationProvider;
import com.rmc.config.ServerConfig;
import com.rmc.http.HttpClientService;
import com.rmc.logging.AppLogger;
import com.rmc.state.ApplicationState;
import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
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
                HttpClientService httpClient = HttpClientService.builder().build();
                DjangoAuthenticationProvider provider =
                        new DjangoAuthenticationProvider(httpClient, ServerConfig.BASE_URL);

                DjangoAuthenticationProvider.DjangoAuthResult result =
                        provider.authenticate(login, password);
                
                javafx.application.Platform.runLater(() -> {
                    if (result.isSuccess()) {
                        ApplicationState.getInstance()
                                .login(login, result.getHttpClient().orElse(httpClient));
                        
                        if (!AccountStorageService.isSaved(login)) {
                            promptSaveCredentials(login, password);
                        }
                        
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
    
    private void promptSaveCredentials(String login, String password) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Сохранить данные для входа?");
        alert.setHeaderText(null);
        alert.setContentText("Запомнить логин \"" + login + "\" для быстрого входа в следующий раз?");
        
        ButtonType yes = new ButtonType("Да");
        ButtonType no = new ButtonType("Нет", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(yes, no);
        styleDialog(alert);
        
        alert.showAndWait().ifPresent(button -> {
            if (button == yes) {
                AccountStorageService.save(login, password);
            }
        });
    }
    
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
    
    /**
     * Подставить логин без пароля — используется при повторном входе после
     * "Выхода": учётная запись остаётся сохранённой (доступна через
     * "Сменить пользователя"), но сам "Выход" — осознанное действие,
     * поэтому пароль всегда нужно ввести заново.
     *
     * <p>Вызывается ПОСЛЕ {@link #onEnter()} (см. {@code WorkspaceContainer.transitionTo}),
     * иначе {@link #onEnter()} очистил бы то, что здесь подставлено.</p>
     */
    public void prefillUsername(String username) {
        loginField.setText(username);
        passwordField.clear();
        passwordField.requestFocus();
    }
}
