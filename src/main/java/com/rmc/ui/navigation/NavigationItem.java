package com.rmc.ui.navigation;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * Navigation menu item.
 */
public class NavigationItem extends HBox {
    
    private final Label icon;
    private final Label text;
    private final Runnable handler;
    
    public NavigationItem(String icon, String text, Runnable handler) {
        this.handler = handler;
        
        getStyleClass().add("navigation-item");
        setAlignment(Pos.CENTER_LEFT);
        setPadding(new Insets(10, 20, 10, 20));
        setSpacing(12);
        
        this.icon = new Label(icon);
        this.icon.getStyleClass().add("nav-icon");
        
        this.text = new Label(text);
        this.text.getStyleClass().add("nav-text");
        
        getChildren().addAll(this.icon, this.text);
        
        setOnMouseClicked(e -> handler.run());
        setOnMouseEntered(e -> getStyleClass().add("navigation-item:hover"));
        setOnMouseExited(e -> getStyleClass().remove("navigation-item:hover"));
    }
}
