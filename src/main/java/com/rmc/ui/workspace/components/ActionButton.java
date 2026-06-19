package com.rmc.ui.workspace.components;

import javafx.scene.control.Button;

/**
 * Styled action button.
 */
public class ActionButton extends Button {
    
    public enum Style {
        PRIMARY,
        SECONDARY,
        DANGER
    }
    
    private final Style style;
    
    public ActionButton(String text) {
        this(text, Style.PRIMARY);
    }
    
    public ActionButton(String text, Style style) {
        super(text);
        this.style = style;
        
        getStyleClass().add("action-button");
        getStyleClass().add("action-button-" + style.name().toLowerCase());
    }
}
