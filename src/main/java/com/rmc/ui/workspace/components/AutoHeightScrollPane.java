package com.rmc.ui.workspace.components;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

/**
 * ScrollPane, высота которого подстраивается под содержимое: пока
 * содержимое (например, FlowPane с карточками, переносящимися на
 * несколько строк) умещается в {@code maxHeight} — область ровно по его
 * размеру, без пустого места; если не умещается — высота упирается в
 * {@code maxHeight} и появляется прокрутка.
 *
 * <p>Высота считается напрямую из предпочтительной высоты содержимого при
 * текущей ширине, а не привязкой к фактической высоте содержимого после
 * раскладки — такая привязка запаздывала на один проход и давала
 * урезанную область.</p>
 */
public class AutoHeightScrollPane extends ScrollPane {

    private final Region content;
    private final double maxHeightLimit;

    public AutoHeightScrollPane(Region content, double maxHeightLimit) {
        super(content);
        this.content = content;
        this.maxHeightLimit = maxHeightLimit;
        setFitToWidth(true);
        setMinHeight(USE_PREF_SIZE);
        // Ширина изменилась -> изменилось число строк у FlowPane -> пересчитать высоту.
        widthProperty().addListener((obs, oldValue, newValue) -> requestLayout());
        // Содержимое поменялось (добавили/убрали карточки) -> пересчитать высоту.
        content.layoutBoundsProperty().addListener((obs, oldValue, newValue) -> requestLayout());
    }

    /** Пересчитать высоту после ручной смены содержимого. */
    public void refreshSize() {
        requestLayout();
    }

    @Override
    protected double computePrefHeight(double width) {
        double horizontalInsets = getInsets().getLeft() + getInsets().getRight();
        double verticalInsets = getInsets().getTop() + getInsets().getBottom();
        double available = (width > 0 ? width : getWidth()) - horizontalInsets;
        double contentHeight = available > 0 ? content.prefHeight(available) : content.prefHeight(-1);
        return Math.min(contentHeight + verticalInsets, maxHeightLimit);
    }

    @Override
    protected double computeMaxHeight(double width) {
        return maxHeightLimit;
    }
}
