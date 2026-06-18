package com.rmc.filters.session;

import com.rmc.filters.builder.FilterQueryBuilder;
import com.rmc.filters.model.FilterGroup;
import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.parser.FilterOption;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Session for managing filter state.
 * 
 * <p>Stores all FilterDefinitions, selected values, and query string.
 * All other components work through FilterSession only.</p>
 */
public class FilterSession {
    
    private final List<FilterGroup> groups;
    private final Map<String, FilterDefinition> definitions;
    private final Map<String, String> singleValues;
    private final Map<String, Set<String>> multiValues;
    private final LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    
    private final List<Consumer<FilterSession>> listeners;
    
    public FilterSession(List<FilterGroup> groups) {
        this.groups = new ArrayList<>(groups);
        this.definitions = new HashMap<>();
        this.singleValues = new LinkedHashMap<>();
        this.multiValues = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        this.listeners = new CopyOnWriteArrayList<>();
        
        initDefinitions();
    }
    
    private void initDefinitions() {
        for (FilterGroup group : groups) {
            for (FilterDefinition def : group.getFilters()) {
                if (def.getName() != null && !def.getName().isEmpty()) {
                    definitions.put(def.getName(), def);
                    
                    // Initialize with default value if exists
                    String defaultValue = def.getValue();
                    if (defaultValue != null && !defaultValue.isEmpty()) {
                        singleValues.put(def.getName(), defaultValue);
                    }
                }
            }
        }
    }
    
    /**
     * Set single value for filter.
     */
    public void setValue(String name, String value) {
        if (value == null || value.isEmpty()) {
            singleValues.remove(name);
        } else {
            singleValues.put(name, value);
        }
        updateTimestamp();
        notifyListeners();
    }
    
    /**
     * Get single value for filter.
     */
    public String getValue(String name) {
        return singleValues.get(name);
    }
    
    /**
     * Add value for multi-select filter.
     */
    public void addMultiValue(String name, String value) {
        if (value == null || value.isEmpty()) return;
        
        multiValues.computeIfAbsent(name, k -> new HashSet<>()).add(value);
        updateTimestamp();
        notifyListeners();
    }
    
    /**
     * Remove value from multi-select filter.
     */
    public void removeMultiValue(String name, String value) {
        if (value == null) return;
        
        Set<String> values = multiValues.get(name);
        if (values != null) {
            values.remove(value);
            if (values.isEmpty()) {
                multiValues.remove(name);
            }
        }
        updateTimestamp();
        notifyListeners();
    }
    
    /**
     * Get all selected values for multi-select.
     */
    public Set<String> getMultiValues(String name) {
        return multiValues.getOrDefault(name, Collections.emptySet());
    }
    
    /**
     * Clear all values for a filter.
     */
    public void clearValue(String name) {
        singleValues.remove(name);
        multiValues.remove(name);
        updateTimestamp();
        notifyListeners();
    }
    
    /**
     * Clear all filter values.
     */
    public void clearAll() {
        singleValues.clear();
        multiValues.clear();
        updateTimestamp();
        notifyListeners();
    }
    
    /**
     * Build query string from current values.
     */
    public String buildQueryString() {
        FilterQueryBuilder builder = new FilterQueryBuilder();
        
        // Single values
        for (Map.Entry<String, String> entry : singleValues.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            
            if (value != null && !value.isEmpty()) {
                builder.addValue(name, value);
            }
        }
        
        // Multi values
        for (Map.Entry<String, Set<String>> entry : multiValues.entrySet()) {
            String name = entry.getKey();
            Set<String> values = entry.getValue();
            
            if (values != null && !values.isEmpty()) {
                builder.addValues(name, new ArrayList<>(values));
            }
        }
        
        return builder.toQueryString();
    }
    
    /**
     * Get count of active filters.
     */
    public int getActiveFilterCount() {
        int count = 0;
        count += singleValues.values().stream().filter(v -> v != null && !v.isEmpty()).count();
        count += multiValues.values().stream().mapToInt(Set::size).sum();
        return count;
    }
    
    /**
     * Add listener for changes.
     */
    public void addListener(Consumer<FilterSession> listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove listener.
     */
    public void removeListener(Consumer<FilterSession> listener) {
        listeners.remove(listener);
    }
    
    private void updateTimestamp() {
        this.lastUpdated = LocalDateTime.now();
    }
    
    private void notifyListeners() {
        for (Consumer<FilterSession> listener : listeners) {
            try {
                listener.accept(this);
            } catch (Exception e) {
                // Ignore listener errors
            }
        }
    }
    
    // Getters
    
    public List<FilterGroup> getGroups() {
        return groups;
    }
    
    public List<FilterDefinition> getAllFilters() {
        List<FilterDefinition> all = new ArrayList<>();
        for (FilterGroup group : groups) {
            all.addAll(group.getFilters());
        }
        return all;
    }
    
    public Optional<FilterDefinition> getDefinition(String name) {
        return Optional.ofNullable(definitions.get(name));
    }
    
    public Map<String, FilterDefinition> getDefinitions() {
        return definitions;
    }
    
    public Map<String, String> getSingleValues() {
        return singleValues;
    }
    
    public Map<String, Set<String>> getMultiValues() {
        return multiValues;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }
    
    public int getTotalFilterCount() {
        return definitions.size();
    }
    
    @Override
    public String toString() {
        return String.format("FilterSession{filters=%d, active=%d}", 
                getTotalFilterCount(), getActiveFilterCount());
    }
}
