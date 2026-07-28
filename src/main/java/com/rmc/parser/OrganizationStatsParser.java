package com.rmc.parser;

import com.rmc.logging.AppLogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Парсер страницы учреждения (/org/{id}/).
 *
 * <p>Извлекает числовые показатели из блоков статистики Semantic UI
 * (.statistic &gt; .value + .label), например:</p>
 * <pre>
 * &lt;div class="statistic centered"&gt;
 *     &lt;div class="value centered"&gt;78&lt;/div&gt;
 *     &lt;div class="label centered"&gt;Зачислений по Бюджету&lt;/div&gt;
 * &lt;/div&gt;
 * </pre>
 *
 * <p>Подписи не хардкодятся — извлекаются все .statistic-блоки, какие
 * реально есть на странице, поэтому если на сайте добавится/уберётся
 * какой-то показатель, парсер подхватит это автоматически.</p>
 */
public class OrganizationStatsParser {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private OrganizationStatsParser() {
        // Утилитарный класс
    }
    
    public static ParseResult parse(String html) {
        logger.info(LOG_PARSING_START);
        
        if (html == null || html.isEmpty()) {
            logger.warn(LOG_EMPTY_HTML);
            return ParseResult.builder()
                    .success(false)
                    .errorMessage("HTML пуст или null")
                    .build();
        }
        
        try {
            Document document = Jsoup.parse(html);
            Map<String, Integer> stats = new LinkedHashMap<>();
            
            Elements statisticBlocks = document.select(".statistic");
            for (Element block : statisticBlocks) {
                Element valueEl = block.selectFirst(".value");
                Element labelEl = block.selectFirst(".label");
                
                if (valueEl == null || labelEl == null) {
                    continue;
                }
                
                String label = labelEl.text().trim();
                if (label.isEmpty()) {
                    continue;
                }
                
                Integer value = parseNumber(valueEl.text());
                if (value == null) {
                    continue;
                }
                
                // merge, а не put — на случай если один и тот же показатель
                // встретится в нескольких блоках .statistic на странице.
                stats.merge(label, value, Integer::sum);
            }
            
            logger.info(LOG_PARSING_COMPLETE, stats.size());
            
            return ParseResult.builder()
                    .success(true)
                    .stats(stats)
                    .build();
                    
        } catch (Exception e) {
            logger.error(LOG_PARSING_ERROR, e.getMessage());
            return ParseResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    private static Integer parseNumber(String text) {
        if (text == null) {
            return null;
        }
        String digits = text.replaceAll("[^0-9-]", "");
        if (digits.isEmpty() || "-".equals(digits)) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    // Константы для логирования
    private static final String LOG_PARSING_START = "Разбор страницы учреждения...";
    private static final String LOG_PARSING_COMPLETE = "Разбор завершён. Найдено показателей: {}";
    private static final String LOG_PARSING_ERROR = "Ошибка разбора страницы учреждения: {}";
    private static final String LOG_EMPTY_HTML = "HTML пуст";
    
    /**
     * Результат парсинга страницы учреждения.
     */
    public static class ParseResult {
        
        private final boolean success;
        private final Map<String, Integer> stats;
        private final String errorMessage;
        
        private ParseResult(Builder builder) {
            this.success = builder.success;
            this.stats = Map.copyOf(builder.stats);
            this.errorMessage = builder.errorMessage;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        /**
         * @return показатели вида "подпись" -&gt; число, в порядке появления на странице
         */
        public Map<String, Integer> getStats() {
            return stats;
        }
        
        public Optional<String> getErrorMessage() {
            return Optional.ofNullable(errorMessage);
        }
        
        public static class Builder {
            
            private boolean success;
            private Map<String, Integer> stats = new LinkedHashMap<>();
            private String errorMessage;
            
            public Builder success(boolean success) {
                this.success = success;
                return this;
            }
            
            public Builder stats(Map<String, Integer> stats) {
                this.stats = new LinkedHashMap<>(stats);
                return this;
            }
            
            public Builder errorMessage(String message) {
                this.errorMessage = message;
                return this;
            }
            
            public ParseResult build() {
                return new ParseResult(this);
            }
        }
    }
}
