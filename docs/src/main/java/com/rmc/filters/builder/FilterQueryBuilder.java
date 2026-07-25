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
    private final Map<String, List<String>> multiValues;
    private final Map<String, FilterDefinition> definitions;
    
    public FilterQueryBuilder() {
        this.values = new LinkedHashMap<>();
        this.multiValues = new LinkedHashMap<>();
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
     * 
     * <p>На сайте имя мультиселекта уже оканчивается на "__in"
     * (например, "federal_project__in") — дописывать суффикс повторно
     * нельзя. Значения отдаются как повторяющиеся пары "name=v1&name=v2",
     * как это делает обычный браузер при отправке &lt;select multiple&gt;
     * через GET (а не одним параметром через запятую).</p>
     */
    public FilterQueryBuilder addValues(String name, List<String> values) {
        if (name == null || name.isEmpty() || values == null || values.isEmpty()) {
            return this;
        }
        
        List<String> nonEmpty = values.stream()
                .filter(v -> v != null && !v.isEmpty())
                .collect(Collectors.toList());
        
        if (!nonEmpty.isEmpty()) {
            multiValues.put(name, nonEmpty);
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
        List<String> pairs = new ArrayList<>();
        
        for (Map.Entry<String, String> e : values.entrySet()) {
            pairs.add(encode(e.getKey()) + "=" + encode(e.getValue()));
        }
        
        for (Map.Entry<String, List<String>> e : multiValues.entrySet()) {
            String encodedName = encode(e.getKey());
            for (String v : e.getValue()) {
                pairs.add(encodedName + "=" + encode(v));
            }
        }
        
        return String.join("&", pairs);
    }
    
    /**
     * Построить query string с префиксом.
     */
    public String toQueryString(String prefix) {
        List<String> pairs = new ArrayList<>();
        
        for (Map.Entry<String, String> e : values.entrySet()) {
            pairs.add(encode(prefix + e.getKey()) + "=" + encode(e.getValue()));
        }
        
        for (Map.Entry<String, List<String>> e : multiValues.entrySet()) {
            String encodedName = encode(prefix + e.getKey());
            for (String v : e.getValue()) {
                pairs.add(encodedName + "=" + encode(v));
            }
        }
        
        return String.join("&", pairs);
    }
    
    /**
     * Получить Map значений (только одиночные; для мультизначений см. {@link #getMultiValues()}).
     */
    public Map<String, String> toMap() {
        return new LinkedHashMap<>(values);
    }
    
    /**
     * Получить Map мультизначений.
     */
    public Map<String, List<String>> getMultiValues() {
        return new LinkedHashMap<>(multiValues);
    }
    
    /**
     * Получить количество установленных значений (одиночных и мультизначений вместе).
     */
    public int size() {
        return values.size() + multiValues.size();
    }
    
    /**
     * Проверить, есть ли значения.
     */
    public boolean isEmpty() {
        return values.isEmpty() && multiValues.isEmpty();
    }
    
    /**
     * Очистить все значения.
     */
    public void clear() {
        values.clear();
        multiValues.clear();
        definitions.clear();
    }
    
    /**
     * Получить значение по имени.
     */
    public Optional<String> getValue(String name) {
        return Optional.ofNullable(values.get(name));
    }
    
    /**
     * Получить все значения (только одиночные).
     */
    public Collection<String> getValues() {
        return values.values();
    }
    
    /**
     * Получить все имена параметров (одиночных и мультизначений).
     */
    public Set<String> getNames() {
        Set<String> names = new LinkedHashSet<>(values.keySet());
        names.addAll(multiValues.keySet());
        return names;
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
