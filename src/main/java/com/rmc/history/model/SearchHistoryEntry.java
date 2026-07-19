package com.rmc.history.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Одна запись истории поиска: применённые фильтры (в виде query-строки —
 * для повторного запуска — и человекочитаемой сводки для отображения) и
 * итог последнего запуска, если он был доведён до конца.
 */
public class SearchHistoryEntry {
    
    private final String id;
    private final LocalDateTime timestamp;
    private final String queryString;
    private final List<String> filterSummary;
    private final Integer totalPrograms;
    private final Integer totalInstitutions;
    
    private SearchHistoryEntry(Builder builder) {
        this.id = builder.id;
        this.timestamp = builder.timestamp;
        this.queryString = builder.queryString;
        this.filterSummary = List.copyOf(builder.filterSummary);
        this.totalPrograms = builder.totalPrograms;
        this.totalInstitutions = builder.totalInstitutions;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public String getId() {
        return id;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * @return query-строка (без ведущего "?"), пригодная для повторного запуска
     */
    public String getQueryString() {
        return queryString;
    }
    
    /**
     * @return человекочитаемые строки вида "Подпись: значение(я)"
     */
    public List<String> getFilterSummary() {
        return filterSummary;
    }
    
    public Integer getTotalPrograms() {
        return totalPrograms;
    }
    
    public Integer getTotalInstitutions() {
        return totalInstitutions;
    }
    
    public static class Builder {
        
        private String id;
        private LocalDateTime timestamp;
        private String queryString = "";
        private List<String> filterSummary = new ArrayList<>();
        private Integer totalPrograms;
        private Integer totalInstitutions;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder queryString(String queryString) {
            this.queryString = queryString;
            return this;
        }
        
        public Builder filterSummary(List<String> filterSummary) {
            this.filterSummary = new ArrayList<>(filterSummary);
            return this;
        }
        
        public Builder totalPrograms(Integer totalPrograms) {
            this.totalPrograms = totalPrograms;
            return this;
        }
        
        public Builder totalInstitutions(Integer totalInstitutions) {
            this.totalInstitutions = totalInstitutions;
            return this;
        }
        
        public SearchHistoryEntry build() {
            return new SearchHistoryEntry(this);
        }
    }
}
