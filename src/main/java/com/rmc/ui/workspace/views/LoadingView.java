package com.rmc.ui.workspace.views;

import com.rmc.ui.workspace.WorkspaceView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Loading view - shows progress spinner.
 */
public class LoadingView extends VBox implements WorkspaceView {
    
    private final Label statusLabel;
    private final Label dotsLabel;
    private Timeline dotAnimation;
    
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
        
        getChildren().addAll(icon, statusLabel, dotsLabel, progress);
    }
    
    public void setStatus(String status) {
        statusLabel.setText(status);
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
    }
    
    private void startDotsAnimation() {
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
