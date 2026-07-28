package com.rmc.parser.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Одна группа программы — с карточки в блоке "Действующих групп: N" на
 * странице конкретной программы. Показатели (модуль, вместимость,
 * сколько сейчас обучается) хранятся как есть в исходном виде "подпись —
 * значение", как и на сайте, без хардкода конкретных подписей — если
 * сайт добавит новую строку в таблицу карточки, она тоже попадёт сюда.
 */
public class ProgramGroup {
    
    private final String name;
    private final String dateRange;
    private final Map<String, String> details;
    
    private ProgramGroup(Builder builder) {
        this.name = builder.name;
        this.dateRange = builder.dateRange;
        this.details = Map.copyOf(builder.details);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * @return название/номер группы, например "№1, 1 г.о. 2025"
     */
    public String getName() {
        return name;
    }
    
    public Optional<String> getDateRange() {
        return Optional.ofNullable(dateRange);
    }
    
    /**
     * @return все строки таблицы карточки группы ("подпись" -&gt; "значение")
     */
    public Map<String, String> getDetails() {
        return details;
    }
    
    /**
     * Найти значение по подписи строки (без учёта регистра) — например
     * "Сейчас обучается" или "Максимальное количество детей в группе".
     */
    public Optional<String> getDetail(String label) {
        if (label == null) {
            return Optional.empty();
        }
        return details.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getKey().equalsIgnoreCase(label))
                .map(Map.Entry::getValue)
                .findFirst();
    }
    
    /**
     * @return число из значения строки по подписи, если строка есть и
     * значение — число
     */
    public Optional<Integer> getDetailAsInt(String label) {
        return getDetail(label).map(v -> {
            try {
                String digits = v.replaceAll("[^0-9-]", "");
                return digits.isEmpty() ? null : Integer.parseInt(digits);
            } catch (NumberFormatException e) {
                return null;
            }
        });
    }
    
    public static class Builder {
        
        private String name;
        private String dateRange;
        private Map<String, String> details = new LinkedHashMap<>();
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder dateRange(String dateRange) {
            this.dateRange = dateRange;
            return this;
        }
        
        public Builder details(Map<String, String> details) {
            this.details = new LinkedHashMap<>(details);
            return this;
        }
        
        public ProgramGroup build() {
            return new ProgramGroup(this);
        }
    }
}
