package com.rmc.filters.ui;

import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.TextAlignment;

/**
 * Chip component for multi-select.
 * 
 * <p>Displays a selected value with a close button.</p>
 */
public class Chip extends HBox {
    
    private static final PseudoClass CLOSED_PSEUDO_CLASS = PseudoClass.getPseudoClass("closed");
    
    private final Label label;
    private final Region closeButton;
    private EventHandler<ActionEvent> closeHandler;
    
    public Chip(String text) {
        getStyleClass().add("chip");
        setSpacing(6);
        setPadding(new Insets(4, 8, 4, 10));
        setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        label = new Label(text);
        label.setTextAlignment(TextAlignment.CENTER);
        
        closeButton = new Region();
        closeButton.getStyleClass().add("chip-close");
        closeButton.setMinSize(16, 16);
        closeButton.setPrefSize(16, 16);
        
        // Геометрия иконки (SVG-shape) задаётся инлайн, а цвет — через CSS-класс
        // .chip-close, чтобы hover-подсветка из stylesheet реально работала:
        // inline -fx-background-color всегда перекрывал бы правила из CSS.
        closeButton.setStyle(
                "-fx-cursor: hand; " +
                "-fx-shape: \"M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12\"; " +
                "-fx-background-size: 12px; " +
                "-fx-background-repeat: no-repeat; " +
                "-fx-background-position: center;");
        
        closeButton.setOnMouseClicked(e -> {
            if (closeHandler != null) {
                closeHandler.handle(null);
            }
        });
        
        closeButton.hoverProperty().addListener((obs, old, isHover) -> {
            closeButton.pseudoClassStateChanged(CLOSED_PSEUDO_CLASS, isHover);
        });
        
        getChildren().addAll(label, closeButton);
    }
    
    public void setText(String text) {
        label.setText(text);
    }
    
    public String getText() {
        return label.getText();
    }
    
    public void setOnCloseHandler(EventHandler<ActionEvent> handler) {
        this.closeHandler = handler;
    }
}
