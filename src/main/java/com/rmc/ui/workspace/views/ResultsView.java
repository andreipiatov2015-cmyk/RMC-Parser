package com.rmc.ui.workspace.views;

import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceView;
import com.rmc.ui.workspace.components.ActionButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Results view - shows analysis results placeholder.
 */
public class ResultsView extends VBox implements WorkspaceView {
    
    private final WorkspaceContainer container;
    private final ActionButton backButton;
    
    public ResultsView(WorkspaceContainer container) {
        this.container = container;
        
        getStyleClass().add("results-view");
        setSpacing(16);
        setAlignment(Pos.TOP_CENTER);
        
        // Title
        Label title = new Label("📊 Результаты анализа");
        title.getStyleClass().add("results-title");
        
        // Placeholder content
        VBox placeholder = new VBox();
        placeholder.setAlignment(Pos.CENTER);
        placeholder.setSpacing(16);
        placeholder.setPadding(new Insets(48));
        placeholder.getStyleClass().add("results-placeholder");
        placeholder.setMaxWidth(600);
        
        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size: 64px;");
        
        Label message = new Label("Результаты появятся после завершения анализа");
        message.setStyle("-fx-font-size: 16px; -fx-text-fill: #57606A;");
        
        placeholder.getChildren().addAll(icon, message);
        VBox.setVgrow(placeholder, Priority.ALWAYS);
        
        // Separator
        Separator separator = new Separator();
        
        // Back button
        backButton = new ActionButton("← Назад к фильтрам", ActionButton.Style.SECONDARY);
        backButton.setOnAction(e -> container.onBackToFilters());
        
        getChildren().addAll(title, placeholder, separator, backButton);
    }
    
    @Override
    public Pane getRoot() {
        return this;
    }
    
    @Override
    public void onEnter() {
        // Reset placeholder when entering
    }
}
