package com.rmc.ui.workspace.views;

import com.rmc.ui.workspace.WorkspaceView;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Loading view - крутящийся индикатор с заголовком и живым статусом
 * (тот же стиль, что и у загрузки данных учреждения в "Избранном"), с
 * необязательной кнопкой "Отменить" для долгих операций (например, обход
 * множества учреждений).
 */
public class LoadingView extends VBox implements WorkspaceView {
    
    private final Label titleLabel;
    private final Label statusLabel;
    private final Button cancelButton;
    private Runnable onCancel;
    
    public LoadingView() {
        getStyleClass().add("loading-view");
        setAlignment(Pos.CENTER);
        setSpacing(14);
        
        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(56, 56);
        progressIndicator.getStyleClass().add("institution-loading-spinner");
        
        titleLabel = new Label("Загрузка");
        titleLabel.getStyleClass().add("institution-loading-title");
        
        statusLabel = new Label("Пожалуйста, подождите...");
        statusLabel.getStyleClass().add("loading-text");
        
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
        
        getChildren().addAll(progressIndicator, titleLabel, statusLabel, cancelButton);
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
    public void onExit() {
        // Сбрасываем на случай, если этот экран покажут заново для другой
        // операции без явного вызова setCancellable() — не хотим унести
        // отмену в контекст, где она уже не имеет смысла.
        setCancellable(false, null);
    }
}
