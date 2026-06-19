package com.rmc.ui.workspace.components;

import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.parser.FilterOption;
import com.rmc.filters.parser.FilterType;
import com.rmc.filters.session.FilterSession;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Filter card component - shows a single filter.
 */
public class FilterCard extends VBox {
    
    private final FilterDefinition definition;
    private final FilterSession session;
    private final Consumer<FilterSession> onChange;
    
    private Label titleLabel;
    private Node control;
    
    public FilterCard(FilterDefinition definition, FilterSession session, Consumer<FilterSession> onChange) {
        this.definition = definition;
        this.session = session;
        this.onChange = onChange;
        
        getStyleClass().add("filter-card");
        setSpacing(6);
        setPadding(new Insets(12));
        setAlignment(Pos.TOP_LEFT);
        
        setupCard();
    }
    
    private void setupCard() {
        // Title
        String title = definition.getCaption() != null ? definition.getCaption() : definition.getName();
        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("filter-card-title");
        
        // Control based on type
        control = createControl();
        
        getChildren().addAll(titleLabel, control);
    }
    
    private Node createControl() {
        FilterType type = definition.getType();
        
        if (type == null) {
            return new TextField();
        }
        
        return switch (type) {
            case SELECT, MULTISELECT -> createSelectControl();
            case CHECKBOX -> createCheckboxControl();
            case RADIO -> createRadioControl();
            case TEXT -> createTextControl();
            case NUMBER -> createNumberControl();
            case DATE -> createDateControl();
            case HIDDEN -> new TextField(); // Should not be visible
        };
    }
    
    private Node createSelectControl() {
        ComboBox<String> combo = new ComboBox<>();
        combo.getStyleClass().add("filter-combo");
        
        // Add options
        List<FilterOption> options = definition.getOptions();
        if (options != null) {
            for (FilterOption opt : options) {
                combo.getItems().add(opt.getLabel());
            }
        }
        
        // Set value from session
        String currentValue = session.getSingleValue(definition.getName());
        if (currentValue != null) {
            // Find matching option
            for (FilterOption opt : options) {
                if (opt.getValue().equals(currentValue)) {
                    combo.setValue(opt.getLabel());
                    break;
                }
            }
        }
        
        // Handle selection
        combo.setOnAction(e -> {
            String selectedLabel = combo.getValue();
            if (selectedLabel != null) {
                // Find value
                for (FilterOption opt : options) {
                    if (opt.getLabel().equals(selectedLabel)) {
                        session.setSingleValue(definition.getName(), opt.getValue());
                        break;
                    }
                }
                if (onChange != null) onChange.accept(session);
            }
        });
        
        return combo;
    }
    
    private Node createCheckboxControl() {
        CheckBox checkBox = new CheckBox();
        checkBox.getStyleClass().add("filter-checkbox");
        
        Boolean currentValue = session.getBooleanValue(definition.getName());
        checkBox.setSelected(currentValue != null && currentValue);
        
        checkBox.setOnAction(e -> {
            session.setBooleanValue(definition.getName(), checkBox.isSelected());
            if (onChange != null) onChange.accept(session);
        });
        
        return checkBox;
    }
    
    private Node createRadioControl() {
        ToggleGroup group = new ToggleGroup();
        VBox container = new VBox();
        container.setSpacing(4);
        
        List<FilterOption> options = definition.getOptions();
        String currentValue = session.getSingleValue(definition.getName());
        
        if (options != null) {
            for (FilterOption opt : options) {
                RadioButton radio = new RadioButton(opt.getLabel());
                radio.setToggleGroup(group);
                radio.getStyleClass().add("filter-radio");
                
                if (opt.getValue().equals(currentValue)) {
                    radio.setSelected(true);
                }
                
                radio.setOnAction(e -> {
                    if (radio.isSelected()) {
                        session.setSingleValue(definition.getName(), opt.getValue());
                        if (onChange != null) onChange.accept(session);
                    }
                });
                
                container.getChildren().add(radio);
            }
        }
        
        return container;
    }
    
    private Node createTextControl() {
        TextField field = new TextField();
        field.getStyleClass().add("filter-text");
        field.setPromptText("Введите значение...");
        
        String currentValue = session.getSingleValue(definition.getName());
        if (currentValue != null) {
            field.setText(currentValue);
        }
        
        field.textProperty().addListener((obs, old, val) -> {
            session.setSingleValue(definition.getName(), val);
            if (onChange != null) onChange.accept(session);
        });
        
        return field;
    }
    
    private Node createNumberControl() {
        TextField field = new TextField();
        field.getStyleClass().add("filter-text");
        field.setPromptText("Введите число...");
        
        Integer currentValue = session.getIntegerValue(definition.getName());
        if (currentValue != null) {
            field.setText(String.valueOf(currentValue));
        }
        
        field.textProperty().addListener((obs, old, val) -> {
            if (val != null && !val.isEmpty()) {
                try {
                    session.setIntegerValue(definition.getName(), Integer.parseInt(val));
                } catch (NumberFormatException ignored) {}
            }
            if (onChange != null) onChange.accept(session);
        });
        
        return field;
    }
    
    private Node createDateControl() {
        DatePicker picker = new DatePicker();
        picker.getStyleClass().add("filter-date");
        
        return picker;
    }
}
