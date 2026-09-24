package com.rmc.ui.workspace.views;

import com.rmc.ui.workspace.WorkspaceView;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Loading view - крутящийся индикатор с заголовком и живым статусом
 * (тот же стиль, что и у загрузки данных учреждения в "Избранном"), с
 * необязательной кнопкой "Отменить" для долгих операций (например, обход
 * множества учреждений).
 *
 * <p>Когда вызывающая сторона знает точное количество шагов (например,
 * "учреждение N из M" при анализе), дополнительно показывается полоса
 * прогресса с процентом и примерной оценкой оставшегося времени — она
 * скрыта, пока количество шагов неизвестно (например, во время загрузки
 * самого списка фильтров, до начала обхода учреждений).</p>
 */
public class LoadingView extends VBox implements WorkspaceView {
    
    private final Label titleLabel;
    private final Label statusLabel;
    private final ProgressBar progressBar;
    private final Label etaLabel;
    private final Button cancelButton;
    private Runnable onCancel;
    private long startTimeMillis = -1;
    
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
        
        progressBar = new ProgressBar(0);
        progressBar.getStyleClass().add("loading-progress-bar");
        progressBar.setPrefWidth(280);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        
        etaLabel = new Label();
        etaLabel.getStyleClass().add("loading-eta-label");
        etaLabel.setVisible(false);
        etaLabel.setManaged(false);
        
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
        
        getChildren().addAll(progressIndicator, titleLabel, statusLabel, progressBar, etaLabel, cancelButton);
    }
    
    public void setStatus(String status) {
        statusLabel.setText(status);
    }
    
    /**
     * Сбрасывает полосу прогресса и точку отсчёта времени — вызывать перед
     * началом новой долгой операции (до первого {@link #setProgress}).
     */
    public void resetProgress() {
        startTimeMillis = -1;
        progressBar.setProgress(0);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        etaLabel.setVisible(false);
        etaLabel.setManaged(false);
    }
    
    /**
     * Обновляет полосу прогресса и оценку оставшегося времени.
     * {@code total <= 0} означает "количество шагов пока неизвестно" —
     * полоса и оценка времени в этом случае скрываются (например, во
     * время загрузки страниц списка программ, до того как стало известно
     * число учреждений).
     */
    public void setProgress(int current, int total) {
        if (total <= 0) {
            progressBar.setVisible(false);
            progressBar.setManaged(false);
            etaLabel.setVisible(false);
            etaLabel.setManaged(false);
            return;
        }
        
        if (startTimeMillis < 0) {
            startTimeMillis = System.currentTimeMillis();
        }
        
        double fraction = Math.max(0, Math.min(1.0, (double) current / total));
        progressBar.setProgress(fraction);
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        
        if (current > 0) {
            long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
            long estimatedTotalMillis = (long) (elapsedMillis / fraction);
            long remainingMillis = Math.max(0, estimatedTotalMillis - elapsedMillis);
            etaLabel.setText(Math.round(fraction * 100) + "%   •   осталось примерно "
                    + formatDuration(remainingMillis));
            etaLabel.setVisible(true);
            etaLabel.setManaged(true);
        }
    }
    
    private String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (minutes <= 0) {
            return seconds + " сек";
        }
        return minutes + " мин " + seconds + " сек";
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
        resetProgress();
    }
}
