package com.rmc.ui.workspace.views;

import com.rmc.ui.workspace.WorkspaceView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Loading view - shows progress spinner, with an optional cancel button
 * for long-running operations (например, обход множества учреждений).
 */
public class LoadingView extends VBox implements WorkspaceView {
    
    private final Label statusLabel;
    private final Label dotsLabel;
    private final Button cancelButton;
    private Timeline dotAnimation;
    private Runnable onCancel;
    
    public LoadingView() {
        getStyleClass().add("loading-view");
        setAlignment(Pos.CENTER);
        setSpacing(16);
        
        // Loading icon
        Label icon = new Label("⏳");
        icon.setStyle("-fx-font-size: 48px;");
        
        // Status text
        statusLabel = new Label("Загрузка...");
        statusLabel.getStyleClass().add("loading-text");
        
        // Animated dots
        dotsLabel = new Label("");
        dotsLabel.getStyleClass().add("loading-dots");
        
        // Progress indicator (simple)
        Label progress = new Label("[░░░░░░░░░░]");
        progress.getStyleClass().add("loading-progress");
        
        // Cancel button — виден только когда для текущей операции есть
        // смысл отмены (см. setCancellable); по умолчанию скрыт.
        cancelButton = new Button("Отменить");
        cancelButton.getStyleClass().add("loading-cancel-button");
        cancelButton.setOnAction(e -> {
            if (onCancel != null) {
                cancelButton.setDisable(true);
                cancelButton.setText("Отмена...");
                onCancel.run();
            }
        });
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
        
        getChildren().addAll(icon, statusLabel, dotsLabel, progress, cancelButton);
    }
    
    public void setStatus(String status) {
        statusLabel.setText(status);
    }
    
    /**
     * Включить/выключить кнопку "Отменить" для текущей операции загрузки.
     * Отдельная от {@link #setOnCancel} настройка — на случай, если экран
     * показывается для операции, которую отменять не имеет смысла (быстрая
     * загрузка страницы фильтров), кнопку можно просто не показывать.
     */
    public void setCancellable(boolean cancellable, Runnable onCancel) {
        this.onCancel = onCancel;
        cancelButton.setVisible(cancellable);
        cancelButton.setManaged(cancellable);
        cancelButton.setDisable(false);
        cancelButton.setText("Отменить");
    }
    
    @Override
    public Pane getRoot() {
        return this;
    }
    
    @Override
    public void onEnter() {
        startDotsAnimation();
    }
    
    @Override
    public void onExit() {
        stopDotsAnimation();
        // Сбрасываем на случай, если этот экран покажут заново для другой
        // операции без явного вызова setCancellable() — не хотим унести
        // отмену в контекст, где она уже не имеет смысла.
        setCancellable(false, null);
    }
    
    private void startDotsAnimation() {
        stopDotsAnimation();
        
        final int[] count = {0};
        dotAnimation = new Timeline(new KeyFrame(
            Duration.seconds(0.5),
            e -> {
                count[0] = (count[0] + 1) % 4;
                String dots = ".".repeat(count[0]);
                dotsLabel.setText(dots);
            }
        ));
        dotAnimation.setCycleCount(Timeline.INDEFINITE);
        dotAnimation.play();
    }
    
    private void stopDotsAnimation() {
        if (dotAnimation != null) {
            dotAnimation.stop();
        }
    }
}
