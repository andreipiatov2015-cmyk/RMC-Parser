package com.rmc.ui.workspace.components;

import com.rmc.ui.icons.TablerIcon;
import com.rmc.ui.icons.TablerIcons;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Область из двух панелей, делящих свою ширину в пропорции {@code ratio}
 * (0 — вся ширина левой, 1 — вся правой), с плавным переключением между
 * тремя состояниями: только левая / поровну / только правая.
 *
 * <p>Раскладка считается вручную, а собственные минимальную/
 * предпочтительную ширину область объявляет нулевыми. Раньше панели
 * растягивались через привязку своей ширины к ширине контейнера, а
 * минимальная ширина самого контейнера считалась JavaFX как сумма
 * минимальных ширин панелей — замкнутый круг: при округлении вверх на
 * каждом кадре анимации контейнер "подрастал" на пиксель-другой, и после
 * нескольких нажатий на стрелки вся программа уезжала за правый край
 * экрана вместе с кнопками. Здесь ширина зависит только от того, что
 * выделил родитель, и обратной связи нет.</p>
 *
 * <p>Каждая панель и сама область обрезаются по своим границам, иначе
 * содержимое схлопнутой (нулевой ширины) панели торчало бы поверх
 * соседней.</p>
 */
public final class RatioSplitPane extends Region {

    private enum Mode { LEFT_ONLY, SPLIT, RIGHT_ONLY }

    private final Region left;
    private final Region right;
    private final DoubleProperty ratio = new SimpleDoubleProperty(0.5);
    private Mode currentMode = Mode.SPLIT;

    public RatioSplitPane(Region left, Region right) {
        this.left = left;
        this.right = right;
        getChildren().addAll(left, right);
        ratio.addListener((obs, oldValue, newValue) -> requestLayout());
        clipToBounds(this);
        clipToBounds(left);
        clipToBounds(right);
    }

    private static void clipToBounds(Region region) {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
    }

    @Override
    protected double computeMinWidth(double height) {
        return 0;
    }

    @Override
    protected double computePrefWidth(double height) {
        return 0;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 0;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 200;
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        double leftWidth = Math.round(width * (1.0 - ratio.get()));
        left.resizeRelocate(0, 0, leftWidth, height);
        right.resizeRelocate(leftWidth, 0, width - leftWidth, height);
    }

    /**
     * Три кнопки-переключателя "только левая / поровну / только правая" —
     * ставятся в ряд с кнопками действий под областью.
     */
    public HBox createToggleControls(String leftTooltip, String rightTooltip) {
        Label btnLeft = toggleButton(TablerIcons.CHEVRON_LEFT, leftTooltip, Mode.LEFT_ONLY);

        Label btnSplit = new Label("‖");
        btnSplit.getStyleClass().add("results-split-toggle-button");
        Tooltip.install(btnSplit, new Tooltip("Показать обе области поровну"));
        btnSplit.setOnMouseClicked(e -> animateTo(Mode.SPLIT));

        Label btnRight = toggleButton(TablerIcons.CHEVRON_RIGHT, rightTooltip, Mode.RIGHT_ONLY);

        HBox row = new HBox(6, btnLeft, btnSplit, btnRight);
        row.getStyleClass().add("results-split-toggle-row");
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private Label toggleButton(String iconPath, String tooltipText, Mode mode) {
        Label label = new Label();
        label.setGraphic(TablerIcon.of(iconPath, 18));
        label.getStyleClass().add("results-split-toggle-button");
        Tooltip.install(label, new Tooltip(tooltipText));
        label.setOnMouseClicked(e -> animateTo(mode));
        return label;
    }

    private void animateTo(Mode mode) {
        if (mode == currentMode) {
            return;
        }
        currentMode = mode;

        double target = switch (mode) {
            case LEFT_ONLY -> 0.0;
            case SPLIT -> 0.5;
            case RIGHT_ONLY -> 1.0;
        };

        new Timeline(new KeyFrame(Duration.millis(280),
                new KeyValue(ratio, target, Interpolator.EASE_BOTH))).play();
    }
}
