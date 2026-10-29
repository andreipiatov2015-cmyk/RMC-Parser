package com.rmc.ui.topbar;

import com.rmc.ui.icons.TablerIcon;
import com.rmc.ui.icons.TablerIcons;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Переключатель раздела ("Реестр программ" / "Соглашения" / "Параметры РМЦ") в TopBar —
 * круглые значки, которые при наведении мыши "раскрываются" в пилюлю с
 * подписью, а соседний значок при этом естественно отъезжает в сторону
 * (это просто следствие того, что оба значка лежат в одном {@link HBox} —
 * когда один становится шире, второй сдвигается сам, без какой-либо
 * дополнительной анимации сдвига).
 *
 * <p>Текущий раздел всегда показан "раскрытым" (как при наведении), чтобы
 * было видно, где сейчас находится пользователь, даже когда мышь не над
 * переключателем.</p>
 */
public class SectionSwitcher extends HBox {

    private final Pill registryPill;
    private final Pill cooperatesPill;
    private final Pill paramsPill;

    private Runnable onShowRegistry;
    private Runnable onShowCooperates;
    private Runnable onShowParams;

    public SectionSwitcher() {
        getStyleClass().add("section-switcher");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);

        registryPill = new Pill(TablerIcons.LIST, "Реестр программ");
        cooperatesPill = new Pill(TablerIcons.FILE_TEXT, "Соглашения");
        paramsPill = new Pill(TablerIcons.SETTINGS, "Параметры РМЦ");

        registryPill.setOnAction(() -> {
            setActive(registryPill);
            if (onShowRegistry != null) {
                onShowRegistry.run();
            }
        });
        cooperatesPill.setOnAction(() -> {
            setActive(cooperatesPill);
            if (onShowCooperates != null) {
                onShowCooperates.run();
            }
        });

        paramsPill.setOnAction(() -> {
            setActive(paramsPill);
            if (onShowParams != null) {
                onShowParams.run();
            }
        });

        getChildren().addAll(registryPill, cooperatesPill, paramsPill);

        // "Реестр программ" — раздел, с которым приложение всегда
        // начинает работу (он и был единственным разделом раньше).
        setActive(registryPill);
    }

    private void setActive(Pill active) {
        registryPill.setActive(active == registryPill);
        cooperatesPill.setActive(active == cooperatesPill);
        paramsPill.setActive(active == paramsPill);
    }

    public void setOnShowRegistry(Runnable handler) {
        this.onShowRegistry = handler;
    }

    public void setOnShowCooperates(Runnable handler) {
        this.onShowCooperates = handler;
    }

    public void setOnShowParams(Runnable handler) {
        this.onShowParams = handler;
    }

    /**
     * Один значок переключателя: круглая иконка + текстовый "хвост",
     * ширина которого анимируется от 0 (только иконка, кружок) до
     * естественной ширины надписи (раскрытая пилюля). Хвост обрезается
     * прямоугольным clip, чтобы текст не торчал поверх соседних элементов,
     * пока ширина меньше его естественной.
     */
    private static final class Pill extends HBox {

        /** Высота пилюли — та же, что и у соседних значков TopBar (топбар-иконка:
         *  16px иконка + 8px отступ сверху/снизу = 32px). Фиксированная высота,
         *  а не "по содержимому" — иначе текст внутри тянул бы пилюлю выше. */
        private static final double HEIGHT = 32;

        private final StackPane textClip;
        private final double naturalTextWidth;
        private boolean active;
        private Runnable onAction;

        Pill(String iconPath, String text) {
            getStyleClass().add("section-switcher-pill");
            setAlignment(Pos.CENTER);
            setPrefHeight(HEIGHT);
            setMinHeight(HEIGHT);
            setMaxHeight(HEIGHT);

            Label icon = new Label();
            icon.setGraphic(TablerIcon.of(iconPath, 14));
            icon.getStyleClass().add("section-switcher-icon");

            Label textLabel = new Label(text);
            textLabel.getStyleClass().add("section-switcher-text");

            // Естественная ширина надписи — считаем один раз через
            // временный Text (тот же приём, что и в SelectableText для
            // авто-высоты TextArea), а не через layoutBounds самого
            // Label, которые до первого прохода layout не готовы.
            Text measurer = new Text(text);
            measurer.setFont(Font.font(12));
            this.naturalTextWidth = measurer.getLayoutBounds().getWidth() + 6;

            // StackPane, а не Pane — чтобы надпись была по центру ПО
            // ВЫСОТЕ пилюли автоматически, без ручного позиционирования.
            textClip = new StackPane(textLabel);
            textClip.setAlignment(Pos.CENTER_LEFT);
            textClip.setMinWidth(0);
            textClip.setPrefWidth(0);
            textClip.setMaxWidth(0);
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(textClip.widthProperty());
            clip.heightProperty().bind(textClip.heightProperty());
            textClip.setClip(clip);

            getChildren().addAll(icon, textClip);

            setOnMouseEntered(e -> {
                if (!active) {
                    animateTo(naturalTextWidth);
                }
            });
            setOnMouseExited(e -> {
                if (!active) {
                    animateTo(0);
                }
            });
            setOnMouseClicked(e -> {
                if (onAction != null) {
                    onAction.run();
                }
            });
        }

        void setActive(boolean active) {
            this.active = active;
            getStyleClass().removeAll("section-switcher-pill-active");
            if (active) {
                getStyleClass().add("section-switcher-pill-active");
                animateTo(naturalTextWidth);
            } else {
                animateTo(0);
            }
        }

        void setOnAction(Runnable handler) {
            this.onAction = handler;
        }

        private void animateTo(double targetWidth) {
            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(220),
                            new KeyValue(textClip.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                            new KeyValue(textClip.minWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                            new KeyValue(textClip.maxWidthProperty(), targetWidth, Interpolator.EASE_BOTH))
            );
            timeline.play();
        }
    }
}
