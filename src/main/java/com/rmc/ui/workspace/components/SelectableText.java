package com.rmc.ui.workspace.components;

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;

/**
 * Фабрика "выделяемого мышью текста" — нередактируемые TextField/TextArea
 * вместо обычного {@link javafx.scene.control.Label}.
 *
 * <p>JavaFX Label в принципе не умеет выделение текста мышью (протяжкой,
 * двойным кликом по слову, Ctrl+C) — это ограничение самого тулкита. А вот
 * TextField/TextArea умеют это "из коробки". Класс "selectable-text" в CSS
 * убирает у них вид поля ввода (рамку, фон, подсветку фокуса), так что
 * визуально они выглядят как обычный текст.</p>
 *
 * <p>Вынесено из {@code ResultsView} сюда, чтобы использовать тот же приём
 * и на других экранах результатов (например, "Соглашения"), не дублируя
 * код.</p>
 */
public final class SelectableText {

    private SelectableText() {
        // Утилитарный класс
    }

    /**
     * Нередактируемое однострочное поле вместо {@link javafx.scene.control.Label}.
     */
    public static TextField field(String text, String... styleClasses) {
        TextField field = new TextField(text != null ? text : "");
        field.setEditable(false);
        field.setFocusTraversable(true);
        field.getStyleClass().add("selectable-text");
        field.getStyleClass().addAll(styleClasses);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    /**
     * То же самое, что {@link #field}, но для текста, который может
     * переноситься на несколько строк (TextArea, а не TextField). Высота
     * подгоняется под содержимое автоматически (см. {@link #bindAutoHeight}).
     */
    public static TextArea area(String text, String... styleClasses) {
        TextArea area = new TextArea(text != null ? text : "");
        area.setEditable(false);
        area.setWrapText(true);
        area.setFocusTraversable(true);
        area.getStyleClass().add("selectable-text");
        area.getStyleClass().addAll(styleClasses);
        area.setMaxWidth(Double.MAX_VALUE);
        area.setPrefRowCount(1);
        bindAutoHeight(area);
        return area;
    }

    private static void bindAutoHeight(TextArea area) {
        Text measurer = new Text();
        measurer.textProperty().bind(area.textProperty());
        measurer.fontProperty().bind(area.fontProperty());

        Runnable resize = () -> {
            double width = area.getWidth();
            if (width <= 0) {
                return;
            }
            measurer.setWrappingWidth(Math.max(0, width - 24));
            double textHeight = measurer.getLayoutBounds().getHeight();
            area.setPrefHeight(textHeight + 22);
        };

        area.widthProperty().addListener((obs, oldV, newV) -> resize.run());
        area.textProperty().addListener((obs, oldV, newV) -> Platform.runLater(resize));
        Platform.runLater(resize);
    }
}
