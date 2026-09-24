package com.rmc.search.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Итог анализа по выбранным фильтрам: общее количество найденных программ
 * и учреждений, суммарные показатели по всем учреждениям (отдельно —
 * только по отфильтрованным программам, отдельно — по учреждениям целиком)
 * и разбивка по каждому учреждению отдельно.
 */
public class AnalysisResult {
    
    private final boolean success;
    private final boolean cancelled;
    private final int totalPrograms;
    private final int totalInstitutions;
    private final Map<String, Integer> filteredTotals;
    private final Map<String, Integer> overallTotals;
    private final List<InstitutionAnalysis> institutions;
    private final String errorMessage;
    
    private AnalysisResult(Builder builder) {
        this.success = builder.success;
        this.cancelled = builder.cancelled;
        this.totalPrograms = builder.totalPrograms;
        this.totalInstitutions = builder.totalInstitutions;
        this.filteredTotals = Map.copyOf(builder.filteredTotals);
        this.overallTotals = Map.copyOf(builder.overallTotals);
        this.institutions = List.copyOf(builder.institutions);
        this.errorMessage = builder.errorMessage;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * @return true, если анализ был прерван пользователем (кнопка
     * "Отменить"), а не завершился ошибкой сам по себе
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    public int getTotalPrograms() {
        return totalPrograms;
    }
    
    public int getTotalInstitutions() {
        return totalInstitutions;
    }
    
    /**
     * @return суммарные показатели ("подпись" -&gt; сумма по всем учреждениям)
     * только по программам, прошедшим фильтр
     */
    public Map<String, Integer> getFilteredTotals() {
        return filteredTotals;
    }
    
    /**
     * @return суммарные показатели по учреждениям целиком, без учёта
     * фильтра (старая логика подсчёта)
     */
    public Map<String, Integer> getOverallTotals() {
        return overallTotals;
    }
    
    public List<InstitutionAnalysis> getInstitutions() {
        return institutions;
    }
    
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }
    
    public static class Builder {
        
        private boolean success;
        private boolean cancelled;
        private int totalPrograms;
        private int totalInstitutions;
        private Map<String, Integer> filteredTotals = new LinkedHashMap<>();
        private Map<String, Integer> overallTotals = new LinkedHashMap<>();
        private List<InstitutionAnalysis> institutions = new ArrayList<>();
        private String errorMessage;
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder cancelled(boolean cancelled) {
            this.cancelled = cancelled;
            return this;
        }
        
        public Builder totalPrograms(int totalPrograms) {
            this.totalPrograms = totalPrograms;
            return this;
        }
        
        public Builder totalInstitutions(int totalInstitutions) {
            this.totalInstitutions = totalInstitutions;
            return this;
        }
        
        public Builder filteredTotals(Map<String, Integer> filteredTotals) {
            this.filteredTotals = new LinkedHashMap<>(filteredTotals);
            return this;
        }
        
        public Builder overallTotals(Map<String, Integer> overallTotals) {
            this.overallTotals = new LinkedHashMap<>(overallTotals);
            return this;
        }
        
        public Builder institutions(List<InstitutionAnalysis> institutions) {
            this.institutions = new ArrayList<>(institutions);
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public AnalysisResult build() {
            return new AnalysisResult(this);
        }
    }
}
