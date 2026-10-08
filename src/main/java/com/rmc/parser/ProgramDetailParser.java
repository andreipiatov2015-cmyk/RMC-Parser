package com.rmc.parser;

import com.rmc.logging.AppLogger;
import com.rmc.parser.model.ProgramGroup;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Парсер страницы конкретной программы (/programs/{id}/).
 *
 * <p>Извлекает:</p>
 * <ul>
 *   <li>показатели зачислений — та же разметка ".statistic" (.value + .label),
 *       что и на странице учреждения, см. {@link OrganizationStatsParser};</li>
 *   <li>количество действующих групп — из заголовка вида
 *       "Действующих групп: 12" (ищем по тексту, не по конкретному классу —
 *       устойчивее к изменениям вёрстки);</li>
 *   <li>сами группы — карточки с классом "mcard", у каждой дата, название
 *       и таблица показателей (модуль, вместимость, сколько сейчас
 *       обучается);</li>
 *   <li>ссылка на "навигатор" (отдельный сайт, cabinet.ruobr.ru/navigator/...) —
 *       подпись к ней ("ссылка на навигатор:") генерируется CSS-правилом
 *       ::before и физически отсутствует в HTML, поэтому ссылку ищем по
 *       самому адресу (см. {@link #NAVIGATOR_URL_FRAGMENT}), а не по тексту
 *       рядом с ней — так устойчивее к изменениям вёрстки/оформления.</li>
 * </ul>
 */
public class ProgramDetailParser {

    private static final Logger logger = AppLogger.getLogger();
    private static final Pattern DIGITS = Pattern.compile("(\\d+)");
    private static final String NAVIGATOR_URL_FRAGMENT = "cabinet.ruobr.ru/navigator";
    
    private ProgramDetailParser() {
        // Утилитарный класс
    }
    
    public static ParseResult parse(String html) {
        if (html == null || html.isEmpty()) {
            return ParseResult.builder().success(false).errorMessage("HTML пуст или null").build();
        }
        
        try {
            Document document = Jsoup.parse(html);
            
            Map<String, Integer> stats = parseStats(document);
            Integer activeGroupsCount = parseActiveGroupsCount(document);
            List<ProgramGroup> groups = parseGroups(document);
            String navigatorUrl = parseNavigatorUrl(document);

            return ParseResult.builder()
                    .success(true)
                    .stats(stats)
                    .activeGroupsCount(activeGroupsCount)
                    .groups(groups)
                    .navigatorUrl(navigatorUrl)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Ошибка разбора страницы программы: {}", e.getMessage());
            return ParseResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    private static Map<String, Integer> parseStats(Document document) {
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
            stats.merge(label, value, Integer::sum);
        }
        return stats;
    }
    
    /**
     * Ищем заголовок с текстом "Действующих групп: N" по содержимому, а
     * не по конкретному CSS-классу — так устойчивее к переверстке сайта.
     */
    private static Integer parseActiveGroupsCount(Document document) {
        for (Element header : document.select("h1, h2, h3, h4, h5")) {
            String text = header.text();
            if (text != null && text.contains("Действующих групп")) {
                Matcher m = DIGITS.matcher(text);
                if (m.find()) {
                    try {
                        return Integer.parseInt(m.group(1));
                    } catch (NumberFormatException ignored) {
                        // оставляем null
                    }
                }
                break;
            }
        }
        return null;
    }
    
    private static List<ProgramGroup> parseGroups(Document document) {
        List<ProgramGroup> groups = new ArrayList<>();
        Elements cards = document.select(".mcard");
        
        for (Element card : cards) {
            Element content = card.selectFirst(".content");
            if (content == null) {
                continue;
            }
            
            Element dateP = content.selectFirst("p");
            String dateRange = dateP != null ? dateP.text().trim() : null;
            if (dateRange != null && dateRange.isEmpty()) {
                dateRange = null;
            }
            
            Element headerEl = content.selectFirst(".ui.header, .header");
            String name = headerEl != null ? headerEl.text().trim() : "Группа";
            
            Map<String, String> details = new LinkedHashMap<>();
            Elements rows = content.select("table tbody tr");
            for (Element row : rows) {
                Elements cells = row.select("td");
                if (cells.size() >= 2) {
                    String key = cells.get(0).text().trim();
                    String value = cells.get(1).text().trim();
                    if (!key.isEmpty()) {
                        details.put(key, value);
                    }
                }
            }
            
            groups.add(ProgramGroup.builder()
                    .name(name)
                    .dateRange(dateRange)
                    .details(details)
                    .build());
        }
        
        return groups;
    }
    
    /**
     * Ищет ссылку на "навигатор" по самому адресу ({@value #NAVIGATOR_URL_FRAGMENT}),
     * а не по подписи рядом с ней — подпись "ссылка на навигатор:" создаётся
     * CSS-псевдоэлементом ::before и не присутствует в HTML, который видит
     * Jsoup. Берём первую подходящую ссылку на странице.
     */
    private static String parseNavigatorUrl(Document document) {
        for (Element link : document.select("a[href]")) {
            String href = link.attr("href");
            if (href != null && href.contains(NAVIGATOR_URL_FRAGMENT)) {
                return href.trim();
            }
        }
        return null;
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
    
    public static class ParseResult {
        
        private final boolean success;
        private final String errorMessage;
        private final Map<String, Integer> stats;
        private final Integer activeGroupsCount;
        private final List<ProgramGroup> groups;
        private final String navigatorUrl;

        private ParseResult(Builder builder) {
            this.success = builder.success;
            this.errorMessage = builder.errorMessage;
            this.stats = Map.copyOf(builder.stats);
            this.activeGroupsCount = builder.activeGroupsCount;
            this.groups = List.copyOf(builder.groups);
            this.navigatorUrl = builder.navigatorUrl;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public Optional<String> getErrorMessage() {
            return Optional.ofNullable(errorMessage);
        }
        
        public Map<String, Integer> getStats() {
            return stats;
        }
        
        public Optional<Integer> getActiveGroupsCount() {
            return Optional.ofNullable(activeGroupsCount);
        }
        
        public List<ProgramGroup> getGroups() {
            return groups;
        }

        public Optional<String> getNavigatorUrl() {
            return Optional.ofNullable(navigatorUrl);
        }

        public static class Builder {

            private boolean success;
            private String errorMessage;
            private Map<String, Integer> stats = new LinkedHashMap<>();
            private Integer activeGroupsCount;
            private List<ProgramGroup> groups = new ArrayList<>();
            private String navigatorUrl;
            
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

            public Builder navigatorUrl(String navigatorUrl) {
                this.navigatorUrl = navigatorUrl;
                return this;
            }

            public ParseResult build() {
                return new ParseResult(this);
            }
        }
    }
}
