package com.rmc.filters.parser;

import java.util.Optional;

/**
 * Опция фильтра (значение для выбора).
 * 
 * <p>Представляет один вариант выбора в фильтре:</p>
 * <ul>
 *   <li>Выпадающий список (select/option)</li>
 *   <li>Радио кнопки (input type="radio")</li>
 *   <li>Флажки (input type="checkbox")</li>
 * </ul>
 */
public class FilterOption {
    
    private final String value;
    private final String label;
    private final boolean selected;
    private final boolean disabled;
    
    private FilterOption(Builder builder) {
        this.value = builder.value;
        this.label = builder.label;
        this.selected = builder.selected;
        this.disabled = builder.disabled;
    }
    
    /**
     * Создать Builder для построения FilterOption.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Создать из HTML option элемента.
     */
    public static FilterOption fromOptionElement(String value, String label, boolean selected, boolean disabled) {
        return builder()
                .value(value)
                .label(label)
                .selected(selected)
                .disabled(disabled)
                .build();
    }
    
    /**
     * @return Значение опции
     */
    public String getValue() {
        return value;
    }
    
    /**
     * @return Текстовая метка опции
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * @return true если опция выбрана
     */
    public boolean isSelected() {
        return selected;
    }
    
    /**
     * @return true если опция отключена
     */
    public boolean isDisabled() {
        return disabled;
    }
    
    /**
     * @return Значение или empty
     */
    public Optional<String> getValueOpt() {
        return Optional.ofNullable(value);
    }
    
    @Override
    public String toString() {
        return String.format("FilterOption{value=%s, label=%s, selected=%s}", value, label, selected);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterOption that = (FilterOption) o;
        return value != null ? value.equals(that.value) : that.value == null;
    }
    
    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
    
    /**
     * Builder для создания FilterOption.
     */
    public static class Builder {
        
        private String value;
        private String label;
        private boolean selected;
        private boolean disabled;
        
        public Builder value(String value) {
            this.value = value;
            return this;
        }
        
        public Builder label(String label) {
            this.label = label;
            return this;
        }
        
        public Builder selected(boolean selected) {
            this.selected = selected;
            return this;
        }
        
        public Builder disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }
        
        public FilterOption build() {
            return new FilterOption(this);
        }
    }
}
