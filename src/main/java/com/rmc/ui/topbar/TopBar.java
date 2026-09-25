package com.rmc.ui.topbar;

import com.rmc.auth.account.AccountStorageService;
import com.rmc.auth.account.SavedAccount;
import com.rmc.state.ApplicationState;
import com.rmc.ui.icons.TablerIcon;
import com.rmc.ui.icons.TablerIcons;
import com.rmc.ui.settings.SettingsWindow;
import com.rmc.ui.theme.ThemeService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;

/**
 * Верхняя панель — всегда на виду, независимо от текущего экрана.
 *
 * <p>Раньше здесь дублировались логин пользователя, статус подключения и
 * версия программы (каждое ещё в одном-двух других местах интерфейса).
 * Теперь версия и статус подключения показываются только в {@code
 * StatusBar} внизу, а здесь — единственный блок "аватар + логин", клик по
 * которому открывает меню (Выход / Сменить пользователя / Настройки).
 * Рядом — быстрый доступ к избранному, RMCAI-чату и переключению темы.</p>
 */
public class TopBar extends HBox {
    
    private final Label titleLabel;
    private final Label favoritesButton;
    private final Label rmcAiButton;
    private final Label themeButton;
    private final StackPane avatarCircle;
    private final Label userNameLabel;
    private final HBox userBlock;
    private final ContextMenu userMenu;
    
    private Runnable onSwitchUser;
    private Runnable onLogoutConfirmed;
    private Runnable onShowFavorites;
    private Runnable onShowRmcAi;
    
    public TopBar() {
        getStyleClass().add("top-bar");
        setPadding(new Insets(0, 16, 0, 12));
        setAlignment(Pos.CENTER_LEFT);
        setPrefHeight(48);
        
        titleLabel = new Label("RMC Framework");
        titleLabel.getStyleClass().add("title-label");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Быстрый доступ — раньше эти два пункта были только в выезжающем
        // меню, теперь на виду всегда, с подсказкой при наведении.
        favoritesButton = iconButton(TablerIcons.STAR, "Избранные учреждения");
        favoritesButton.setOnMouseClicked(e -> {
            if (onShowFavorites != null) {
                onShowFavorites.run();
            }
        });
        
        rmcAiButton = iconButton(TablerIcons.ROBOT, "RMCAI");
        rmcAiButton.setOnMouseClicked(e -> {
            if (onShowRmcAi != null) {
                onShowRmcAi.run();
            }
        });
        
        // Быстрое переключение темы — дублирует то же самое в "Настройках",
        // но тему меняют часто, доставать её из настроек каждый раз неудобно.
        themeButton = new Label();
        themeButton.setGraphic(TablerIcon.of(themeIconPath()));
        themeButton.getStyleClass().add("topbar-icon-button");
        Tooltip themeTooltipControl = new Tooltip(themeTooltip());
        Tooltip.install(themeButton, themeTooltipControl);
        themeButton.setOnMouseClicked(e -> {
            ThemeService.toggle();
            themeButton.setGraphic(TablerIcon.of(themeIconPath()));
            themeTooltipControl.setText(themeTooltip());
        });
        
        // Блок "аватар + логин" — единственное место, где теперь
        // показывается, кто авторизован.
        avatarCircle = new StackPane();
        avatarCircle.setPrefSize(28, 28);
        avatarCircle.setMaxSize(28, 28);
        avatarCircle.setMinSize(28, 28);
        avatarCircle.getStyleClass().add("topbar-avatar");
        
        userNameLabel = new Label();
        userNameLabel.getStyleClass().add("user-label");
        
        userBlock = new HBox(8, avatarCircle, userNameLabel);
        userBlock.getStyleClass().add("topbar-user-block");
        userBlock.setAlignment(Pos.CENTER_LEFT);
        userBlock.setPadding(new Insets(4, 10, 4, 6));
        
        MenuItem logoutItem = new MenuItem("Выход");
        logoutItem.setOnAction(e -> confirmLogout());
        MenuItem switchUserItem = new MenuItem("Сменить пользователя");
        switchUserItem.setOnAction(e -> {
            if (onSwitchUser != null) {
                onSwitchUser.run();
            }
        });
        MenuItem settingsItem = new MenuItem("Настройки");
        settingsItem.setOnAction(e -> SettingsWindow.show(
                getScene() != null ? getScene().getWindow() : null, this::refreshUserBlock));
        
        userMenu = new ContextMenu(logoutItem, switchUserItem, settingsItem);
        userBlock.setOnMouseClicked(e -> userMenu.show(userBlock, Side.BOTTOM, 0, 4));
        
        getChildren().addAll(titleLabel, spacer, favoritesButton, rmcAiButton, themeButton, userBlock);
        
        refreshUserBlock();
    }
    
    private Label iconButton(String iconPathData, String tooltipText) {
        Label label = new Label();
        label.setGraphic(TablerIcon.of(iconPathData));
        label.getStyleClass().add("topbar-icon-button");
        Tooltip.install(label, new Tooltip(tooltipText));
        return label;
    }
    
    private String themeIconPath() {
        return ThemeService.isDarkMode() ? TablerIcons.SUN : TablerIcons.MOON;
    }
    
    private String themeTooltip() {
        return ThemeService.isDarkMode() ? "Светлая тема" : "Тёмная тема";
    }
    
    public void setOnSwitchUser(Runnable handler) {
        this.onSwitchUser = handler;
    }
    
    /**
     * Вызывается ПОСЛЕ того, как пользователь подтвердил выход в диалоге —
     * обработчик должен выполнить фактический выход
     * ({@code ApplicationState.logout()}) и переключить экран.
     */
    public void setOnLogoutConfirmed(Runnable handler) {
        this.onLogoutConfirmed = handler;
    }
    
    public void setOnShowFavorites(Runnable handler) {
        this.onShowFavorites = handler;
    }
    
    public void setOnShowRmcAi(Runnable handler) {
        this.onShowRmcAi = handler;
    }
    
    public void onAuthStateChanged() {
        refreshUserBlock();
    }
    
    /**
     * Перерисовывает аватар и логин — вызывается при входе/выходе, а
     * также после закрытия окна настроек (вдруг пользователь сменил
     * аватар или отображаемое имя на вкладке "Профиль").
     */
    private void refreshUserBlock() {
        ApplicationState state = ApplicationState.getInstance();
        boolean authenticated = state.isAuthenticated();
        
        userBlock.setVisible(authenticated);
        userBlock.setManaged(authenticated);
        if (!authenticated) {
            return;
        }
        
        String username = state.getUsername();
        String displayName = AccountStorageService.loadAll().stream()
                .filter(a -> a.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .flatMap(SavedAccount::getDisplayName)
                .filter(name -> !name.isBlank())
                .orElse(username);
        userNameLabel.setText(displayName);
        
        avatarCircle.getChildren().clear();
        var avatarFile = AccountStorageService.getAvatarFile(username);
        if (avatarFile.isPresent()) {
            try {
                Image image = new Image(avatarFile.get().toURI().toString(), 56, 56, true, true, true);
                Circle circle = new Circle(14);
                circle.setFill(new ImagePattern(image));
                avatarCircle.getChildren().add(circle);
                return;
            } catch (Exception ignored) {
                // покажем заглушку ниже
            }
        }
        Label placeholder = new Label();
        placeholder.setGraphic(TablerIcon.of(TablerIcons.USER, 14));
        placeholder.getStyleClass().add("topbar-avatar-icon");
        avatarCircle.getChildren().add(placeholder);
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
    
    private void styleDialog(Alert alert) {
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard.css").toExternalForm());
        alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/dashboard-dark.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("app-dialog");
        if (ThemeService.isDarkMode()) {
            alert.getDialogPane().getStyleClass().add("dark-theme");
        }
    }
}
