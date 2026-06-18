package com.rmc.filters.parser;

import com.rmc.logging.AppLogger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Парсер HTML страницы для извлечения фильтров.
 * 
 * <p>Автоматически находит все элементы форм:</p>
 * <ul>
 *   <li>select - выпадающие списки</li>
 *   <li>input - текстовые поля, чекбоксы, радио кнопки</li>
 *   <li>textarea - текстовые области</li>
 * </ul>
 * 
 * <p>Извлекает:</p>
 * <ul>
 *   <li>name - имя поля</li>
 *   <li>caption - текстовая метка (label)</li>
 *   <li>value - текущее значение</li>
 *   <li>options - варианты выбора</li>
 *   <li>placeholder - подсказка</li>
 *   <li>selected - выбранные элементы</li>
 *   <li>multiple - множественный выбор</li>
 *   <li>disabled - отключён</li>
 *   <li>required - обязательность</li>
 * </ul>
 */
public class FilterParser {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private FilterParser() {
        // Утилитарный класс
    }
    
    /**
     * Разобрать HTML и извлечь все фильтры.
     * 
     * @param html HTML контент
     * @return Результат парсинга
     */
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
            
            // Сначала ищем #modern_filter_form
            Element filterForm = document.selectFirst("#modern_filter_form");
            
            // Если не нашли, ищем любую форму с фильтрами
            if (filterForm == null) {
                filterForm = document.selectFirst("form[class*=filter]");
            }
            
            // Если всё ещё не нашли, используем весь документ
            if (filterForm == null) {
                filterForm = document;
                logger.info("Форма #modern_filter_form не найдена, используется весь документ");
            } else {
                logger.info("Найдена форма фильтров");
            }
            
            List<FilterDefinition> filters = new ArrayList<>();
            Map<String, String> labelMap = buildLabelMap(document);
            
            // Парсим select элементы
            Elements selectElements = filterForm.select("select");
            for (Element select : selectElements) {
                FilterDefinition filter = parseSelectElement(select, labelMap);
                if (filter != null) {
                    filters.add(filter);
                }
            }
            
            // Парсим input элементы
            Elements inputElements = filterForm.select("input");
            for (Element input : inputElements) {
                FilterDefinition filter = parseInputElement(input, labelMap);
                if (filter != null) {
                    filters.add(filter);
                }
            }
            
            // Парсим textarea элементы
            Elements textareaElements = filterForm.select("textarea");
            for (Element textarea : textareaElements) {
                FilterDefinition filter = parseTextareaElement(textarea, labelMap);
                if (filter != null) {
                    filters.add(filter);
                }
            }
            
            logger.info(LOG_PARSING_COMPLETE, filters.size());
            
            return ParseResult.builder()
                    .success(true)
                    .filters(filters)
                    .filterCount(filters.size())
                    .build();
                    
        } catch (Exception e) {
            logger.error(LOG_PARSING_ERROR, e.getMessage());
            return ParseResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    /**
     * Построить карту меток: for -> text
     */
    private static Map<String, String> buildLabelMap(Document document) {
        Map<String, String> labelMap = new HashMap<>();
        
        Elements labels = document.select("label");
        for (Element label : labels) {
            String forAttr = label.attr("for");
            String text = label.text().trim();
            
            if (!forAttr.isEmpty() && !text.isEmpty()) {
                labelMap.put(forAttr, text);
            }
        }
        
        return labelMap;
    }
    
    /**
     * Найти метку для элемента.
     */
    private static String findCaption(Element element, Map<String, String> labelMap) {
        // 1. Проверяем родительский label
        Element parentLabel = element.parent().closest("label");
        if (parentLabel != null) {
            String text = parentLabel.text().trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        
        // 2. Проверяем label по for атрибуту
        String id = element.attr("id");
        if (!id.isEmpty() && labelMap.containsKey(id)) {
            return labelMap.get(id);
        }
        
        // 3. Проверяем предшествующий sibling label
        Element prevLabel = element.previousElementSibling();
        if (prevLabel != null && prevLabel.tagName().equals("label")) {
            String text = prevLabel.text().trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        
        return null;
    }
    
    /**
     * Парсить select элемент.
     */
    private static FilterDefinition parseSelectElement(Element select, Map<String, String> labelMap) {
        String name = select.attr("name").trim();
        
        if (name.isEmpty()) {
            return null;
        }
        
        String caption = findCaption(select, labelMap);
        boolean multiple = select.hasAttr("multiple");
        boolean disabled = select.hasAttr("disabled");
        boolean required = select.hasAttr("required");
        
        List<FilterOption> options = new ArrayList<>();
        Elements optionElements = select.select("option");
        
        for (Element option : optionElements) {
            String value = option.attr("value");
            String label = option.text().trim();
            boolean selected = option.hasAttr("selected");
            boolean optionDisabled = option.hasAttr("disabled");
            
            // Если value пустой, используем текст
            if (value.isEmpty()) {
                value = label;
            }
            
            // Если label пустой, используем value
            if (label.isEmpty()) {
                label = value;
            }
            
            options.add(FilterOption.builder()
                    .value(value)
                    .label(label)
                    .selected(selected)
                    .disabled(optionDisabled)
                    .build());
        }
        
        // Получаем текущее выбранное значение
        String currentValue = null;
        Elements selectedOptions = select.select("option[selected]");
        if (!selectedOptions.isEmpty()) {
            currentValue = selectedOptions.first().attr("value");
            if (currentValue.isEmpty()) {
                currentValue = selectedOptions.first().text().trim();
            }
        }
        
        FilterType type = multiple ? FilterType.SELECT_MULTIPLE : FilterType.SELECT;
        
        return FilterDefinition.builder()
                .name(name)
                .caption(caption)
                .type(type)
                .value(currentValue)
                .options(options)
                .multiple(multiple)
                .disabled(disabled)
                .required(required)
                .id(select.attr("id"))
                .build();
    }
    
    /**
     * Парсить input элемент.
     */
    private static FilterDefinition parseInputElement(Element input, Map<String, String> labelMap) {
        String name = input.attr("name").trim();
        
        if (name.isEmpty()) {
            return null;
        }
        
        String typeAttr = input.attr("type").toLowerCase();
        FilterType type = FilterType.fromHtmlType(typeAttr);
        String caption = findCaption(input, labelMap);
        String value = input.attr("value");
        boolean disabled = input.hasAttr("disabled");
        boolean required = input.hasAttr("required");
        
        // Для checkbox и radio получаем options из группы
        List<FilterOption> options = new ArrayList<>();
        
        if (type == FilterType.CHECKBOX || type == FilterType.RADIO) {
            String groupName = name;
            Elements groupElements = input.ownerDocument().select(
                    "input[name='" + groupName + "']");
            
            for (Element groupInput : groupElements) {
                String optValue = groupInput.attr("value");
                String optLabel = optValue;
                boolean optSelected = groupInput.hasAttr("checked");
                
                // Пытаемся найти метку
                String inputId = groupInput.attr("id");
                if (!inputId.isEmpty() && labelMap.containsKey(inputId)) {
                    optLabel = labelMap.get(inputId);
                } else {
                    Element siblingLabel = groupInput.previousElementSibling();
                    if (siblingLabel != null && siblingLabel.tagName().equals("label")) {
                        optLabel = siblingLabel.text().trim();
                    }
                }
                
                options.add(FilterOption.builder()
                        .value(optValue)
                        .label(optLabel)
                        .selected(optSelected)
                        .build());
            }
        }
        
        return FilterDefinition.builder()
                .name(name)
                .caption(caption)
                .type(type)
                .value(value)
                .options(options)
                .placeholder(input.attr("placeholder"))
                .disabled(disabled)
                .required(required)
                .id(input.attr("id"))
                .build();
    }
    
    /**
     * Парсить textarea элемент.
     */
    private static FilterDefinition parseTextareaElement(Element textarea, Map<String, String> labelMap) {
        String name = textarea.attr("name").trim();
        
        if (name.isEmpty()) {
            return null;
        }
        
        String caption = findCaption(textarea, labelMap);
        String value = textarea.text();
        boolean disabled = textarea.hasAttr("disabled");
        boolean required = textarea.hasAttr("required");
        
        return FilterDefinition.builder()
                .name(name)
                .caption(caption)
                .type(FilterType.TEXTAREA)
                .value(value)
                .placeholder(textarea.attr("placeholder"))
                .disabled(disabled)
                .required(required)
                .id(textarea.attr("id"))
                .build();
    }
    
    // Константы для логирования
    private static final String LOG_PARSING_START = "Разбор HTML страницы...";
    private static final String LOG_PARSING_COMPLETE = "Разбор завершён. Найдено фильтров: {}";
    private static final String LOG_PARSING_ERROR = "Ошибка разбора HTML: {}";
    private static final String LOG_EMPTY_HTML = "HTML пуст";
    
    /**
     * Результат парсинга.
     */
    public static class ParseResult {
        
        private final boolean success;
        private final List<FilterDefinition> filters;
        private final int filterCount;
        private final String errorMessage;
        
        private ParseResult(Builder builder) {
            this.success = builder.success;
            this.filters = List.copyOf(builder.filters);
            this.filterCount = builder.filterCount;
            this.errorMessage = builder.errorMessage;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public List<FilterDefinition> getFilters() {
            return filters;
        }
        
        public int getFilterCount() {
            return filterCount;
        }
        
        public Optional<String> getErrorMessage() {
            return Optional.ofNullable(errorMessage);
        }
        
        public static class Builder {
            
            private boolean success;
            private List<FilterDefinition> filters = new ArrayList<>();
            private int filterCount;
            private String errorMessage;
            
            public Builder success(boolean success) {
                this.success = success;
                return this;
            }
            
            public Builder filters(List<FilterDefinition> filters) {
                this.filters = new ArrayList<>(filters);
                return this;
            }
            
            public Builder filterCount(int count) {
                this.filterCount = count;
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
