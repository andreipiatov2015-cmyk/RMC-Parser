package com.rmc.filters.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Определение фильтра.
 * 
 * <p>Содержит всю информацию о фильтре:</p>
 * <ul>
 *   <li>name - имя поля</li>
 *   <li>caption - отображаемое название</li>
 *   <li>type - тип элемента</li>
 *   <li>value - текущее значение</li>
 *   <li>options - варианты выбора (для select, radio, checkbox)</li>
 *   <li>placeholder - подсказка</li>
 *   <li>required - обязательность</li>
 *   <li>multiple - множественный выбор</li>
 *   <li>disabled - отключён</li>
 * </ul>
 */
public class FilterDefinition {
    
    private final String name;
    private final String caption;
    private final FilterType type;
    private final String value;
    private final List<FilterOption> options;
    private final String placeholder;
    private final boolean required;
    private final boolean multiple;
    private final boolean disabled;
    private final String id;
    
    private FilterDefinition(Builder builder) {
        this.name = builder.name;
        this.caption = builder.caption;
        this.type = builder.type;
        this.value = builder.value;
        this.options = List.copyOf(builder.options);
        this.placeholder = builder.placeholder;
        this.required = builder.required;
        this.multiple = builder.multiple;
        this.disabled = builder.disabled;
        this.id = builder.id;
    }
    
    /**
     * Создать Builder для построения FilterDefinition.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * @return Имя поля
     */
    public String getName() {
        return name;
    }
    
    /**
     * @return Отображаемое название
     */
    public String getCaption() {
        return caption;
    }
    
    /**
     * @return Тип фильтра
     */
    public FilterType getType() {
        return type;
    }
    
    /**
     * @return Текущее значение
     */
    public String getValue() {
        return value;
    }
    
    /**
     * @return Список вариантов выбора
     */
    public List<FilterOption> getOptions() {
        return options;
    }
    
    /**
     * @return Подсказка
     */
    public String getPlaceholder() {
        return placeholder;
    }
    
    /**
     * @return true если поле обязательно
     */
    public boolean isRequired() {
        return required;
    }
    
    /**
     * @return true если множественный выбор
     */
    public boolean isMultiple() {
        return multiple;
    }
    
    /**
     * @return true если поле отключено
     */
    public boolean isDisabled() {
        return disabled;
    }
    
    /**
     * @return ID элемента
     */
    public String getId() {
        return id;
    }
    
    /**
     * @return Имя или empty
     */
    public Optional<String> getNameOpt() {
        return Optional.ofNullable(name);
    }
    
    /**
     * @return true если есть варианты выбора
     */
    public boolean hasOptions() {
        return !options.isEmpty();
    }
    
    /**
     * @return Количество вариантов выбора
     */
    public int getOptionCount() {
        return options.size();
    }
    
    /**
     * @return true если это select элемент
     */
    public boolean isSelect() {
        return type != null && type.isSelect();
    }
    
    @Override
    public String toString() {
        return String.format("FilterDefinition{name=%s, caption=%s, type=%s, options=%d}", 
                name, caption, type, options.size());
    }
    
    /**
     * Builder для создания FilterDefinition.
     */
    public static class Builder {
        
        private String name;
        private String caption;
        private FilterType type = FilterType.TEXT;
        private String value;
        private List<FilterOption> options = new ArrayList<>();
        private String placeholder;
        private boolean required;
        private boolean multiple;
        private boolean disabled;
        private String id;
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder caption(String caption) {
            this.caption = caption;
            return this;
        }
        
        public Builder type(FilterType type) {
            this.type = type;
            return this;
        }
        
        public Builder value(String value) {
            this.value = value;
            return this;
        }
        
        public Builder options(List<FilterOption> options) {
            this.options = new ArrayList<>(options);
            return this;
        }
        
        public Builder addOption(FilterOption option) {
            this.options.add(option);
            return this;
        }
        
        public Builder placeholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }
        
        public Builder required(boolean required) {
            this.required = required;
            return this;
        }
        
        public Builder multiple(boolean multiple) {
            this.multiple = multiple;
            return this;
        }
        
        public Builder disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public FilterDefinition build() {
            return new FilterDefinition(this);
        }
    }
}
