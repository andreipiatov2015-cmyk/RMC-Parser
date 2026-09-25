package com.rmc.ui.icons;

import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.SVGPath;

/**
 * Строит JavaFX-узел из данных иконки {@link TablerIcons} — контурная
 * иконка на исходной сетке 24x24 со штрихом 2px (без заливки, как и всё
 * семейство Tabler), масштабируется до нужного размера в пикселях.
 *
 * <p>Цвет обводки задаётся через CSS-класс, а не жёстко в коде, чтобы
 * иконка сама следовала текущей теме (см. класс {@code tabler-icon} в
 * dashboard.css/dashboard-dark.css) — обычный текстовый серый цвет по
 * умолчанию, и белый вариант {@link #onPrimary} для иконок на цветных
 * кнопках (например, "Экспорт в Excel").</p>
 */
public final class TablerIcon {
    
    private static final double DEFAULT_SIZE = 16;
    
    private TablerIcon() {
    }
    
    public static SVGPath of(String pathData) {
        return of(pathData, DEFAULT_SIZE);
    }
    
    public static SVGPath of(String pathData, double sizePx) {
        SVGPath path = new SVGPath();
        path.setContent(pathData);
        path.getStyleClass().add("tabler-icon");
        path.setFill(Color.TRANSPARENT);
        path.setStrokeWidth(2);
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        
        double scale = sizePx / 24.0;
        path.setScaleX(scale);
        path.setScaleY(scale);
        return path;
    }
    
    /**
     * Для иконок поверх цветных кнопок (PRIMARY/DANGER в {@code
     * ActionButton}) — белая обводка вместо обычной серой, читаемая на
     * тёмном/цветном фоне в обеих темах.
     */
    public static SVGPath onPrimary(String pathData) {
        return onPrimary(pathData, DEFAULT_SIZE);
    }
    
    public static SVGPath onPrimary(String pathData, double sizePx) {
        SVGPath path = of(pathData, sizePx);
        path.getStyleClass().add("tabler-icon-on-primary");
        return path;
    }
}
