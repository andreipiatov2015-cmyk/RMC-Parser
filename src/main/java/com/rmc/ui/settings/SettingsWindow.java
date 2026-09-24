package com.rmc.ui.settings;

import com.rmc.auth.account.AccountStorageService;
import com.rmc.auth.account.SavedAccount;
import com.rmc.state.ApplicationState;
import com.rmc.ui.theme.ThemeService;
import com.rmc.update.UpdateCheckResult;
import com.rmc.update.UpdateCheckService;
import com.rmc.version.VersionService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Окно "Настройки": список категорий слева, содержимое выбранной
 * категории — справа. Объединяет то, что раньше было раскидано по
 * выезжающему меню (тема, обновление, о программе) и отдельному диалогу
 * "Настроить внешний вид учётной записи" из {@code TopBar} — теперь всё
 * в одном месте.
 */
public class SettingsWindow {
    
    private final Stage stage;
    private final VBox contentArea;
    private final Runnable onProfileChanged;
    
    private SettingsWindow(Window owner, Runnable onProfileChanged) {
        this.onProfileChanged = onProfileChanged;
        
        stage = new Stage(StageStyle.UTILITY);
        stage.setTitle("Настройки");
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setResizable(false);
        
        VBox categoryList = new VBox();
        categoryList.getStyleClass().add("settings-category-list");
        categoryList.setSpacing(2);
        categoryList.setPadding(new Insets(12, 8, 12, 8));
        categoryList.setPrefWidth(180);
        
        contentArea = new VBox();
        contentArea.getStyleClass().add("settings-content-area");
        contentArea.setPadding(new Insets(24));
        contentArea.setSpacing(16);
        
        categoryList.getChildren().addAll(
                createCategoryItem("Профиль", this::showProfile, true),
                createCategoryItem("Оформление", this::showAppearance, false),
                createCategoryItem("Обновление программы", this::showUpdate, false),
                createCategoryItem("О программе", this::showAbout, false)
        );
        
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-dialog");
        root.setLeft(categoryList);
        root.setCenter(contentArea);
        
        Scene scene = new Scene(root, 560, 420);
        scene.getStylesheets().add(getClass().getResource("/styles/dashboard.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/dashboard-dark.css").toExternalForm());
        if (ThemeService.isDarkMode()) {
            root.getStyleClass().add("dark-theme");
        }
        stage.setScene(scene);
        
        showProfile();
    }
    
    /**
     * Показать окно настроек. {@code onProfileChanged} вызывается после
     * закрытия окна — чтобы вызывающая сторона (аватар/логин в TopBar)
     * могла обновить своё отображение, если пользователь сменил аватар
     * или отображаемое имя.
     */
    public static void show(Window owner, Runnable onProfileChanged) {
        SettingsWindow window = new SettingsWindow(owner, onProfileChanged);
        window.stage.setOnHidden(e -> {
            if (onProfileChanged != null) {
                onProfileChanged.run();
            }
        });
        window.stage.showAndWait();
    }
    
    private Node createCategoryItem(String text, Runnable onSelect, boolean initiallyActive) {
        Label label = new Label(text);
        label.getStyleClass().add("settings-category-item");
        if (initiallyActive) {
            label.getStyleClass().add("settings-category-item-active");
        }
        label.setMaxWidth(Double.MAX_VALUE);
        label.setOnMouseClicked(e -> {
            for (Node sibling : ((VBox) label.getParent()).getChildren()) {
                sibling.getStyleClass().remove("settings-category-item-active");
            }
            label.getStyleClass().add("settings-category-item-active");
            onSelect.run();
        });
        return label;
    }
    
    // ------------------------------------------------------------------
    // Профиль (аватар + отображаемое имя) — перенесено из TopBar как есть.
    // ------------------------------------------------------------------
    private void showProfile() {
        contentArea.getChildren().clear();
        
        ApplicationState state = ApplicationState.getInstance();
        if (!state.isAuthenticated()) {
            contentArea.getChildren().add(sectionLabel("Профиль"));
            Label note = new Label("Сначала войдите в систему, чтобы настроить профиль учётной записи.");
            note.setWrapText(true);
            contentArea.getChildren().add(note);
            return;
        }
        
        String username = state.getUsername();
        boolean accountIsSaved = AccountStorageService.isSaved(username);
        
        contentArea.getChildren().add(sectionLabel("Профиль"));
        
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
            File selected = fileChooser.showOpenDialog(stage);
            if (selected != null) {
                chosenAvatarFile[0] = selected;
                refreshAvatarPreview.run();
            }
        });
        
        HBox avatarRow = new HBox(16, avatarPreview, chooseAvatarButton);
        avatarRow.setAlignment(Pos.CENTER_LEFT);
        contentArea.getChildren().add(avatarRow);
        
        if (!accountIsSaved) {
            Label note = new Label("Учётная запись \"" + username + "\" не сохранена (не отмечена \"Сохранить "
                    + "данные для входа\" при входе) — настройки профиля доступны только для сохранённых "
                    + "учётных записей.");
            note.setWrapText(true);
            note.getStyleClass().add("account-picker-status");
            contentArea.getChildren().add(note);
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
        
        Button saveButton = new Button("Сохранить");
        saveButton.setOnAction(e -> {
            if (chosenAvatarFile[0] != null) {
                AccountStorageService.saveAvatar(username, chosenAvatarFile[0]);
            }
            String newDisplayName = displayNameField.getText();
            AccountStorageService.setDisplayName(username, newDisplayName == null ? "" : newDisplayName.trim());
            if (onProfileChanged != null) {
                onProfileChanged.run();
            }
        });
        
        contentArea.getChildren().addAll(nameFieldLabel, displayNameField, saveButton);
    }
    
    // ------------------------------------------------------------------
    // Оформление (тема)
    // ------------------------------------------------------------------
    private void showAppearance() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(sectionLabel("Оформление"));
        
        Label description = new Label("Тема интерфейса");
        description.getStyleClass().add("account-picker-status");
        
        Button themeToggleButton = new Button();
        themeToggleButton.setText(ThemeService.isDarkMode() ? "☀ Переключить на светлую" : "🌙 Переключить на тёмную");
        themeToggleButton.setOnAction(e -> {
            ThemeService.toggle();
            themeToggleButton.setText(ThemeService.isDarkMode() ? "☀ Переключить на светлую" : "🌙 Переключить на тёмную");
            boolean dark = ThemeService.isDarkMode();
            if (dark && !stage.getScene().getRoot().getStyleClass().contains("dark-theme")) {
                stage.getScene().getRoot().getStyleClass().add("dark-theme");
            } else if (!dark) {
                stage.getScene().getRoot().getStyleClass().remove("dark-theme");
            }
        });
        
        contentArea.getChildren().addAll(description, themeToggleButton);
    }
    
    // ------------------------------------------------------------------
    // Обновление программы
    // ------------------------------------------------------------------
    private void showUpdate() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(sectionLabel("Обновление программы"));
        
        Label versionLabel = new Label("Текущая версия: " + VersionService.getCurrentVersionString());
        
        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        
        Button checkButton = new Button("Проверить обновления");
        Button downloadButton = new Button("Скачать и установить");
        downloadButton.setVisible(false);
        downloadButton.setManaged(false);
        
        UpdateCheckResult[] lastResult = new UpdateCheckResult[1];
        
        checkButton.setOnAction(e -> {
            checkButton.setDisable(true);
            statusLabel.setText("Проверка...");
            downloadButton.setVisible(false);
            downloadButton.setManaged(false);
            
            new Thread(() -> {
                UpdateCheckService service = new UpdateCheckService();
                UpdateCheckResult result = service.checkForUpdates();
                javafx.application.Platform.runLater(() -> {
                    checkButton.setDisable(false);
                    lastResult[0] = result;
                    if (!result.isSuccess()) {
                        statusLabel.setText("Не удалось проверить обновления: "
                                + result.getErrorMessage().orElse("неизвестная ошибка"));
                        return;
                    }
                    if (!result.isUpdateAvailable()) {
                        statusLabel.setText("У вас установлена последняя версия.");
                        return;
                    }
                    StringBuilder sb = new StringBuilder("Доступна новая версия: ")
                            .append(result.getLatestVersion().orElse("?"));
                    result.getReleaseNotes().filter(n -> !n.isBlank())
                            .ifPresent(notes -> sb.append("\n\n").append(notes));
                    statusLabel.setText(sb.toString());
                    if (result.getDownloadUrl().isPresent()) {
                        downloadButton.setVisible(true);
                        downloadButton.setManaged(true);
                    }
                });
            }).start();
        });
        
        downloadButton.setOnAction(e -> {
            if (lastResult[0] != null) {
                downloadUpdate(lastResult[0]);
            }
        });
        
        contentArea.getChildren().addAll(versionLabel, checkButton, downloadButton, statusLabel);
    }
    
    /**
     * Скачивает обновление и запускает установщик в режиме "/passive" —
     * та же логика, что раньше была в NavigationDrawer.
     */
    private void downloadUpdate(UpdateCheckResult result) {
        String assetName = result.getAssetName().orElse("RMC-Framework-update.exe");
        Path destination = Paths.get(System.getProperty("java.io.tmpdir"), assetName);
        
        Label progressTitle = new Label("Загрузка обновления");
        progressTitle.getStyleClass().add("institution-loading-title");
        
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(280);
        
        Label progressStatus = new Label("Подготовка...");
        progressStatus.getStyleClass().add("loading-status-small");
        
        VBox progressContent = new VBox(12, progressTitle, progressBar, progressStatus);
        progressContent.setPadding(new Insets(24, 24, 20, 24));
        progressContent.setAlignment(Pos.CENTER);
        progressContent.getStyleClass().add("app-dialog");
        
        Stage progressStage = new Stage(StageStyle.UTILITY);
        progressStage.setTitle("Обновление");
        progressStage.setResizable(false);
        progressStage.initModality(Modality.APPLICATION_MODAL);
        progressStage.initOwner(stage);
        Scene progressScene = new Scene(progressContent, 340, 140);
        progressScene.getStylesheets().add(getClass().getResource("/styles/dashboard.css").toExternalForm());
        progressScene.getStylesheets().add(getClass().getResource("/styles/dashboard-dark.css").toExternalForm());
        if (ThemeService.isDarkMode()) {
            progressContent.getStyleClass().add("dark-theme");
        }
        progressStage.setScene(progressScene);
        progressStage.show();
        
        new Thread(() -> {
            try {
                UpdateCheckService service = new UpdateCheckService();
                service.download(result.getDownloadUrl().orElseThrow(), destination, (bytesRead, totalBytes) -> {
                    if (totalBytes > 0) {
                        double fraction = (double) bytesRead / totalBytes;
                        javafx.application.Platform.runLater(() -> {
                            progressBar.setProgress(fraction);
                            progressStatus.setText(Math.round(fraction * 100) + "%   ("
                                    + (bytesRead / 1024 / 1024) + " МБ из " + (totalBytes / 1024 / 1024) + " МБ)");
                        });
                    }
                });
                
                javafx.application.Platform.runLater(() -> {
                    progressStage.close();
                    launchInstallerAndClose(destination);
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    progressStage.close();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Ошибка обновления");
                    alert.setHeaderText("Не удалось скачать обновление");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }
    
    private void launchInstallerAndClose(Path installerPath) {
        try {
            new ProcessBuilder(installerPath.toString(), "/passive").start();
            javafx.application.Platform.exit();
            System.exit(0);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка обновления");
            alert.setHeaderText("Не удалось запустить установщик");
            alert.setContentText("Файл сохранён: " + installerPath + "\n\nЗапустите его вручную.\n\n" + e.getMessage());
            alert.showAndWait();
        }
    }
    
    // ------------------------------------------------------------------
    // О программе
    // ------------------------------------------------------------------
    private void showAbout() {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(sectionLabel("О программе"));
        
        Label info = new Label(
                "RMC Framework\n\n"
                        + "Версия: " + VersionService.getCurrentVersionString() + "\n\n"
                        + "Приложение для работы с реестром образовательных программ\n"
                        + "через HTTP — без браузера и Selenium."
        );
        info.setWrapText(true);
        contentArea.getChildren().add(info);
    }
    
    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("filter-section-title");
        return label;
    }
}
