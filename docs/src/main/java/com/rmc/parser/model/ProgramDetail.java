package com.rmc.parser.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Данные со страницы конкретной программы (/programs/{id}/): показатели
 * зачислений и список действующих групп с количеством детей в каждой.
 */
public class ProgramDetail {
    
    private final String programId;
    private final String title;
    private final boolean success;
    private final String errorMessage;
    private final Map<String, Integer> stats;
    private final Integer activeGroupsCount;
    private final List<ProgramGroup> groups;
    
    private ProgramDetail(Builder builder) {
        this.programId = builder.programId;
        this.title = builder.title;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
        this.stats = Map.copyOf(builder.stats);
        this.activeGroupsCount = builder.activeGroupsCount;
        this.groups = List.copyOf(builder.groups);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public String getProgramId() {
        return programId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }
    
    /**
     * @return показатели вида "подпись" -&gt; число (например
     * "Зачислений бюджетных" -&gt; 90), взятые с той же ".statistic"-разметки,
     * что и на странице учреждения
     */
    public Map<String, Integer> getStats() {
        return stats;
    }
    
    public Optional<Integer> getActiveGroupsCount() {
        return Optional.ofNullable(activeGroupsCount);
    }
    
    public List<ProgramGroup> getGroups() {
        return groups;
    }
    
    /**
     * @return суммарно "Сейчас обучается" по всем группам программы —
     * фактическое число детей, а не заявленное в статистике
     */
    public int getTotalCurrentlyEnrolled() {
        int total = 0;
        for (ProgramGroup group : groups) {
            total += group.getDetailAsInt("Сейчас обучается").orElse(0);
        }
        return total;
    }
    
    public static class Builder {
        
        private String programId;
        private String title;
        private boolean success = true;
        private String errorMessage;
        private Map<String, Integer> stats = new LinkedHashMap<>();
        private Integer activeGroupsCount;
        private List<ProgramGroup> groups = new ArrayList<>();
        
        public Builder programId(String programId) {
            this.programId = programId;
            return this;
        }
        
        public Builder title(String title) {
            this.title = title;
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
        
        public Builder stats(Map<String, Integer> stats) {
            this.stats = new LinkedHashMap<>(stats);
            return this;
        }
        
        public Builder activeGroupsCount(Integer activeGroupsCount) {
            this.activeGroupsCount = activeGroupsCount;
            return this;
        }
        
        public Builder groups(List<ProgramGroup> groups) {
            this.groups = new ArrayList<>(groups);
            return this;
        }
        
        public ProgramDetail build() {
            return new ProgramDetail(this);
        }
    }
}
