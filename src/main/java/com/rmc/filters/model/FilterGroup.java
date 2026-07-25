package com.rmc.filters.model;

import com.rmc.filters.parser.FilterDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Группа фильтров.
 * 
 * <p>Представляет логическую группу фильтров на форме.
 * Например, "Учреждение", "Реестры", "Статусы" и т.д.</p>
 */
public class FilterGroup {
    
    private final String name;
    private final String title;
    private final List<FilterDefinition> filters;
    
    public FilterGroup(String name, String title, List<FilterDefinition> filters) {
        this.name = name;
        this.title = title;
        this.filters = List.copyOf(filters);
    }
    
    public String getName() {
        return name;
    }
    
    public String getTitle() {
        return title;
    }
    
    public List<FilterDefinition> getFilters() {
        return filters;
    }
    
    public int getFilterCount() {
        return filters.size();
    }
    
    public boolean isEmpty() {
        return filters.isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("FilterGroup{name=%s, title=%s, filters=%d}", name, title, filters.size());
    }
    
    public static class Builder {
        private String name;
        private String title;
        private List<FilterDefinition> filters = new ArrayList<>();
        
        public Builder name(String name) { this.name = name; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder filters(List<FilterDefinition> filters) { this.filters = new ArrayList<>(filters); return this; }
        public Builder addFilter(FilterDefinition filter) { this.filters.add(filter); return this; }
        public FilterGroup build() { return new FilterGroup(name, title, filters); }
    }
}
