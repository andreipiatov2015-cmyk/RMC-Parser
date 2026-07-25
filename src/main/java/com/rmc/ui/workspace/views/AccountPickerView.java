package com.rmc.ui.workspace.views;

import com.rmc.auth.account.AccountStorageService;
import com.rmc.auth.account.SavedAccount;
import com.rmc.auth.django.DjangoAuthenticationProvider;
import com.rmc.config.ServerConfig;
import com.rmc.http.HttpClientService;
import com.rmc.logging.AppLogger;
import com.rmc.state.ApplicationState;
import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceState;
import com.rmc.ui.workspace.WorkspaceView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Экран выбора учётной записи — показывается при старте приложения, если
 * есть сохранённые аккаунты (см. {@link AccountStorageService}). Карточка
 * с логином — быстрый вход без повторного набора пароля; отдельная
 * карточка "+" — переход к обычной форме входа для добавления ещё одного
 * аккаунта.
 */
public class AccountPickerView extends VBox implements WorkspaceView {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private final WorkspaceContainer container;
    private final FlowPane cardsPane;
    private final Label statusLabel;
    
    public AccountPickerView(WorkspaceContainer container) {
        this.container = container;
        
        getStyleClass().add("account-picker-view");
        setAlignment(Pos.CENTER);
        setSpacing(24);
        
        Label title = new Label("👤 Выберите учётную запись");
        title.getStyleClass().add("account-picker-title");
        
        cardsPane = new FlowPane();
        cardsPane.getStyleClass().add("account-picker-cards");
        cardsPane.setHgap(20);
        cardsPane.setVgap(20);
        cardsPane.setAlignment(Pos.CENTER);
        cardsPane.setMaxWidth(600);
        cardsPane.setPrefWrapLength(600);
        
        statusLabel = new Label();
        statusLabel.getStyleClass().add("account-picker-status");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        
        getChildren().addAll(title, cardsPane, statusLabel);
    }
    
    @Override
    public Pane getRoot() {
        return this;
    }
    
    @Override
    public void onEnter() {
        refresh();
    }
    
    /**
     * Есть ли хотя бы одна сохранённая учётная запись — используется
     * при старте приложения, чтобы решить, с какого экрана начинать.
     */
    public static boolean hasSavedAccounts() {
        return !AccountStorageService.loadAll().isEmpty();
    }
    
    /**
     * Перечитать список сохранённых учётных записей и перестроить карточки.
     */
    public void refresh() {
        cardsPane.getChildren().clear();
        hideStatus();
        
        List<SavedAccount> accounts = AccountStorageService.loadAll();
        for (SavedAccount account : accounts) {
            cardsPane.getChildren().add(createAccountCard(account));
        }
        cardsPane.getChildren().add(createAddCard());
    }
    
    private Pane createAccountCard(SavedAccount account) {
        StackPane wrapper = new StackPane();
        wrapper.getStyleClass().add("account-card-wrapper");
        wrapper.setAlignment(Pos.TOP_RIGHT);
        
        VBox card = new VBox();
        card.getStyleClass().add("account-card");
        card.setAlignment(Pos.CENTER);
        card.setSpacing(10);
        card.setPadding(new Insets(20));
        card.setPrefWidth(140);
        
        StackPane avatar = createAvatarNode(account);
        
        VBox nameBox = new VBox(2);
        nameBox.setAlignment(Pos.CENTER);
        
        Optional<String> displayName = account.getDisplayName();
        if (displayName.isPresent()) {
            Label bigName = new Label(displayName.get());
            bigName.getStyleClass().add("account-card-display-name");
            bigName.setWrapText(true);
            
            Label smallLogin = new Label(account.getUsername());
            smallLogin.getStyleClass().add("account-card-username");
            
            nameBox.getChildren().addAll(bigName, smallLogin);
        } else {
            Label nameLabel = new Label(account.getUsername());
            nameLabel.getStyleClass().add("account-card-name");
            nameLabel.setWrapText(true);
            nameBox.getChildren().add(nameLabel);
        }
        
        card.getChildren().addAll(avatar, nameBox);
        card.setOnMouseClicked(e -> attemptAutoLogin(account, card));
        
        Label removeButton = new Label("×");
        removeButton.getStyleClass().add("account-card-remove");
        removeButton.setOnMouseClicked(e -> {
            e.consume();
            confirmRemoveAccount(account);
        });
        StackPane.setMargin(removeButton, new Insets(6, 6, 0, 0));
        
        wrapper.getChildren().addAll(card, removeButton);
        return wrapper;
    }
    
    /**
     * Спрашивает подтверждение и, если пользователь согласен, удаляет
     * сохранённые данные для входа — карточка исчезнет из пикера, пароль
     * для этого логина придётся ввести заново при следующем входе.
     */
    private void confirmRemoveAccount(SavedAccount account) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Забыть учётную запись?");
        confirm.setHeaderText(null);
        confirm.setContentText("Забыть сохранённые данные для входа \"" + account.getUsername()
                + "\"? Понадобится ввести пароль заново при следующем входе.");
        
        ButtonType yes = new ButtonType("Да");
        ButtonType no = new ButtonType("Нет", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);
        styleDialog(confirm);
        
        confirm.showAndWait().ifPresent(button -> {
            if (button == yes) {
                AccountStorageService.remove(account.getUsername());
                refresh();
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
    
    private Pane createAddCard() {
        VBox card = new VBox();
        card.getStyleClass().addAll("account-card", "account-card-add");
        card.setAlignment(Pos.CENTER);
        card.setSpacing(10);
        card.setPadding(new Insets(20));
        card.setPrefWidth(140);
        
        StackPane circle = new StackPane();
        circle.getStyleClass().add("account-add-circle");
        circle.setPrefSize(64, 64);
        circle.setMaxSize(64, 64);
        circle.setMinSize(64, 64);
        
        Label plus = new Label("+");
        plus.getStyleClass().add("account-add-plus");
        circle.getChildren().add(plus);
        
        Label label = new Label("Добавить учётную запись");
        label.getStyleClass().add("account-card-name");
        label.setWrapText(true);
        
        card.getChildren().addAll(circle, label);
        card.setOnMouseClicked(e -> container.transitionTo(WorkspaceState.AUTH));
        
        return card;
    }
    
    /**
     * Аватар учётной записи: если пользователь задал своё изображение в
     * настройках внешнего вида — показываем его (обрезанным по кругу),
     * иначе — заглушку-человечка.
     */
    private StackPane createAvatarNode(SavedAccount account) {
        Optional<File> avatarFile = AccountStorageService.getAvatarFile(account.getUsername());
        if (avatarFile.isPresent()) {
            try {
                Image image = new Image(avatarFile.get().toURI().toString(), 128, 128, true, true, true);
                Circle circle = new Circle(32);
                circle.setFill(new ImagePattern(image));
                
                StackPane wrapper = new StackPane(circle);
                wrapper.setPrefSize(64, 64);
                wrapper.setMaxSize(64, 64);
                wrapper.setMinSize(64, 64);
                return wrapper;
            } catch (Exception e) {
                logger.warn("Не удалось загрузить аватар для {}: {}", account.getUsername(), e.getMessage());
            }
        }
        return createAvatarPlaceholder();
    }
    
    private StackPane createAvatarPlaceholder() {
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("account-avatar");
        avatar.setPrefSize(64, 64);
        avatar.setMaxSize(64, 64);
        avatar.setMinSize(64, 64);
        
        Label icon = new Label("👤");
        icon.getStyleClass().add("account-avatar-icon");
        avatar.getChildren().add(icon);
        
        return avatar;
    }
    
    private void attemptAutoLogin(SavedAccount account, Pane card) {
        showStatus("Вход как " + account.getUsername() + "...");
        card.setDisable(true);
        
        Optional<String> passwordOpt = AccountStorageService.decryptPassword(account);
        if (passwordOpt.isEmpty()) {
            showStatus("Не удалось расшифровать сохранённый пароль. Войдите вручную.");
            card.setDisable(false);
            return;
        }
        String password = passwordOpt.get();
        
        new Thread(() -> {
            try {
                HttpClientService httpClient = HttpClientService.builder().build();
                DjangoAuthenticationProvider provider =
                        new DjangoAuthenticationProvider(httpClient, ServerConfig.BASE_URL);
                
                DjangoAuthenticationProvider.DjangoAuthResult result =
                        provider.authenticate(account.getUsername(), password);
                
                javafx.application.Platform.runLater(() -> {
                    card.setDisable(false);
                    if (result.isSuccess()) {
                        ApplicationState.getInstance()
                                .login(account.getUsername(), result.getHttpClient().orElse(httpClient));
                        hideStatus();
                        container.onLoginSuccess(account.getUsername());
                    } else {
                        showStatus("Не удалось войти: "
                                + result.getErrorMessage().orElse("неизвестная ошибка")
                                + ". Возможно, пароль изменился — войдите вручную (карточка \"+\").");
                    }
                });
            } catch (Exception e) {
                logger.error("Ошибка автоматического входа", e);
                javafx.application.Platform.runLater(() -> {
                    card.setDisable(false);
                    showStatus("Ошибка: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void showStatus(String message) {
        statusLabel.setText(message);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
    
    private void hideStatus() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }
}
