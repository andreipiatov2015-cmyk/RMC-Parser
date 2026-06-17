package com.rmc.ui.dynamic;

import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.parser.FilterOption;
import com.rmc.filters.parser.FilterType;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.*;

/**
 * Связыватель контролов с значениями фильтров.
 * 
 * <p>Позволяет:</p>
 * <ul>
 *   <li>Получить значения из контролов</li>
 *   <li>Установить значения в контролы</li>
 *   <li>Преобразовать значения в формат для отправки формы</li>
 * </ul>
 */
public class FilterBinder {
    
    private final Map<String, Node> controlMap;
    
    public FilterBinder() {
        this.controlMap = new HashMap<>();
    }
    
    /**
     * Зарегистрировать контрол с именем фильтра.
     * 
     * @param filterName имя фильтра
     * @param control JavaFX контрол
     */
    public void register(String filterName, Node control) {
        controlMap.put(filterName, control);
    }
    
    /**
     * Получить значение контрола по имени фильтра.
     * 
     * @param filterName имя фильтра
     * @return значение или null
     */
    public String getValue(String filterName) {
        Node control = controlMap.get(filterName);
        if (control == null) {
            return null;
        }
        
        return getValueFromControl(control);
    }
    
    /**
     * Получить значение из контрола.
     */
    private String getValueFromControl(Node control) {
        if (control instanceof TextInputControl textInput) {
            return textInput.getText();
        }
        
        if (control instanceof CheckBox checkBox) {
            return checkBox.isSelected() ? "true" : "false";
        }
        
        if (control instanceof ComboBox<?> comboBox) {
            Object selected = comboBox.getSelectionModel().getSelectedItem();
            if (selected != null) {
                return selected.toString();
            }
            return null;
        }
        
        if (control instanceof ListView<?> listView) {
            // Для ListView возвращаем значения выбранных элементов через запятую
            var selectedItems = listView.getSelectionModel().getSelectedItems();
            if (selectedItems.isEmpty()) {
                return null;
            }
            
            Object userData = listView.getUserData();
            if (userData instanceof Object[] arr && arr[1] instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> labelToValue = (Map<String, String>) arr[1];
                List<String> values = new ArrayList<>();
                for (Object item : selectedItems) {
                    String label = item.toString();
                    String value = labelToValue.get(label);
                    values.add(value != null ? value : label);
                }
                return String.join(",", values);
            }
            
            return String.join(",", selectedItems.stream().map(Object::toString).toList());
        }
        
        if (control instanceof VBox vBox) {
            // Radio buttons
            ToggleGroup toggleGroup = null;
            for (Node node : vBox.getChildren()) {
                if (node instanceof RadioButton radio) {
                    toggleGroup = radio.getToggleGroup();
                    if (toggleGroup != null && toggleGroup.getSelectedToggle() == radio) {
                        Object userData = radio.getUserData();
                        if (userData instanceof String[] arr) {
                            return arr[1]; // Возвращаем value
                        }
                        return radio.getText();
                    }
                }
            }
        }
        
        if (control instanceof DatePicker datePicker) {
            LocalDate date = datePicker.getValue();
            if (date != null) {
                return date.toString();
            }
            return null;
        }
        
        if (control instanceof Slider slider) {
            return String.valueOf((int) slider.getValue());
        }
        
        if (control instanceof ColorPicker colorPicker) {
            javafx.scene.paint.Color color = colorPicker.getValue();
            if (color != null) {
                return String.format("#%02X%02X%02X", 
                        (int) (color.getRed() * 255),
                        (int) (color.getGreen() * 255),
                        (int) (color.getBlue() * 255));
            }
            return null;
        }
        
        if (control instanceof Button button) {
            // File chooser - ищем связанное текстовое поле
            String filterName = (String) button.getUserData();
            TextField textField = (TextField) button.getScene().lookup("#filePath_" + filterName);
            if (textField != null) {
                return textField.getText();
            }
        }
        
        return null;
    }
    
    /**
     * Установить значение контрола.
     * 
     * @param filterName имя фильтра
     * @param value значение
     */
    public void setValue(String filterName, String value) {
        Node control = controlMap.get(filterName);
        if (control == null) {
            return;
        }
        
        setValueToControl(control, value);
    }
    
    /**
     * Установить значение в контрол.
     */
    private void setValueToControl(Node control, String value) {
        if (control instanceof TextInputControl textInput) {
            textInput.setText(value);
            return;
        }
        
        if (control instanceof CheckBox checkBox) {
            checkBox.setSelected("true".equalsIgnoreCase(value) 
                    || "1".equals(value) 
                    || "yes".equalsIgnoreCase(value));
            return;
        }
        
        if (control instanceof ComboBox<?> comboBox) {
            if (value != null) {
                // Try to find matching item by string
                for (int i = 0; i < comboBox.getItems().size(); i++) {
                    Object item = comboBox.getItems().get(i);
                    if (value.equals(item.toString())) {
                        comboBox.getSelectionModel().select(i);
                        break;
                    }
                }
            }
            return;
        }
        
        if (control instanceof DatePicker datePicker) {
            if (value != null) {
                try {
                    LocalDate date = LocalDate.parse(value);
                    datePicker.setValue(date);
                } catch (Exception ignored) {}
            }
            return;
        }
        
        if (control instanceof Slider slider) {
            if (value != null) {
                try {
                    slider.setValue(Double.parseDouble(value));
                } catch (NumberFormatException ignored) {}
            }
            return;
        }
        
        if (control instanceof ColorPicker colorPicker) {
            if (value != null) {
                try {
                    javafx.scene.paint.Color color = javafx.scene.paint.Color.web(value);
                    colorPicker.setValue(color);
                } catch (Exception ignored) {}
            }
            return;
        }
    }
    
    /**
     * Получить все значения в виде Map.
     * 
     * @return Map имя → значение
     */
    public Map<String, String> getAllValues() {
        Map<String, String> values = new HashMap<>();
        for (Map.Entry<String, Node> entry : controlMap.entrySet()) {
            String value = getValueFromControl(entry.getValue());
            if (value != null && !value.isEmpty()) {
                values.put(entry.getKey(), value);
            }
        }
        return values;
    }
    
    /**
     * Получить все значения для отправки формы (Query String format).
     * 
     * @return строка вида "name1=value1&name2=value2"
     */
    public String toQueryString() {
        Map<String, String> values = getAllValues();
        if (values.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(urlEncode(entry.getKey()));
            sb.append("=");
            sb.append(urlEncode(entry.getValue()));
        }
        return sb.toString();
    }
    
    /**
     * Проверить, зарегистрирован ли контрол.
     */
    public boolean hasControl(String filterName) {
        return controlMap.containsKey(filterName);
    }
    
    /**
     * Получить контрол по имени.
     */
    public Node getControl(String filterName) {
        return controlMap.get(filterName);
    }
    
    /**
     * Получить количество зарегистрированных контролов.
     */
    public int getControlCount() {
        return controlMap.size();
    }
    
    /**
     * Очистить все значения.
     */
    public void clear() {
        for (Node control : controlMap.values()) {
            if (control instanceof TextInputControl textInput) {
                textInput.clear();
            } else if (control instanceof CheckBox checkBox) {
                checkBox.setSelected(false);
            } else if (control instanceof ComboBox<?> comboBox) {
                comboBox.getSelectionModel().clearSelection();
            } else if (control instanceof ListView<?> listView) {
                listView.getSelectionModel().clearSelection();
            } else if (control instanceof DatePicker datePicker) {
                datePicker.setValue(null);
            } else if (control instanceof Slider slider) {
                slider.setValue(slider.getMin());
            }
        }
    }
    
    /**
     * URL encode строки.
     */
    private String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }
}
