package com.rmc.ui.dynamic;

import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.parser.FilterOption;
import com.rmc.filters.parser.FilterType;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.HashMap;
import java.util.Map;

/**
 * Фабрика для создания JavaFX контролов из FilterDefinition.
 * 
 * <p>Автоматически создаёт:</p>
 * <ul>
 *   <li>select → ComboBox или ListView</li>
 *   <li>checkbox → CheckBox</li>
 *   <li>radio → ToggleGroup с RadioButtons</li>
 *   <li>text → TextField</li>
 *   <li>textarea → TextArea</li>
 *   <li>number → Spinner или TextField</li>
 *   <li>date → DatePicker</li>
 *   <li>hidden → (пропускается)</li>
 * </ul>
 */
public class FilterControlFactory {
    
    private FilterControlFactory() {
        // Утилитарный класс
    }
    
    /**
     * Создать контрол для фильтра.
     * 
     * @param filter определение фильтра
     * @param grid сетка для добавления метки
     * @param rowIndex текущий индекс строки
     * @return пара (control, rowIncrement)
     */
    public static ControlResult createControl(FilterDefinition filter, GridPane grid, int rowIndex) {
        String caption = filter.getCaption();
        FilterType type = filter.getType();
        
        Label label = new Label(caption != null ? caption : filter.getName());
        label.setPadding(new Insets(0, 10, 0, 0));
        GridPane.setHalignment(label, HPos.RIGHT);
        
        Node control = createControlByType(filter);
        if (control == null) {
            return null; // hidden или неизвестный тип
        }
        
        // Добавляем в grid
        grid.add(label, 0, rowIndex);
        grid.add(control, 1, rowIndex);
        
        // Устанавливаем constraints
        GridPane.setHgrow(control, Priority.ALWAYS);
        GridPane.setMargin(control, new Insets(5, 0, 5, 0));
        
        int rowIncrement = 1;
        
        // Для multiple select используем ListView - добавляем extra строку
        if (type == FilterType.SELECT_MULTIPLE && filter.hasOptions()) {
            rowIncrement = 2;
        }
        
        return new ControlResult(control, rowIncrement);
    }
    
    /**
     * Создать контрол по типу фильтра.
     */
    private static Node createControlByType(FilterDefinition filter) {
        FilterType type = filter.getType();
        
        if (type == null) {
            return createTextField(filter);
        }
        
        return switch (type) {
            case TEXT, EMAIL, URL, TEL -> createTextField(filter);
            case PASSWORD -> createPasswordField(filter);
            case NUMBER -> createNumberField(filter);
            case CHECKBOX -> createCheckbox(filter);
            case RADIO -> createRadioGroup(filter);
            case SELECT -> createComboBox(filter);
            case SELECT_MULTIPLE -> createListView(filter);
            case TEXTAREA -> createTextArea(filter);
            case DATE, DATETIME -> createDatePicker(filter);
            case RANGE -> createSlider(filter);
            case COLOR -> createColorPicker(filter);
            case FILE -> createFileChooser(filter);
            case HIDDEN -> null; // Hidden поля не показываем
            case UNKNOWN -> createTextField(filter);
        };
    }
    
    /**
     * Создать TextField.
     */
    private static TextField createTextField(FilterDefinition filter) {
        TextField textField = new TextField();
        textField.setPromptText(filter.getPlaceholder());
        
        if (filter.getValue() != null) {
            textField.setText(filter.getValue());
        }
        
        textField.setDisable(filter.isDisabled());
        
        // Сохраняем имя фильтра в userData
        textField.setUserData(filter.getName());
        
        return textField;
    }
    
    /**
     * Создать PasswordField.
     */
    private static PasswordField createPasswordField(FilterDefinition filter) {
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText(filter.getPlaceholder());
        
        if (filter.getValue() != null) {
            passwordField.setText(filter.getValue());
        }
        
        passwordField.setDisable(filter.isDisabled());
        passwordField.setUserData(filter.getName());
        
        return passwordField;
    }
    
    /**
     * Создать числовое поле.
     */
    private static TextField createNumberField(FilterDefinition filter) {
        TextField numberField = new TextField();
        numberField.setPromptText(filter.getPlaceholder());
        
        if (filter.getValue() != null) {
            numberField.setText(filter.getValue());
        }
        
        // Устанавливаем фильтр для ввода только чисел
        numberField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty() && !newValue.matches("\\d*")) {
                numberField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        
        numberField.setDisable(filter.isDisabled());
        numberField.setUserData(filter.getName());
        
        return numberField;
    }
    
    /**
     * Создать CheckBox.
     */
    private static CheckBox createCheckbox(FilterDefinition filter) {
        CheckBox checkBox = new CheckBox();
        
        if (filter.getValue() != null) {
            checkBox.setSelected("true".equalsIgnoreCase(filter.getValue()) 
                    || "1".equals(filter.getValue()) 
                    || "yes".equalsIgnoreCase(filter.getValue()));
        }
        
        checkBox.setDisable(filter.isDisabled());
        checkBox.setUserData(filter.getName());
        
        return checkBox;
    }
    
    /**
     * Создать группу RadioButtons.
     */
    private static Node createRadioGroup(FilterDefinition filter) {
        VBox vBox = new VBox(5);
        
        ToggleGroup toggleGroup = new ToggleGroup();
        
        for (FilterOption option : filter.getOptions()) {
            RadioButton radioButton = new RadioButton(option.getLabel());
            radioButton.setToggleGroup(toggleGroup);
            radioButton.setDisable(filter.isDisabled() || option.isDisabled());
            
            if (option.isSelected()) {
                radioButton.setSelected(true);
            }
            
            // Сохраняем имя фильтра и значение опции
            radioButton.setUserData(new String[]{filter.getName(), option.getValue()});
            
            vBox.getChildren().add(radioButton);
        }
        
        return vBox;
    }
    
    /**
     * Создать ComboBox.
     */
    private static ComboBox<String> createComboBox(FilterDefinition filter) {
        ComboBox<String> comboBox = new ComboBox<>();
        
        if (filter.hasOptions()) {
            for (FilterOption option : filter.getOptions()) {
                comboBox.getItems().add(option.getLabel());
                
                if (option.isSelected()) {
                    comboBox.getSelectionModel().select(option.getLabel());
                }
            }
        }
        
        comboBox.setPromptText(filter.getPlaceholder());
        comboBox.setDisable(filter.isDisabled());
        comboBox.setUserData(filter.getName());
        
        // Если есть значение по умолчанию
        if (filter.getValue() != null && !filter.hasOptions()) {
            comboBox.getItems().add(filter.getValue());
            comboBox.getSelectionModel().select(filter.getValue());
        }
        
        return comboBox;
    }
    
    /**
     * Создать ListView для множественного выбора.
     */
    private static ListView<String> createListView(FilterDefinition filter) {
        ListView<String> listView = new ListView<>();
        
        Map<String, String> labelToValue = new HashMap<>();
        
        if (filter.hasOptions()) {
            for (FilterOption option : filter.getOptions()) {
                listView.getItems().add(option.getLabel());
                labelToValue.put(option.getLabel(), option.getValue());
                
                if (option.isSelected()) {
                    listView.getSelectionModel().select(option.getLabel());
                }
            }
        }
        
        listView.setUserData(new Object[]{filter.getName(), labelToValue});
        listView.setDisable(filter.isDisabled());
        listView.setPrefHeight(Math.min(150, filter.getOptions().size() * 24 + 20));
        
        // Разрешаем множественный выбор
        listView.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        
        return listView;
    }
    
    /**
     * Создать TextArea.
     */
    private static TextArea createTextArea(FilterDefinition filter) {
        TextArea textArea = new TextArea();
        textArea.setPromptText(filter.getPlaceholder());
        
        if (filter.getValue() != null) {
            textArea.setText(filter.getValue());
        }
        
        textArea.setDisable(filter.isDisabled());
        textArea.setUserData(filter.getName());
        textArea.setPrefRowCount(3);
        
        return textArea;
    }
    
    /**
     * Создать DatePicker.
     */
    private static DatePicker createDatePicker(FilterDefinition filter) {
        DatePicker datePicker = new DatePicker();
        
        if (filter.getValue() != null) {
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(filter.getValue());
                datePicker.setValue(date);
            } catch (Exception e) {
                // Игнорируем ошибку парсинга даты
            }
        }
        
        datePicker.setDisable(filter.isDisabled());
        datePicker.setUserData(filter.getName());
        
        return datePicker;
    }
    
    /**
     * Создать Slider.
     */
    private static Slider createSlider(FilterDefinition filter) {
        Slider slider = new Slider();
        
        // TODO: получить min/max из атрибутов
        slider.setMin(0);
        slider.setMax(100);
        slider.setValue(50);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(25);
        
        if (filter.getValue() != null) {
            try {
                slider.setValue(Double.parseDouble(filter.getValue()));
            } catch (NumberFormatException e) {
                // Игнорируем
            }
        }
        
        slider.setDisable(filter.isDisabled());
        slider.setUserData(filter.getName());
        
        return slider;
    }
    
    /**
     * Создать ColorPicker.
     */
    private static ColorPicker createColorPicker(FilterDefinition filter) {
        ColorPicker colorPicker = new ColorPicker();
        
        if (filter.getValue() != null) {
            try {
                javafx.scene.paint.Color color = javafx.scene.paint.Color.web(filter.getValue());
                colorPicker.setValue(color);
            } catch (Exception e) {
                // Игнорируем
            }
        }
        
        colorPicker.setDisable(filter.isDisabled());
        colorPicker.setUserData(filter.getName());
        
        return colorPicker;
    }
    
    /**
     * Создать FileChooser (кнопка для выбора файла).
     */
    private static Button createFileChooser(FilterDefinition filter) {
        Button button = new Button("Выбрать файл...");
        button.setUserData(filter.getName());
        button.setDisable(filter.isDisabled());
        
        button.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Выберите файл");
            
            javafx.stage.FileChooser.ExtensionFilter extFilter = 
                    new javafx.stage.FileChooser.ExtensionFilter("Все файлы", "*.*");
            fileChooser.getExtensionFilters().add(extFilter);
            
            java.io.File selectedFile = fileChooser.showOpenDialog(button.getScene().getWindow());
            if (selectedFile != null) {
                // Создаём текстовое поле для отображения пути
                TextField textField = (TextField) button.getScene().lookup("#filePath_" + filter.getName());
                if (textField != null) {
                    textField.setText(selectedFile.getAbsolutePath());
                }
            }
        });
        
        return button;
    }
    
    /**
     * Результат создания контрола.
     */
    public static class ControlResult {
        private final Node control;
        private final int rowIncrement;
        
        public ControlResult(Node control, int rowIncrement) {
            this.control = control;
            this.rowIncrement = rowIncrement;
        }
        
        public Node getControl() {
            return control;
        }
        
        public int getRowIncrement() {
            return rowIncrement;
        }
    }
}
