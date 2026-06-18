package com.rmc.filters.builder;

import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.parser.FilterOption;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Строитель запроса из выбранных значений фильтров.
 * 
 * <p>Формирует query string совместимый с сервером.</p>
 */
public class FilterQueryBuilder {
    
    private final Map<String, String> values;
    private final Map<String, FilterDefinition> definitions;
    
    public FilterQueryBuilder() {
        this.values = new LinkedHashMap<>();
        this.definitions = new HashMap<>();
    }
    
    /**
     * Добавить фильтр и его значение.
     */
    public FilterQueryBuilder addFilter(String name, String value, FilterDefinition definition) {
        if (value != null && !value.isEmpty()) {
            values.put(name, value);
            definitions.put(name, definition);
        }
        return this;
    }
    
    /**
     * Добавить значение для фильтра.
     */
    public FilterQueryBuilder addValue(String name, String value) {
        if (value != null && !value.isEmpty()) {
            values.put(name, value);
        }
        return this;
    }
    
    /**
     * Добавить несколько значений для множественного выбора.
     */
    public FilterQueryBuilder addValues(String name, List<String> values) {
        if (values != null && !values.isEmpty()) {
            // Для множественного выбора используем формат name__in=value1,value2
            String joined = String.join(",", values);
            this.values.put(name + "__in", joined);
        }
        return this;
    }
    
    /**
     * Добавить значение для select фильтра с учётом label-to-value mapping.
     */
    public FilterQueryBuilder addSelectValue(String name, String label, FilterDefinition definition) {
        if (label == null || label.isEmpty()) {
            return this;
        }
        
        // Ищем value по label
        if (definition != null && definition.hasOptions()) {
            for (FilterOption option : definition.getOptions()) {
                if (label.equals(option.getLabel())) {
                    values.put(name, option.getValue());
                    definitions.put(name, definition);
                    return this;
                }
            }
        }
        
        // Если label не найден, используем как есть
        return addValue(name, label);
    }
    
    /**
     * Построить query string.
     */
    public String toQueryString() {
        return values.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }
    
    /**
     * Построить query string с префиксом.
     */
    public String toQueryString(String prefix) {
        return values.entrySet().stream()
                .map(e -> encode(prefix + e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }
    
    /**
     * Получить Map значений.
     */
    public Map<String, String> toMap() {
        return new LinkedHashMap<>(values);
    }
    
    /**
     * Получить количество установленных значений.
     */
    public int size() {
        return values.size();
    }
    
    /**
     * Проверить, есть ли значения.
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }
    
    /**
     * Очистить все значения.
     */
    public void clear() {
        values.clear();
        definitions.clear();
    }
    
    /**
     * Получить значение по имени.
     */
    public Optional<String> getValue(String name) {
        return Optional.ofNullable(values.get(name));
    }
    
    /**
     * Получить все значения.
     */
    public Collection<String> getValues() {
        return values.values();
    }
    
    /**
     * Получить все имена параметров.
     */
    public Set<String> getNames() {
        return values.keySet();
    }
    
    /**
     * Получить определение фильтра по имени.
     */
    public Optional<FilterDefinition> getDefinition(String name) {
        return Optional.ofNullable(definitions.get(name));
    }
    
    private String encode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
    
    @Override
    public String toString() {
        return "FilterQueryBuilder{" + toQueryString() + "}";
    }
}
