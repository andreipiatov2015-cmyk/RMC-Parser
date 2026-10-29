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

    /** Загрузка страницы фильтров раздела "Соглашения" (/cooperates/) */
    COOPERATES_LOADING_FILTERS,

    /** Фильтры "Соглашения" загружены, можно настраивать и запускать анализ */
    COOPERATES_FILTERS_READY,

    /** Выполняется анализ раздела "Соглашения" */
    COOPERATES_ANALYZING,

    /** Показ результатов анализа раздела "Соглашения" */
    COOPERATES_RESULTS,

    /** Загрузка данных раздела "Параметры РМЦ" (фильтры и карточки трёх страниц) */
    PARAMS_ANALYZING,

    /** Показ данных раздела "Параметры РМЦ" */
    PARAMS_READY,

    /** Error occurred */
    ERROR
}
