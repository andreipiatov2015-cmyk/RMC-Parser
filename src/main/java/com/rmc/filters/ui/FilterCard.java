package com.rmc.filters.ui;

import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.parser.FilterOption;
import com.rmc.filters.parser.FilterType;
import com.rmc.filters.session.FilterSession;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Card component for a single filter.
 * 
 * <p>Automatically creates the appropriate control based on FilterType.</p>
 */
public class FilterCard extends VBox {
    
    private final FilterDefinition definition;
    private final FilterSession session;
    private final Consumer<FilterSession> onChange;
    
    private Label titleLabel;
    private Node control;
    private boolean isUpdating = false;
    
    public FilterCard(FilterDefinition definition, FilterSession session, Consumer<FilterSession> onChange) {
        this.definition = definition;
        this.session = session;
        this.onChange = onChange;
        
        setupCard();
    }
    
    private void setupCard() {
        getStyleClass().add("filter-card");
        setSpacing(4);
        setPadding(new Insets(10));
        setAlignment(Pos.TOP_LEFT);
        setPrefHeight(70);
        setMaxHeight(70);
        
        // Title
        String title = definition.getCaption() != null ? definition.getCaption() : definition.getName();
        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("filter-card-title");
        
        // Control
        control = createControl();
        
        getChildren().addAll(titleLabel, control);
        
        // Load initial value
        loadInitialValue();
    }
    
    private Node createControl() {
        FilterType type = definition.getType();
        
        if (type == null) {
            type = FilterType.TEXT;
        }
        
        return switch (type) {
            case SELECT -> createComboBox();
            case SELECT_MULTIPLE -> createMultiSelect();
            case CHECKBOX -> createCheckbox();
            case RADIO -> createRadioGroup();
            case DATE, DATETIME -> createDatePicker();
            case TEXT, EMAIL, URL, TEL, TEXTAREA, NUMBER, PASSWORD -> createTextField();
            case HIDDEN -> createHiddenField();
            default -> createTextField();
        };
    }
    
    private Node createTextField() {
        TextField textField = new TextField();
        textField.setPromptText(definition.getPlaceholder());
        textField.getStyleClass().add("filter-text-field");
        
        textField.textProperty().addListener((obs, old, val) -> {
            if (!isUpdating) {
                session.setValue(definition.getName(), val);
                notifyChange();
            }
        });
        
        return textField;
    }
    
    private Node createComboBox() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getStyleClass().add("filter-combo-box");
        comboBox.setPromptText("Выберите...");
        
        // Add options
        if (definition.hasOptions()) {
            for (FilterOption option : definition.getOptions()) {
                if (!option.isDisabled()) {
                    comboBox.getItems().add(option.getLabel());
                }
            }
        }
        
        comboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
            }
        });
        
        comboBox.valueProperty().addListener((obs, old, val) -> {
            if (!isUpdating && val != null) {
                String actualValue = findValueByLabel(val);
                session.setValue(definition.getName(), actualValue);
                notifyChange();
            }
        });
        
        return comboBox;
    }
    
    private String findValueByLabel(String label) {
        if (definition.hasOptions()) {
            for (FilterOption option : definition.getOptions()) {
                if (label.equals(option.getLabel())) {
                    return option.getValue();
                }
            }
        }
        return label;
    }
    
    private String findLabelByValue(String value) {
        if (value == null) return null;
        if (definition.hasOptions()) {
            for (FilterOption option : definition.getOptions()) {
                if (value.equals(option.getValue())) {
                    return option.getLabel();
                }
            }
        }
        return value;
    }
    
    private Node createMultiSelect() {
        VBox container = new VBox();
        container.setSpacing(8);
        
        // FlowPane for chips
        FlowPane chipsPane = new FlowPane();
        chipsPane.getStyleClass().add("filter-chips-pane");
        chipsPane.setHgap(6);
        chipsPane.setVgap(6);
        
        // HBox for dropdown
        HBox selectRow = new HBox();
        selectRow.setSpacing(8);
        selectRow.setAlignment(Pos.CENTER_LEFT);
        
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getStyleClass().add("filter-combo-box");
        comboBox.setPromptText("Добавить...");
        comboBox.setPrefWidth(250);
        
        if (definition.hasOptions()) {
            for (FilterOption option : definition.getOptions()) {
                if (!option.isDisabled()) {
                    comboBox.getItems().add(option.getLabel());
                }
            }
        }
        
        Button addBtn = new Button("+");
        addBtn.getStyleClass().add("filter-add-button");
        addBtn.setOnAction(e -> {
            String selected = comboBox.getValue();
            if (selected != null) {
                String actualValue = findValueByLabel(selected);
                session.addMultiValue(definition.getName(), actualValue);
                refreshChips(chipsPane);
                comboBox.getItems().remove(selected);
                comboBox.setValue(null);
                notifyChange();
            }
        });
        
        selectRow.getChildren().addAll(comboBox, addBtn);
        
        container.getChildren().addAll(chipsPane, selectRow);
        
        // Store reference to chips pane for refreshing
        container.setUserData(chipsPane);
        
        return container;
    }
    
    private void refreshChips(FlowPane chipsPane) {
        chipsPane.getChildren().clear();
        
        Set<String> values = session.getMultiValues(definition.getName());
        for (String value : values) {
            String label = findLabelByValue(value);
            if (label == null) label = value;
            
            Chip chip = createChip(label, value);
            chipsPane.getChildren().add(chip);
        }
    }
    
    private Chip createChip(String label, String value) {
        Chip chip = new Chip(label);
        chip.getStyleClass().add("filter-chip");
        
        chip.setOnCloseHandler(e -> {
            session.removeMultiValue(definition.getName(), value);
            refreshChips((FlowPane) control);
            notifyChange();
        });
        
        return chip;
    }
    
    private Node createCheckbox() {
        CheckBox checkBox = new CheckBox();
        checkBox.getStyleClass().add("filter-checkbox");
        
        checkBox.selectedProperty().addListener((obs, old, val) -> {
            if (!isUpdating) {
                session.setValue(definition.getName(), val ? "1" : "");
                notifyChange();
            }
        });
        
        return checkBox;
    }
    
    private Node createRadioGroup() {
        VBox radioGroup = new VBox();
        radioGroup.setSpacing(6);
        
        ToggleGroup toggleGroup = new ToggleGroup();
        
        if (definition.hasOptions()) {
            for (FilterOption option : definition.getOptions()) {
                RadioButton radio = new RadioButton(option.getLabel());
                radio.setToggleGroup(toggleGroup);
                radio.setDisable(option.isDisabled());
                radio.getStyleClass().add("filter-radio");
                
                if (option.isSelected()) {
                    radio.setSelected(true);
                }
                
                radio.setUserData(option.getValue());
                
                radio.selectedProperty().addListener((obs, old, val) -> {
                    if (!isUpdating && val) {
                        RadioButton selected = (RadioButton) toggleGroup.getSelectedToggle();
                        if (selected != null) {
                            session.setValue(definition.getName(), (String) selected.getUserData());
                            notifyChange();
                        }
                    }
                });
                
                radioGroup.getChildren().add(radio);
            }
        }
        
        return radioGroup;
    }
    
    private Node createDatePicker() {
        DatePicker datePicker = new DatePicker();
        datePicker.getStyleClass().add("filter-date-picker");
        datePicker.setPromptText("Выберите дату...");
        
        datePicker.valueProperty().addListener((obs, old, val) -> {
            if (!isUpdating) {
                if (val != null) {
                    String formatted = val.format(DateTimeFormatter.ISO_LOCAL_DATE);
                    session.setValue(definition.getName(), formatted);
                } else {
                    session.setValue(definition.getName(), null);
                }
                notifyChange();
            }
        });
        
        return datePicker;
    }
    
    private Node createHiddenField() {
        // Hidden fields don't need UI
        Label hiddenLabel = new Label("(скрытое поле)");
        hiddenLabel.setStyle("-fx-opacity: 0.5; -fx-font-style: italic;");
        return hiddenLabel;
    }
    
    private void loadInitialValue() {
        isUpdating = true;
        
        String currentValue = session.getValue(definition.getName());
        
        if (control instanceof TextField textField) {
            if (currentValue != null) {
                textField.setText(currentValue);
            }
        } else if (control instanceof ComboBox comboBox) {
            if (currentValue != null) {
                String label = findLabelByValue(currentValue);
                if (label != null) {
                    comboBox.setValue(label);
                }
            }
        } else if (control instanceof CheckBox checkBox) {
            checkBox.setSelected("1".equals(currentValue) || "true".equalsIgnoreCase(currentValue));
        } else if (control instanceof DatePicker datePicker) {
            if (currentValue != null && !currentValue.isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(currentValue);
                    datePicker.setValue(date);
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }
        } else if (control instanceof VBox vBox) {
            // Check if this is multi-select
            Object userData = vBox.getUserData();
            if (userData instanceof FlowPane) {
                refreshChips((FlowPane) userData);
            }
        }
        
        isUpdating = false;
    }
    
    private void notifyChange() {
        if (onChange != null) {
            onChange.accept(session);
        }
    }
    
    public FilterDefinition getDefinition() {
        return definition;
    }
}
