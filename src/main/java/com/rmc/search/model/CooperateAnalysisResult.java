package com.rmc.search.model;

import com.rmc.parser.model.Cooperate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Итог анализа по разделу "Соглашения" (аналог {@link AnalysisResult}, но
 * проще: данные уже полностью есть в самой таблице списка, без захода на
 * отдельные страницы заявок).
 */
public class CooperateAnalysisResult {

    public static final String STATUS_NEW_LABEL = "Новые заявки";
    public static final String STATUS_CONFIRMED_LABEL = "Подтверждённые";
    public static final String STATUS_REJECTED_LABEL = "Отклонённые";
    public static final String STATUS_UNKNOWN_LABEL = "Статус не определён";
    public static final String CONFIRMED_HOURS_TOTAL_LABEL = "Часов по подтверждённым соглашениям";

    private final boolean success;
    private final boolean cancelled;
    private final int totalCooperates;
    private final Map<String, Integer> statusTotals;
    private final List<Cooperate> cooperates;
    private final List<CooperateTerritoryStat> territoryStats;
    private final String errorMessage;

    private CooperateAnalysisResult(Builder builder) {
        this.success = builder.success;
        this.cancelled = builder.cancelled;
        this.totalCooperates = builder.totalCooperates;
        this.statusTotals = Map.copyOf(builder.statusTotals);
        this.cooperates = List.copyOf(builder.cooperates);
        this.territoryStats = List.copyOf(CooperateTerritoryStat.compute(builder.cooperates));
        this.errorMessage = builder.errorMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public int getTotalCooperates() {
        return totalCooperates;
    }

    /**
     * @return суммарные показатели ("подпись" -&gt; значение): количество
     * новых/подтверждённых/отклонённых заявок и суммарные часы по
     * подтверждённым соглашениям
     */
    public Map<String, Integer> getStatusTotals() {
        return statusTotals;
    }

    public List<Cooperate> getCooperates() {
        return cooperates;
    }

    /**
     * @return сводка по территориям и периодам (годам): сколько всего заявок
     * и часов в каждой территории за каждый год
     */
    public List<CooperateTerritoryStat> getTerritoryStats() {
        return territoryStats;
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    public static class Builder {

        private boolean success;
        private boolean cancelled;
        private int totalCooperates;
        private Map<String, Integer> statusTotals = new LinkedHashMap<>();
        private List<Cooperate> cooperates = new ArrayList<>();
        private String errorMessage;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder cancelled(boolean cancelled) {
            this.cancelled = cancelled;
            return this;
        }

        public Builder totalCooperates(int totalCooperates) {
            this.totalCooperates = totalCooperates;
            return this;
        }

        public Builder statusTotals(Map<String, Integer> statusTotals) {
            this.statusTotals = new LinkedHashMap<>(statusTotals);
            return this;
        }

        public Builder cooperates(List<Cooperate> cooperates) {
            this.cooperates = new ArrayList<>(cooperates);
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public CooperateAnalysisResult build() {
            return new CooperateAnalysisResult(this);
        }
    }
}
