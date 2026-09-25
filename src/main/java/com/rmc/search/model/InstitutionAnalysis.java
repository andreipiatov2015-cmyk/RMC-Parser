package com.rmc.search.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Результат обработки одного учреждения. Считаются ОБА набора показателей
 * одновременно, независимо от того, что из этого пользователь решит
 * показать на экране (это регулируется отдельно, в UI, через список
 * галочек):
 *
 * <ul>
 *   <li>{@link #getFilteredStats()} — показатели только по тем программам
 *       учреждения, что вошли в отфильтрованный список (например, только
 *       программы для детей с ОВЗ). Считаются суммированием показателей
 *       со страниц отдельных программ. Сюда же синтетически добавляется
 *       {@link #FILTERED_PROGRAM_COUNT_LABEL} — количество программ этого
 *       учреждения, прошедших фильтр (это не показатель с сайта, а просто
 *       count самих найденных программ).</li>
 *   <li>{@link #getOverallStats()} — показатели со страницы учреждения
 *       целиком (/org/{id}/), то есть по ВСЕМ программам учреждения, без
 *       учёта фильтра. Это старая, изначальная логика подсчёта.</li>
 * </ul>
 */
public class InstitutionAnalysis {
    
    /**
     * Ключ синтетического показателя "сколько программ этого учреждения
     * прошло фильтр" — добавляется в {@link #getFilteredStats()} наравне
     * с показателями, которые реально вернул сайт, чтобы он точно так же
     * участвовал в списке галочек отображения и в экспорте.
     */
    public static final String FILTERED_PROGRAM_COUNT_LABEL = "Программ по фильтру";
    
    /**
     * Эвристическая оценка количества бюджетных/платных программ среди
     * отфильтрованных — НЕ официальный показатель с сайта (на странице
     * отдельной программы такого разделения просто нет), а грубая
     * прикидка по тексту цены на карточке программы в списке. Может
     * ошибаться на нестандартно оформленных карточках — отсюда пометка
     * "(оценка по цене)" прямо в названии.
     */
    public static final String PRICE_BUDGET_COUNT_LABEL = "Программ бюджетных (оценка по цене)";
    public static final String PRICE_PAID_COUNT_LABEL = "Программ платных (оценка по цене)";
    
    private final String organizationId;
    private final String organizationName;
    private final String organizationUrl;
    private final Map<String, Integer> filteredStats;
    private final Map<String, Integer> overallStats;
    private final boolean success;
    private final String errorMessage;
    
    private InstitutionAnalysis(Builder builder) {
        this.organizationId = builder.organizationId;
        this.organizationName = builder.organizationName;
        this.organizationUrl = builder.organizationUrl;
        this.filteredStats = Map.copyOf(builder.filteredStats);
        this.overallStats = Map.copyOf(builder.overallStats);
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
    
    /**
     * @return показатели только по программам, прошедшим фильтр (плюс
     * синтетический {@link #FILTERED_PROGRAM_COUNT_LABEL})
     */
    public Map<String, Integer> getFilteredStats() {
        return filteredStats;
    }
    
    /**
     * @return показатели со страницы учреждения целиком, без учёта фильтра
     */
    public Map<String, Integer> getOverallStats() {
        return overallStats;
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
        private Map<String, Integer> filteredStats = new LinkedHashMap<>();
        private Map<String, Integer> overallStats = new LinkedHashMap<>();
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
        
        public Builder filteredStats(Map<String, Integer> filteredStats) {
            this.filteredStats = new LinkedHashMap<>(filteredStats);
            return this;
        }
        
        public Builder overallStats(Map<String, Integer> overallStats) {
            this.overallStats = new LinkedHashMap<>(overallStats);
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
