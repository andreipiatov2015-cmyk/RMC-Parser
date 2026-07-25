package com.rmc.ui.topbar;

import com.rmc.auth.account.AccountStorageService;
import com.rmc.auth.account.SavedAccount;
import com.rmc.state.ApplicationState;
import com.rmc.ui.theme.ThemeService;
import com.rmc.version.VersionService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Top bar - always visible header.
 */
public class TopBar extends HBox {
    
    private final Label menuButton;
    private final Label titleLabel;
    private final Label versionLabel;
    private final Label userLabel;
    private final Label switchUserButton;
    private final ConnectionIndicator connectionIndicator;
    private final Label settingsButton;
    private final ContextMenu settingsMenu;
    
    private Runnable onMenuClick;
    private Runnable onSwitchUser;
    private Runnable onLogoutConfirmed;
    
    public TopBar() {
        getStyleClass().add("top-bar");
        setPadding(new Insets(0, 16, 0, 12));
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(48);
        
        // Menu button
        menuButton = new Label("☰");
        menuButton.getStyleClass().add("menu-button");
        menuButton.setOnMouseClicked(e -> {
            if (onMenuClick != null) {
                onMenuClick.run();
            }
        });
        
        // Title
        titleLabel = new Label("RMC Framework");
        titleLabel.getStyleClass().add("title-label");
        
        // Version
        versionLabel = new Label("v" + VersionService.getCurrentVersionString());
        versionLabel.getStyleClass().add("version-label");
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // User label
        userLabel = new Label();
        userLabel.getStyleClass().add("user-label");
        
        // Switch user button
        switchUserButton = new Label("Сменить пользователя");
        switchUserButton.getStyleClass().add("switch-user-button");
        switchUserButton.setOnMouseClicked(e -> {
            if (onSwitchUser != null) {
                onSwitchUser.run();
            }
        });
        
        // Connection indicator
        connectionIndicator = new ConnectionIndicator();
        
        // Settings button + dropdown menu
        settingsButton = new Label("⚙");
        settingsButton.getStyleClass().add("settings-button");
        
        MenuItem appearanceItem = new MenuItem("Настроить внешний вид учётной записи");
        appearanceItem.setOnAction(e -> showAppearanceSettings());
        
        MenuItem logoutItem = new MenuItem("Выход");
        logoutItem.setOnAction(e -> confirmLogout());
        
        settingsMenu = new ContextMenu(appearanceItem, logoutItem);
        settingsButton.setOnMouseClicked(e -> settingsMenu.show(settingsButton, Side.BOTTOM, 0, 4));
        
        getChildren().addAll(menuButton, titleLabel, versionLabel, spacer,
                userLabel, switchUserButton, connectionIndicator, settingsButton);
        
        updateUserInfo();
        updateConnectionStatus();
    }
    
    public void setOnMenuClick(Runnable handler) {
        this.onMenuClick = handler;
    }
    
    /**
     * Вызывается при клике "Сменить пользователя" — обработчик должен
     * показать экран выбора учётной записи.
     */
    public void setOnSwitchUser(Runnable handler) {
        this.onSwitchUser = handler;
    }
    
    /**
     * Вызывается ПОСЛЕ того, как пользователь подтвердил выход в диалоге
     * (само диалоговое окно показывает TopBar) — обработчик должен
     * выполнить фактический выход ({@code ApplicationState.logout()}) и
     * переключить экран.
     */
    public void setOnLogoutConfirmed(Runnable handler) {
        this.onLogoutConfirmed = handler;
    }
    
    public void onAuthStateChanged() {
        updateUserInfo();
    }
    
    private void updateUserInfo() {
        ApplicationState state = ApplicationState.getInstance();
        boolean authenticated = state.isAuthenticated();
        
        userLabel.setText(authenticated ? state.getUsername() : "");
        switchUserButton.setVisible(authenticated);
        switchUserButton.setManaged(authenticated);
    }
    
    private void updateConnectionStatus() {
        // In real implementation, check actual connection
        ApplicationState state = ApplicationState.getInstance();
        connectionIndicator.setConnected(state.isAuthenticated());
    }
    
    public void setConnected(boolean connected) {
        connectionIndicator.setConnected(connected);
    }
    
    private void confirmLogout() {
        String username = ApplicationState.getInstance().getUsername();
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Выход");
        confirm.setHeaderText(null);
        confirm.setContentText("Вы хотите выйти из пользователя \"" + username
                + "\"? Вам придётся авторизоваться заново.");
        
        ButtonType yes = new ButtonType("Да");
        ButtonType no = new ButtonType("Нет", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);
        styleDialog(confirm);
        
        confirm.showAndWait().ifPresent(button -> {
            if (button == yes && onLogoutConfirmed != null) {
                onLogoutConfirmed.run();
            }
        });
    }
    
    /**
     * Настройка внешнего вида учётной записи — доступна только если
     * пользователь авторизован. Позволяет сменить аватар (показывается
     * на экране выбора аккаунта вместо заглушки-человечка) и задать
     * отображаемое имя (крупно на карточке вместо логина, сам логин —
     * мелким серым текстом под ним).
     */
    private void showAppearanceSettings() {
        ApplicationState state = ApplicationState.getInstance();
        if (!state.isAuthenticated()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Настройка внешнего вида");
            info.setHeaderText(null);
            info.setContentText("Сначала войдите в систему, чтобы настроить профиль учётной записи.");
            styleDialog(info);
            info.showAndWait();
            return;
        }
        
        String username = state.getUsername();
        boolean accountIsSaved = AccountStorageService.isSaved(username);
        
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Настройка внешнего вида учётной записи");
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm());
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard-dark.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().add("app-dialog");
        if (ThemeService.isDarkMode()) {
            dialog.getDialogPane().getStyleClass().add("dark-theme");
        }
        
        // Предпросмотр аватара
        StackPane avatarPreview = new StackPane();
        avatarPreview.setPrefSize(96, 96);
        avatarPreview.setMaxSize(96, 96);
        avatarPreview.setMinSize(96, 96);
        avatarPreview.getStyleClass().add("account-avatar");
        
        File[] chosenAvatarFile = new File[1];
        
        Runnable refreshAvatarPreview = () -> {
            avatarPreview.getChildren().clear();
            File fileToShow = chosenAvatarFile[0] != null
                    ? chosenAvatarFile[0]
                    : AccountStorageService.getAvatarFile(username).orElse(null);
            if (fileToShow != null) {
                try {
                    Image image = new Image(fileToShow.toURI().toString(), 192, 192, true, true, true);
                    Circle circle = new Circle(48);
                    circle.setFill(new ImagePattern(image));
                    avatarPreview.getChildren().add(circle);
                    return;
                } catch (Exception ignored) {
                    // покажем заглушку ниже
                }
            }
            Label placeholder = new Label("👤");
            placeholder.getStyleClass().add("account-avatar-icon");
            avatarPreview.getChildren().add(placeholder);
        };
        refreshAvatarPreview.run();
        
        Button chooseAvatarButton = new Button("Выбрать изображение...");
        chooseAvatarButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Выберите изображение для аватара");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Изображения", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));
            File selected = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (selected != null) {
                chosenAvatarFile[0] = selected;
                refreshAvatarPreview.run();
            }
        });
        
        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.getChildren().addAll(avatarPreview, chooseAvatarButton);
        
        if (!accountIsSaved) {
            Label note = new Label("Учётная запись \"" + username + "\" не сохранена (не отмечена \"Сохранить "
                    + "данные для входа\" при входе) — настройки внешнего вида доступны только для сохранённых "
                    + "учётных записей, показываются на экране их выбора.");
            note.setWrapText(true);
            note.setMaxWidth(320);
            note.getStyleClass().add("account-picker-status");
            content.getChildren().add(note);
            
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
            dialog.showAndWait();
            return;
        }
        
        Label nameFieldLabel = new Label("Отображаемое имя (необязательно):");
        nameFieldLabel.getStyleClass().add("account-picker-status");
        
        TextField displayNameField = new TextField();
        displayNameField.setPromptText("Например: Администратор");
        displayNameField.setMaxWidth(260);
        AccountStorageService.loadAll().stream()
                .filter(a -> a.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .flatMap(SavedAccount::getDisplayName)
                .ifPresent(displayNameField::setText);
        
        content.getChildren().addAll(nameFieldLabel, displayNameField);
        dialog.getDialogPane().setContent(content);
        
        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(saveButtonType, ButtonType.CANCEL);
        
        dialog.showAndWait().ifPresent(button -> {
            if (button == saveButtonType) {
                if (chosenAvatarFile[0] != null) {
                    AccountStorageService.saveAvatar(username, chosenAvatarFile[0]);
                }
                String newDisplayName = displayNameField.getText();
                AccountStorageService.setDisplayName(username, newDisplayName == null ? "" : newDisplayName.trim());
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
}
