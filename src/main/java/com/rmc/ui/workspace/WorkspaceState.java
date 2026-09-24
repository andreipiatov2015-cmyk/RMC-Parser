package com.rmc.ui.workspace;

/**
 * All possible states for the Workspace.
 */
public enum WorkspaceState {
    /** Account picker - shown at startup if there are saved accounts */
    ACCOUNT_PICKER,
    
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
    
    /** Избранные учреждения */
    FAVORITES,
    
    /** Подробная информация по конкретному избранному учреждению */
    INSTITUTION_DETAIL,
    
    /** RMCAI — чат с нейросетью по данным сайта */
    RMC_AI,
    
    /** Error occurred */
    ERROR
}
