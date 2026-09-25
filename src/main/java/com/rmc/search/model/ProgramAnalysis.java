package com.rmc.search.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Результат обработки одной отдельной программы, прошедшей фильтр —
 * то же самое, что суммируется в {@link InstitutionAnalysis#getFilteredStats()}
 * по учреждению целиком, но здесь сохранено на уровне одной программы,
 * чтобы показать раздел "По программам" отдельно от "По учреждениям".
 */
public class ProgramAnalysis {
    
    /**
     * Ключ эвристической оценки "бюджетная или платная программа" — не
     * официальный показатель с сайта программы (там такого просто нет),
     * а грубая прикидка по тексту цены на карточке программы в списке.
     * См. {@link com.rmc.parser.model.Program#getPrice()}.
     */
    public static final String PRICE_CATEGORY_LABEL = "Тип по цене (оценка)";
    
    private final String programId;
    private final String programTitle;
    private final String programUrl;
    private final String organizationName;
    private final String organizationId;
    private final Map<String, Integer> filteredStats;
    private final String priceCategoryEstimate;
    private final boolean success;
    private final String errorMessage;
    
    private ProgramAnalysis(Builder builder) {
        this.programId = builder.programId;
        this.programTitle = builder.programTitle;
        this.programUrl = builder.programUrl;
        this.organizationName = builder.organizationName;
        this.organizationId = builder.organizationId;
        this.filteredStats = Map.copyOf(builder.filteredStats);
        this.priceCategoryEstimate = builder.priceCategoryEstimate;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public String getProgramId() {
        return programId;
    }
    
    public String getProgramTitle() {
        return programTitle;
    }
    
    public Optional<String> getProgramUrl() {
        return Optional.ofNullable(programUrl);
    }
    
    public String getOrganizationName() {
        return organizationName;
    }
    
    public String getOrganizationId() {
        return organizationId;
    }
    
    public Map<String, Integer> getFilteredStats() {
        return filteredStats;
    }
    
    /**
     * @return "Бюджетная" / "Платная" — эвристическая оценка по тексту
     * цены на карточке, либо пусто, если цена не распозналась
     */
    public Optional<String> getPriceCategoryEstimate() {
        return Optional.ofNullable(priceCategoryEstimate);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }
    
    public static class Builder {
        
        private String programId;
        private String programTitle;
        private String programUrl;
        private String organizationName;
        private String organizationId;
        private Map<String, Integer> filteredStats = new LinkedHashMap<>();
        private String priceCategoryEstimate;
        private boolean success;
        private String errorMessage;
        
        public Builder programId(String programId) {
            this.programId = programId;
            return this;
        }
        
        public Builder programTitle(String programTitle) {
            this.programTitle = programTitle;
            return this;
        }
        
        public Builder programUrl(String programUrl) {
            this.programUrl = programUrl;
            return this;
        }
        
        public Builder organizationName(String organizationName) {
            this.organizationName = organizationName;
            return this;
        }
        
        public Builder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }
        
        public Builder filteredStats(Map<String, Integer> filteredStats) {
            this.filteredStats = new LinkedHashMap<>(filteredStats);
            return this;
        }
        
        public Builder priceCategoryEstimate(String priceCategoryEstimate) {
            this.priceCategoryEstimate = priceCategoryEstimate;
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
        
        public ProgramAnalysis build() {
            return new ProgramAnalysis(this);
        }
    }
}
