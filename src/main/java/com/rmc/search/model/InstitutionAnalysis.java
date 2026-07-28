package com.rmc.search.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Результат обработки одного учреждения: показатели с его страницы
 * (/org/{id}/), либо ошибка, если страницу не удалось получить/разобрать.
 */
public class InstitutionAnalysis {
    
    private final String organizationId;
    private final String organizationName;
    private final String organizationUrl;
    private final Map<String, Integer> stats;
    private final boolean success;
    private final String errorMessage;
    
    private InstitutionAnalysis(Builder builder) {
        this.organizationId = builder.organizationId;
        this.organizationName = builder.organizationName;
        this.organizationUrl = builder.organizationUrl;
        this.stats = Map.copyOf(builder.stats);
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public String getOrganizationName() {
        return organizationName;
    }
    
    /**
     * @return полная (абсолютная) ссылка на страницу учреждения, если известна
     */
    public Optional<String> getOrganizationUrl() {
        return Optional.ofNullable(organizationUrl);
    }
    
    public Map<String, Integer> getStats() {
        return stats;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }
    
    public static class Builder {
        
        private String organizationId;
        private String organizationName;
        private String organizationUrl;
        private Map<String, Integer> stats = new LinkedHashMap<>();
        private boolean success;
        private String errorMessage;
        
        public Builder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }
        
        public Builder organizationName(String organizationName) {
            this.organizationName = organizationName;
            return this;
        }
        
        public Builder organizationUrl(String organizationUrl) {
            this.organizationUrl = organizationUrl;
            return this;
        }
        
        public Builder stats(Map<String, Integer> stats) {
            this.stats = new LinkedHashMap<>(stats);
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        public InstitutionAnalysis build() {
            return new InstitutionAnalysis(this);
        }
    }
}
