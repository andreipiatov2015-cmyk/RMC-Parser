package com.rmc.ui.workspace;

import javafx.scene.layout.Pane;

/**
 * Interface for workspace views.
 */
public interface WorkspaceView {
    
    /**
     * Get the root pane for this view.
     */
    Pane getRoot();
    
    /**
     * Called when this view becomes active.
     */
    default void onEnter() {}
    
    /**
     * Called when leaving this view.
     */
    default void onExit() {}
}
