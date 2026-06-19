package com.rmc.ui.workspace;

/**
 * All possible states for the Workspace.
 */
public enum WorkspaceState {
    /** Login screen */
    AUTH,
    
    /** Loading filter page after login */
    LOADING_FILTERS,
    
    /** Filters ready, user can configure and search */
    FILTERS_READY,
    
    /** Performing search/analysis */
    ANALYZING,
    
    /** Showing results */
    RESULTS,
    
    /** Error occurred */
    ERROR
}
