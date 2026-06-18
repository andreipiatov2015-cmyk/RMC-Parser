package com.rmc.ui.dashboard;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Log pane with auto-scroll, search, clear, and save functionality.
 */
public class LogPane extends VBox {
    
    private final ObservableList<String> logMessages = FXCollections.observableArrayList();
    private ListView<String> logListView;
    private TextField searchField;
    private ObservableList<String> filteredMessages = FXCollections.observableArrayList();
    
    public LogPane() {
        getStyleClass().add("card");
        getStyleClass().add("log-pane");
        setPadding(new Insets(20));
        setSpacing(16);
        
        Label titleLabel = new Label("Журнал работы");
        titleLabel.getStyleClass().add("card-title");
        
        // Toolbar
        HBox toolbar = createToolbar();
        
        // Log list
        logListView = new ListView<>();
        logListView.setItems(filteredMessages);
        logListView.getStyleClass().add("log-list");
        VBox.setVgrow(logListView, Priority.ALWAYS);
        
        getChildren().addAll(titleLabel, toolbar, logListView);
        
        // Start listening for log events
        setupLogListener();
    }
    
    private HBox createToolbar() {
        HBox toolbar = new HBox();
        toolbar.setSpacing(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        
        // Search field
        searchField = new TextField();
        searchField.setPromptText("Поиск по журналу...");
        searchField.getStyleClass().add("search-field");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, old, newVal) -> filterLogs(newVal));
        HBox.setHgrow(searchField, Priority.NEVER);
        
        // Clear button
        Button clearBtn = new Button("Очистить журнал");
        clearBtn.getStyleClass().add("toolbar-button");
        clearBtn.setOnAction(e -> clearLogs());
        
        // Save button
        Button saveBtn = new Button("Сохранить журнал");
        saveBtn.getStyleClass().add("toolbar-button");
        saveBtn.setOnAction(e -> saveLogs());
        
        toolbar.getChildren().addAll(searchField, clearBtn, saveBtn);
        
        return toolbar;
    }
    
    private void setupLogListener() {
        // Add initial message
        addLog("Журнал инициализирован");
    }
    
    private void filterLogs(String query) {
        filteredMessages.clear();
        if (query == null || query.isEmpty()) {
            filteredMessages.addAll(logMessages);
        } else {
            String lowerQuery = query.toLowerCase();
            for (String msg : logMessages) {
                if (msg.toLowerCase().contains(lowerQuery)) {
                    filteredMessages.add(msg);
                }
            }
        }
    }
    
    public void addLog(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String formattedMessage = "[" + timestamp + "] " + message;
        
        logMessages.add(0, formattedMessage);
        
        // Limit to 1000 messages
        if (logMessages.size() > 1000) {
            logMessages.remove(logMessages.size() - 1);
        }
        
        // Update filtered
        String searchQuery = searchField.getText();
        filterLogs(searchQuery);
        
        // Auto-scroll
        if (!filteredMessages.isEmpty()) {
            logListView.scrollTo(0);
        }
    }
    
    private void clearLogs() {
        logMessages.clear();
        filteredMessages.clear();
        addLog("Журнал очищен");
    }
    
    private void saveLogs() {
        TextInputDialog dialog = new TextInputDialog("application.log");
        dialog.setTitle("Сохранить журнал");
        dialog.setHeaderText("Введите имя файла");
        dialog.setContentText("Имя файла:");
        
        dialog.showAndWait().ifPresent(filename -> {
            try {
                String userHome = System.getProperty("user.home");
                String desktopPath = userHome + File.separator + "Desktop";
                
                // Try to save to desktop first
                File file = new File(desktopPath, filename);
                if (!file.getParentFile().exists()) {
                    // Fallback to user home
                    file = new File(userHome, filename);
                }
                
                java.nio.file.Files.writeString(
                    file.toPath(),
                    String.join(System.lineSeparator(), logMessages),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
                );
                
                addLog("Журнал сохранен: " + file.getAbsolutePath());
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Успешно");
                alert.setHeaderText("Журнал сохранен");
                alert.setContentText("Файл: " + file.getAbsolutePath());
                alert.show();
                
            } catch (Exception e) {
                addLog("Ошибка сохранения: " + e.getMessage());
                
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка");
                alert.setHeaderText("Ошибка сохранения журнала");
                alert.setContentText(e.getMessage());
                alert.show();
            }
        });
    }
    
    public ObservableList<String> getLogMessages() {
        return logMessages;
    }
}
