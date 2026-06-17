package com.rmc.ui.dynamic;

import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.parser.FilterParser;
import com.rmc.logging.AppLogger;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Динамическая панель фильтров.
 * 
 * <p>Автоматически строит JavaFX интерфейс из списка FilterDefinition.</p>
 * 
 * <p>Поддерживаемые типы:</p>
 * <ul>
 *   <li>select → ComboBox или ListView</li>
 *   <li>checkbox → CheckBox</li>
 *   <li>radio → ToggleGroup с RadioButtons</li>
 *   <li>text → TextField</li>
 *   <li>textarea → TextArea</li>
 *   <li>number → TextField</li>
 *   <li>date → DatePicker</li>
 * </ul>
 * 
 * <p>Если завтра сайт добавит новый фильтр - он автоматически появится в программе.</p>
 */
public class DynamicFilterPane extends VBox {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private final FilterBinder binder;
    private final GridPane gridPane;
    private final Label titleLabel;
    private final Label statusLabel;
    
    private List<FilterDefinition> currentFilters;
    
    /**
     * Создать динамическую панель фильтров.
     */
    public DynamicFilterPane() {
        this.binder = new FilterBinder();
        this.gridPane = createGridPane();
        this.titleLabel = new Label("Динамические фильтры");
        this.statusLabel = new Label();
        
        setupPane();
    }
    
    /**
     * Настроить панель.
     */
    private void setupPane() {
        setSpacing(15);
        setPadding(new Insets(20));
        setAlignment(Pos.TOP_CENTER);
        
        // Заголовок
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Статус
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        
        // ScrollPane для фильтров
        ScrollPane scrollPane = new ScrollPane(gridPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        getChildren().addAll(titleLabel, new Separator(), scrollPane, statusLabel);
    }
    
    /**
     * Создать GridPane для фильтров.
     */
    private GridPane createGridPane() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));
        grid.setAlignment(Pos.TOP_LEFT);
        return grid;
    }
    
    /**
     * Загрузить фильтры из HTML.
     * 
     * @param html HTML контент
     */
    public void loadFromHtml(String html) {
        logger.info(LOG_LOADING_FILTERS);
        
        FilterParser.ParseResult result = FilterParser.parse(html);
        
        if (result.isSuccess()) {
            loadFilters(result.getFilters());
        } else {
            logger.error(LOG_PARSE_ERROR, result.getErrorMessage().orElse("Unknown error"));
            statusLabel.setText("Ошибка разбора: " + result.getErrorMessage().orElse(""));
        }
    }
    
    /**
     * Загрузить фильтры из списка определений.
     * 
     * @param filters список фильтров
     */
    public void loadFilters(List<FilterDefinition> filters) {
        logger.info(LOG_BUILDING_UI, filters.size());
        
        // Очищаем предыдущие фильтры
        gridPane.getChildren().clear();
        binder.getControlCount(); // Reset binder if needed
        
        this.currentFilters = filters;
        
        int rowIndex = 0;
        int controlCount = 0;
        
        for (FilterDefinition filter : filters) {
            // Пропускаем hidden поля
            if (filter.getType() != null && filter.getType().name().equals("HIDDEN")) {
                continue;
            }
            
            try {
                FilterControlFactory.ControlResult result = 
                        FilterControlFactory.createControl(filter, gridPane, rowIndex);
                
                if (result != null) {
                    // Регистрируем контрол в binder
                    String filterName = filter.getName();
                    Node control = result.getControl();
                    
                    if (filterName != null && control != null) {
                        binder.register(filterName, control);
                        controlCount++;
                    }
                    
                    rowIndex += result.getRowIncrement();
                }
            } catch (Exception e) {
                logger.warn(LOG_CONTROL_ERROR, filter.getName(), e.getMessage());
            }
        }
        
        logger.info(LOG_UI_BUILT, controlCount);
        statusLabel.setText(String.format("Загружено %d фильтров", controlCount));
    }
    
    /**
     * Получить значение фильтра.
     * 
     * @param filterName имя фильтра
     * @return значение или empty
     */
    public Optional<String> getValue(String filterName) {
        return Optional.ofNullable(binder.getValue(filterName));
    }
    
    /**
     * Установить значение фильтра.
     * 
     * @param filterName имя фильтра
     * @param value значение
     */
    public void setValue(String filterName, String value) {
        binder.setValue(filterName, value);
    }
    
    /**
     * Получить все значения фильтров.
     * 
     * @return Map имя → значение
     */
    public Map<String, String> getAllValues() {
        return binder.getAllValues();
    }
    
    /**
     * Получить значения в формате Query String.
     * 
     * @return строка вида "name1=value1&name2=value2"
     */
    public String toQueryString() {
        return binder.toQueryString();
    }
    
    /**
     * Получить Binder для работы с фильтрами.
     */
    public FilterBinder getBinder() {
        return binder;
    }
    
    /**
     * Получить текущие фильтры.
     */
    public List<FilterDefinition> getCurrentFilters() {
        return currentFilters;
    }
    
    /**
     * Получить количество загруженных фильтров.
     */
    public int getFilterCount() {
        return currentFilters != null ? currentFilters.size() : 0;
    }
    
    /**
     * Проверить, загружены ли фильтры.
     */
    public boolean hasFilters() {
        return currentFilters != null && !currentFilters.isEmpty();
    }
    
    /**
     * Очистить все значения фильтров.
     */
    public void clearValues() {
        binder.clear();
        statusLabel.setText("Значения очищены");
    }
    
    // Константы для логирования
    private static final String LOG_LOADING_FILTERS = "Загрузка фильтров из HTML...";
    private static final String LOG_BUILDING_UI = "Построение UI для {} фильтров...";
    private static final String LOG_UI_BUILT = "UI построен. Создано контролов: {}";
    private static final String LOG_PARSE_ERROR = "Ошибка разбора HTML: {}";
    private static final String LOG_CONTROL_ERROR = "Ошибка создания контрола для {}: {}";
}
