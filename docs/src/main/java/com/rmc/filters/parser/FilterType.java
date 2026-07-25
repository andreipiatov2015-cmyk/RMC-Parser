package com.rmc.filters.parser;

/**
 * Тип фильтра.
 * 
 * <p>Определяет тип элемента формы:</p>
 * <ul>
 *   <li>TEXT - текстовое поле input type="text"</li>
 *   <li>PASSWORD - поле пароля input type="password"</li>
 *   <li>NUMBER - числовое поле input type="number"</li>
 *   <li>CHECKBOX - флажок input type="checkbox"</li>
 *   <li>RADIO - радио кнопка input type="radio"</li>
 *   <li>SELECT - выпадающий список select</li>
 *   <li>SELECT_MULTIPLE - множественный выбор select multiple</li>
 *   <li>TEXTAREA - текстовая область</li>
 *   <li>HIDDEN - скрытое поле input type="hidden"</li>
 *   <li>DATE - поле даты input type="date"</li>
 *   <li>DATETIME - поле даты и времени input type="datetime-local"</li>
 *   <li>EMAIL - поле email input type="email"</li>
 *   <li>URL - поле URL input type="url"</li>
 *   <li>TEL - поле телефона input type="tel"</li>
 *   <li>RANGE - ползунок input type="range"</li>
 *   <li>COLOR - поле цвета input type="color"</li>
 *   <li>FILE - поле файла input type="file"</li>
 *   <li>UNKNOWN - неизвестный тип</li>
 * </ul>
 */
public enum FilterType {
    
    TEXT("text", "Текстовое поле"),
    PASSWORD("password", "Поле пароля"),
    NUMBER("number", "Числовое поле"),
    CHECKBOX("checkbox", "Флажок"),
    RADIO("radio", "Радио кнопка"),
    SELECT("select", "Выпадающий список"),
    SELECT_MULTIPLE("select-multiple", "Список с множественным выбором"),
    TEXTAREA("textarea", "Текстовая область"),
    HIDDEN("hidden", "Скрытое поле"),
    DATE("date", "Поле даты"),
    DATETIME("datetime-local", "Поле даты и времени"),
    EMAIL("email", "Поле email"),
    URL("url", "Поле URL"),
    TEL("tel", "Поле телефона"),
    RANGE("range", "Ползунок"),
    COLOR("color", "Поле цвета"),
    FILE("file", "Поле файла"),
    UNKNOWN("unknown", "Неизвестный тип");
    
    private final String htmlType;
    private final String displayName;
    
    FilterType(String htmlType, String displayName) {
        this.htmlType = htmlType;
        this.displayName = displayName;
    }
    
    /**
     * Получить тип по атрибуту type HTML элемента.
     */
    public static FilterType fromHtmlType(String htmlType) {
        if (htmlType == null || htmlType.isEmpty()) {
            return TEXT; // По умолчанию text
        }
        
        for (FilterType type : values()) {
            if (type.htmlType.equalsIgnoreCase(htmlType)) {
                return type;
            }
        }
        
        return UNKNOWN;
    }
    
    /**
     * @return HTML тип элемента
     */
    public String getHtmlType() {
        return htmlType;
    }
    
    /**
     * @return Отображаемое имя
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * @return true если тип имеет текстовое значение
     */
    public boolean isTextBased() {
        return this == TEXT || this == PASSWORD || this == EMAIL || 
               this == URL || this == TEL || this == TEXTAREA;
    }
    
    /**
     * @return true если тип выбора (select, radio, checkbox)
     */
    public boolean isSelectionBased() {
        return this == SELECT || this == SELECT_MULTIPLE || 
               this == CHECKBOX || this == RADIO;
    }
    
    /**
     * @return true если это select элемент
     */
    public boolean isSelect() {
        return this == SELECT || this == SELECT_MULTIPLE;
    }
    
    /**
     * @return true если множественный выбор
     */
    public boolean isMultiple() {
        return this == SELECT_MULTIPLE;
    }
}
