package com.rmc.ui.workspace.views;

import com.rmc.ui.workspace.WorkspaceContainer;
import com.rmc.ui.workspace.WorkspaceView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Экран "RMCAI" — заготовка под будущий чат с нейросетью по данным сайта.
 *
 * <p>Пока это заглушка: сама модель, индекс и логика поиска ещё не
 * готовы (собираются и обучаются отдельно, см. {@code com.rmc.corpus}
 * и Python-часть проекта). Задача этого класса на данном этапе —
 * закрепить место в интерфейсе и в навигации, чтобы UI-часть не
 * переделывалась, когда AI-часть будет готова: тогда здесь заменится
 * содержимое {@link #buildContent()} на настоящий чат, а сама точка
 * входа (пункт меню → этот экран) останется той же.</p>
 *
 * <p>Кнопка "Загрузить AI-агента" тоже пока ничего не скачивает —
 * реальная логика загрузки появится, когда будет понятен состав и вес
 * AI-пакета (модель + индекс). См. обсуждение по хотбару в
 * {@code StatusBar} — прогресс загрузки будет показываться там.</p>
 */
public class RmcAiView extends VBox implements WorkspaceView {

    private final WorkspaceContainer container;

    public RmcAiView(WorkspaceContainer container) {
        this.container = container;

        getStyleClass().add("account-picker-view");
        setAlignment(Pos.TOP_CENTER);
        setSpacing(24);
        setPadding(new Insets(24));

        getChildren().add(buildContent());
    }

    private VBox buildContent() {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER);
        box.setSpacing(16);
        box.setMaxWidth(480);

        Label title = new Label("🤖 RMCAI");
        title.getStyleClass().add("account-picker-title");

        Label status = new Label("AI-агент ещё не установлен");
        status.getStyleClass().add("status-item");

        Label description = new Label(
                "Здесь будет чат с нейросетью, которая знает содержимое сайта "
                        + "и умеет отвечать на вопросы по программам, учреждениям и другим "
                        + "разделам. Пока идёт подготовка данных и обучение — сам чат "
                        + "появится здесь позже, без необходимости искать его в новом месте.");
        description.setWrapText(true);
        description.setAlignment(Pos.CENTER);
        description.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button downloadButton = new Button("Загрузить AI-агента");
        downloadButton.getStyleClass().add("action-button-primary");
        downloadButton.setDisable(true);
        downloadButton.setOnAction(e -> {
            // Заглушка: реальная загрузка появится вместе с готовым AI-пакетом.
        });

        Label comingSoon = new Label("(пока недоступно — в разработке)");
        comingSoon.getStyleClass().add("status-item");

        Button backButton = new Button("← Назад");
        backButton.getStyleClass().add("action-button-secondary");
        backButton.setOnAction(e -> container.onBackToFilters());

        box.getChildren().addAll(title, status, description, downloadButton, comingSoon, backButton);
        return box;
    }

    @Override
    public Pane getRoot() {
        return this;
    }
}
