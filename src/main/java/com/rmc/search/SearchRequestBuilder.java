package com.rmc.search;

import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.parser.FilterType;
import com.rmc.ui.dynamic.DynamicFilterPane;
import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Строитель поискового запроса.
 * 
 * <p>Автоматически собирает параметры из:</p>
 * <ul>
 *   <li>DynamicFilterPane - динамическая панель фильтров</li>
 *   <li>FilterBinder - связыватель значений</li>
 *   <li>List<FilterDefinition> - определения фильтров</li>
 *   <li>Map<String, String> - просто Map</li>
 * </ul>
 * 
 * <p>Не использует жёстко прописанные параметры - только атрибут name из HTML.</p>
 */
public class SearchRequestBuilder {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private String baseUrl;
    private Map<String, String> parameters;
    private String searchPath;
    
    private SearchRequestBuilder() {
        this.parameters = new LinkedHashMap<>();
        this.searchPath = "/search/";
    }
    
    /**
     * Создать новый строитель.
     */
    public static SearchRequestBuilder create() {
        return new SearchRequestBuilder();
    }
    
    /**
     * Установить базовый URL.
     */
    public SearchRequestBuilder baseUrl(String url) {
        this.baseUrl = url;
        return this;
    }
    
    /**
     * Установить путь поиска.
     */
    public SearchRequestBuilder searchPath(String path) {
        this.searchPath = path;
        return this;
    }
    
    /**
     * Добавить параметры из DynamicFilterPane.
     * 
     * @param filterPane панель фильтров
     * @return this
     */
    public SearchRequestBuilder addFrom(DynamicFilterPane filterPane) {
        if (filterPane == null) {
            return this;
        }
        
        logger.debug("Adding parameters from DynamicFilterPane");
        
        Map<String, String> values = filterPane.getAllValues();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            addParameter(entry.getKey(), entry.getValue());
        }
        
        return this;
    }
    
    /**
     * Добавить параметры из FilterBinder.
     * 
     * @param binder связыватель
     * @return this
     */
    public SearchRequestBuilder addFrom(com.rmc.ui.dynamic.FilterBinder binder) {
        if (binder == null) {
            return this;
        }
        
        logger.debug("Adding parameters from FilterBinder");
        
        Map<String, String> values = binder.getAllValues();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            addParameter(entry.getKey(), entry.getValue());
        }
        
        return this;
    }
    
    /**
     * Добавить параметры из списка FilterDefinition.
     * 
     * <p>Извлекает текущие значения из UI.</p>
     * 
     * @param filters определения фильтров
     * @param binder связыватель для получения значений
     * @return this
     */
    public SearchRequestBuilder addFromFilters(List<FilterDefinition> filters, 
                                               com.rmc.ui.dynamic.FilterBinder binder) {
        if (filters == null || binder == null) {
            return this;
        }
        
        logger.debug("Adding parameters from FilterDefinitions");
        
        for (FilterDefinition filter : filters) {
            String name = filter.getName();
            if (name == null || name.isEmpty()) {
                continue;
            }
            
            String value = binder.getValue(name);
            if (value != null && !value.isEmpty()) {
                addParameter(name, value);
            }
        }
        
        return this;
    }
    
    /**
     * Добавить параметры из Map.
     * 
     * @param params параметры
     * @return this
     */
    public SearchRequestBuilder addFrom(Map<String, String> params) {
        if (params == null) {
            return this;
        }
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                addParameter(entry.getKey(), entry.getValue());
            }
        }
        
        return this;
    }
    
    /**
     * Добавить параметр.
     */
    public SearchRequestBuilder addParameter(String name, String value) {
        if (name != null && !name.isEmpty() && value != null && !value.isEmpty()) {
            parameters.put(name, value);
            logger.debug("Added parameter: {}={}", name, value);
        }
        return this;
    }
    
    /**
     * Добавить параметр только если значение не пустое.
     */
    public SearchRequestBuilder addParameterIfPresent(String name, String value) {
        if (name != null && value != null && !value.isEmpty()) {
            parameters.put(name, value);
        }
        return this;
    }
    
    /**
     * Добавить множественный параметр (для checkbox с одинаковым name).
     */
    public SearchRequestBuilder addMultiParameter(String name, List<String> values) {
        if (name == null || values == null || values.isEmpty()) {
            return this;
        }
        
        String combined = values.stream()
                .filter(v -> v != null && !v.isEmpty())
                .collect(Collectors.joining(","));
        
        if (!combined.isEmpty()) {
            parameters.put(name, combined);
        }
        
        return this;
    }
    
    /**
     * Удалить параметр.
     */
    public SearchRequestBuilder removeParameter(String name) {
        parameters.remove(name);
        return this;
    }
    
    /**
     * Очистить все параметры.
     */
    public SearchRequestBuilder clear() {
        parameters.clear();
        return this;
    }
    
    /**
     * Построить SearchRequest.
     */
    public SearchRequest build() {
        String fullBaseUrl = buildBaseUrl();
        
        SearchRequest request = SearchRequest.builder()
                .baseUrl(fullBaseUrl)
                .parameters(new LinkedHashMap<>(parameters))
                .build();
        
        logger.info(LOG_REQUEST_BUILT, request.getFullUrl());
        
        return request;
    }
    
    /**
     * Построить только URL без создания объекта.
     */
    public String buildUrl() {
        return build().getFullUrl();
    }
    
    /**
     * Построить базовый URL с путём поиска.
     */
    private String buildBaseUrl() {
        String base = baseUrl != null ? baseUrl : "https://rmc.example.com";
        
        String path = searchPath;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        
        return base + path.substring(1);
    }
    
    /**
     * Получить текущие параметры.
     */
    public Map<String, String> getParameters() {
        return new LinkedHashMap<>(parameters);
    }
    
    /**
     * Получить количество параметров.
     */
    public int getParameterCount() {
        return parameters.size();
    }
    
    /**
     * Проверить, есть ли параметры.
     */
    public boolean hasParameters() {
        return !parameters.isEmpty();
    }
    
    @Override
    public String toString() {
        return "SearchRequestBuilder{" +
                "baseUrl='" + baseUrl + '\'' +
                ", parameters=" + parameters +
                '}';
    }
    
    // Константы для логирования
    private static final String LOG_REQUEST_BUILT = "SearchRequest построен: {}";
}
