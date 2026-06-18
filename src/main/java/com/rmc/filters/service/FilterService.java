package com.rmc.filters.service;

import com.rmc.filters.builder.FilterQueryBuilder;
import com.rmc.filters.loader.FilterPageLoader;
import com.rmc.filters.loader.FilterPageResult;
import com.rmc.filters.model.FilterGroup;
import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.parser.FilterParser;
import com.rmc.http.HttpClientService;
import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с фильтрами.
 * 
 * <p>Загружает страницу фильтров, парсит HTML и возвращает группы фильтров.</p>
 */
public class FilterService {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private final HttpClientService httpClient;
    private final String baseUrl;
    private FilterParser.ParseResult lastParseResult;
    
    public FilterService(HttpClientService httpClient, String baseUrl) {
        this.httpClient = httpClient;
        this.baseUrl = baseUrl;
    }
    
    /**
     * Загрузить фильтры с сервера.
     * 
     * @return результат загрузки
     */
    public FilterLoadResult loadFilters() {
        logger.info(LOG_LOADING_FILTERS);
        logger.info(LOG_USING_ACTIVE_SESSION);
        
        try {
            // Загружаем страницу
            FilterPageLoader loader = FilterPageLoader.builder()
                    .httpClient(httpClient)
                    .baseUrl(baseUrl)
                    .build();
            
            FilterPageResult pageResult = loader.load();
            
            if (!pageResult.isSuccess()) {
                logger.error(LOG_LOAD_ERROR, pageResult.getErrorMessage().orElse("unknown"));
                return FilterLoadResult.failure("Ошибка загрузки: " + pageResult.getErrorMessage().orElse("unknown"));
            }
            
            String html = pageResult.getHtml();
            int statusCode = pageResult.getStatusCode();
            int size = html != null ? html.length() : 0;
            
            logger.info(LOG_HTTP_STATUS, statusCode);
            logger.info(LOG_HTML_SIZE, size);
            logger.info(LOG_PARSING_HTML);
            
            // Парсим HTML
            FilterParser.ParseResult parseResult = FilterParser.parse(html);
            this.lastParseResult = parseResult;
            
            if (!parseResult.isSuccess()) {
                logger.error(LOG_PARSE_ERROR, parseResult.getErrorMessage().orElse("unknown"));
                return FilterLoadResult.failure("Ошибка парсинга: " + parseResult.getErrorMessage().orElse("unknown"));
            }
            
            List<FilterDefinition> filters = parseResult.getFilters();
            
            logger.info(LOG_FILTERS_FOUND, filters.size());
            logger.info(LOG_BUILDING_UI);
            
            // Создаём группы фильтров
            List<FilterGroup> groups = createGroups(filters);
            
            logger.info(LOG_UI_BUILT);
            
            return FilterLoadResult.success(groups, filters.size());
            
        } catch (Exception e) {
            logger.error(LOG_GENERAL_ERROR, e.getMessage());
            return FilterLoadResult.failure("Ошибка: " + e.getMessage());
        }
    }
    
    /**
     * Создать группы фильтров из списка определений.
     * Пока создаём одну группу - позже можно добавить логику разделения по div/fieldset.
     */
    private List<FilterGroup> createGroups(List<FilterDefinition> filters) {
        List<FilterGroup> groups = new ArrayList<>();
        
        // Группируем по первому символу или просто создаём одну группу
        // В будущем можно анализировать HTML структуру для создания настоящих групп
        
        // Пока все фильтры в одной группе
        FilterGroup mainGroup = new FilterGroup.Builder()
                .name("main")
                .title("Параметры поиска")
                .filters(filters)
                .build();
        
        groups.add(mainGroup);
        
        return groups;
    }
    
    /**
     * Получить результат последнего парсинга.
     */
    public Optional<FilterParser.ParseResult> getLastParseResult() {
        return Optional.ofNullable(lastParseResult);
    }
    
    /**
     * Построить query string из текущих значений.
     */
    public String buildQuery(FilterQueryBuilder builder) {
        return builder.toQueryString();
    }
    
    // Константы логирования
    private static final String LOG_LOADING_FILTERS = "==================================================";
    private static final String LOG_LOADING_FILTERS2 = "Получение страницы фильтров";
    private static final String LOG_LOADING_FILTERS3 = "==================================================";
    private static final String LOG_USING_ACTIVE_SESSION = "Используется активная HTTP-сессия";
    private static final String LOG_HTTP_STATUS = "HTTP Status: {}";
    private static final String LOG_HTML_SIZE = "HTML получен. Размер: {} символов";
    private static final String LOG_PARSING_HTML = "Передача страницы в FilterParser";
    private static final String LOG_FILTERS_FOUND = "Найдено фильтров: {}";
    private static final String LOG_BUILDING_UI = "Создание элементов интерфейса...";
    private static final String LOG_UI_BUILT = "Интерфейс фильтров успешно построен.";
    private static final String LOG_LOAD_ERROR = "Ошибка загрузки страницы: {}";
    private static final String LOG_PARSE_ERROR = "Ошибка парсинга HTML: {}";
    private static final String LOG_GENERAL_ERROR = "Общая ошибка: {}";
    
    /**
     * Результат загрузки фильтров.
     */
    public static class FilterLoadResult {
        private final boolean success;
        private final List<FilterGroup> groups;
        private final int filterCount;
        private final String errorMessage;
        
        private FilterLoadResult(boolean success, List<FilterGroup> groups, int filterCount, String errorMessage) {
            this.success = success;
            this.groups = groups;
            this.filterCount = filterCount;
            this.errorMessage = errorMessage;
        }
        
        public static FilterLoadResult success(List<FilterGroup> groups, int filterCount) {
            return new FilterLoadResult(true, groups, filterCount, null);
        }
        
        public static FilterLoadResult failure(String errorMessage) {
            return new FilterLoadResult(false, List.of(), 0, errorMessage);
        }
        
        public boolean isSuccess() { return success; }
        public List<FilterGroup> getGroups() { return groups; }
        public int getFilterCount() { return filterCount; }
        public Optional<String> getErrorMessage() { return Optional.ofNullable(errorMessage); }
    }
}
