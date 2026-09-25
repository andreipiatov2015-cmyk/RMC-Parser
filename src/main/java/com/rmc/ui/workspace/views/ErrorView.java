package com.rmc.ui.workspace.views;

import com.rmc.ui.icons.TablerIcon;
import com.rmc.ui.icons.TablerIcons;
import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceView;
import com.rmc.ui.workspace.components.ActionButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Error view - shows error message with retry option.
 */
public class ErrorView extends VBox implements WorkspaceView {
    
    private final WorkspaceContainer container;
    private final Label errorLabel;
    private final ActionButton retryButton;
    private final ActionButton backButton;
    
    public ErrorView(WorkspaceContainer container) {
        this.container = container;
        
        getStyleClass().add("error-view");
        setAlignment(Pos.CENTER);
        setSpacing(16);
        
        // Error icon
        Label icon = new Label();
        icon.setGraphic(TablerIcon.of(TablerIcons.ALERT_TRIANGLE, 64));
        
        // Title
        Label title = new Label("Произошла ошибка");
        title.getStyleClass().add("error-title");
        
        // Error message
        errorLabel = new Label();
        errorLabel.getStyleClass().add("error-message");
        
        // Retry button
        retryButton = new ActionButton("Повторить", ActionButton.Style.PRIMARY);
        retryButton.setGraphic(TablerIcon.onPrimary(TablerIcons.REFRESH, 14));
        retryButton.setOnAction(e -> container.onRetry());
        retryButton.setMaxWidth(200);
        
        // Back button
        backButton = new ActionButton("Назад", ActionButton.Style.SECONDARY);
        backButton.setGraphic(TablerIcon.of(TablerIcons.ARROW_LEFT, 14));
        backButton.setOnAction(e -> container.onBackToFilters());
        backButton.setMaxWidth(200);
        
        getChildren().addAll(icon, title, errorLabel, retryButton, backButton);
    }
    
    public void setError(String error) {
        errorLabel.setText(error);
    }
    
    @Override
    public Pane getRoot() {
        return this;
    }
    
    @Override
    public void onEnter() {
        errorLabel.setText("Произошла ошибка при выполнении операции");
    }
}
