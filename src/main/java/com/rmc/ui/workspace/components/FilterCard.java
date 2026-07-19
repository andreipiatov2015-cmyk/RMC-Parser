package com.rmc.ui.workspace.components;

import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.parser.FilterOption;
import com.rmc.filters.parser.FilterType;
import com.rmc.filters.session.FilterSession;
import com.rmc.filters.ui.Chip;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
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
            return createTextControl();
        }

        return switch (type) {
            case SELECT -> createSelectControl();
            case SELECT_MULTIPLE -> createMultiSelectControl();
            case CHECKBOX -> createCheckboxControl();
            case RADIO -> createRadioControl();
            case NUMBER -> createNumberControl();
            case DATE, DATETIME -> createDateControl();
            case HIDDEN -> new TextField(); // Should not be visible
            default -> createTextControl(); // TEXT, PASSWORD, TEXTAREA, EMAIL, URL, TEL, RANGE, COLOR, FILE, UNKNOWN
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
        String currentValue = session.getValue(definition.getName());
        if (currentValue != null && options != null) {
            for (FilterOption opt : options) {
                if (currentValue.equals(opt.getValue())) {
                    combo.setValue(opt.getLabel());
                    break;
                }
            }
        }

        // Handle selection
        combo.setOnAction(e -> {
            String selectedLabel = combo.getValue();
            if (selectedLabel != null && options != null) {
                for (FilterOption opt : options) {
                    if (opt.getLabel().equals(selectedLabel)) {
                        session.setValue(definition.getName(), opt.getValue());
                        break;
                    }
                }
                if (onChange != null) onChange.accept(session);
            }
        });

        return combo;
    }

    /**
     * Множественный выбор — чипы с возможностью добавления/удаления,
     * повторяет UI сайта (Select2-подобный multiselect).
     */
    private Node createMultiSelectControl() {
        VBox container = new VBox();
        container.setSpacing(8);

        FlowPane chipsPane = new FlowPane();
        chipsPane.getStyleClass().add("filter-chips-pane");
        chipsPane.setHgap(6);
        chipsPane.setVgap(6);

        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getStyleClass().add("filter-combo");
        comboBox.setPromptText("Добавить...");
        comboBox.setMaxWidth(Double.MAX_VALUE);

        List<FilterOption> options = definition.getOptions();

        // Значение добавляется сразу при выборе в списке — без промежуточного
        // клика по отдельной кнопке "+".
        comboBox.setOnAction(e -> {
            String selectedLabel = comboBox.getValue();
            if (selectedLabel == null || options == null) {
                return;
            }
            for (FilterOption opt : options) {
                if (selectedLabel.equals(opt.getLabel())) {
                    session.addMultiValue(definition.getName(), opt.getValue());
                    break;
                }
            }
            // ВАЖНО: нельзя мутировать comboBox.getItems() синхронно внутри
            // его же onAction — в этот момент попап ComboBox ещё обрабатывает
            // клик по своей внутренней ListView, и очистка списка "из-под
            // неё" ломает индексы выделения (IndexOutOfBoundsException).
            // Откладываем перестройку на следующий pulse.
            javafx.application.Platform.runLater(() -> {
                refreshMultiSelect(chipsPane, comboBox, options);
                comboBox.setValue(null);
            });
            if (onChange != null) onChange.accept(session);
        });

        container.getChildren().addAll(chipsPane, comboBox);

        // Переносим варианты, уже отмеченные как selected в исходном HTML,
        // в FilterSession — иначе будет учтён только первый (см. FilterParser).
        if (options != null) {
            for (FilterOption opt : options) {
                if (opt.isSelected() && opt.getValue() != null) {
                    session.addMultiValue(definition.getName(), opt.getValue());
                }
            }
        }

        refreshMultiSelect(chipsPane, comboBox, options);

        return container;
    }

    /**
     * Перестраивает чипы выбранных значений и список доступных для
     * выбора опций (исключая уже выбранные) одним проходом.
     */
    private void refreshMultiSelect(FlowPane chipsPane, ComboBox<String> comboBox, List<FilterOption> options) {
        Set<String> selectedValues = session.getMultiValues(definition.getName());

        chipsPane.getChildren().clear();
        for (String value : selectedValues) {
            String label = findLabelByValue(value, options);
            Chip chip = new Chip(label != null ? label : value);
            chip.getStyleClass().add("filter-chip");
            chip.setOnCloseHandler(e -> {
                session.removeMultiValue(definition.getName(), value);
                refreshMultiSelect(chipsPane, comboBox, options);
                if (onChange != null) onChange.accept(session);
            });
            chipsPane.getChildren().add(chip);
        }

        comboBox.getItems().clear();
        if (options != null) {
            for (FilterOption opt : options) {
                if (!selectedValues.contains(opt.getValue())) {
                    comboBox.getItems().add(opt.getLabel());
                }
            }
        }
    }

    private String findLabelByValue(String value, List<FilterOption> options) {
        if (value == null || options == null) {
            return null;
        }
        for (FilterOption opt : options) {
            if (value.equals(opt.getValue())) {
                return opt.getLabel();
            }
        }
        return null;
    }

    private Node createCheckboxControl() {
        CheckBox checkBox = new CheckBox();
        checkBox.getStyleClass().add("filter-checkbox");

        String currentValue = session.getValue(definition.getName());
        checkBox.setSelected("1".equals(currentValue) || "true".equalsIgnoreCase(currentValue));

        checkBox.setOnAction(e -> {
            session.setValue(definition.getName(), checkBox.isSelected() ? "1" : "");
            if (onChange != null) onChange.accept(session);
        });

        return checkBox;
    }

    private Node createRadioControl() {
        ToggleGroup group = new ToggleGroup();
        VBox container = new VBox();
        container.setSpacing(4);

        List<FilterOption> options = definition.getOptions();
        String currentValue = session.getValue(definition.getName());

        if (options != null) {
            for (FilterOption opt : options) {
                RadioButton radio = new RadioButton(opt.getLabel());
                radio.setToggleGroup(group);
                radio.getStyleClass().add("filter-radio");

                if (opt.getValue() != null && opt.getValue().equals(currentValue)) {
                    radio.setSelected(true);
                }

                radio.setOnAction(e -> {
                    if (radio.isSelected()) {
                        session.setValue(definition.getName(), opt.getValue());
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

        String currentValue = session.getValue(definition.getName());
        if (currentValue != null) {
            field.setText(currentValue);
        }

        field.textProperty().addListener((obs, old, val) -> {
            session.setValue(definition.getName(), val);
            if (onChange != null) onChange.accept(session);
        });

        return field;
    }

    private Node createNumberControl() {
        TextField field = new TextField();
        field.getStyleClass().add("filter-text");
        field.setPromptText("Введите число...");

        String currentValue = session.getValue(definition.getName());
        if (currentValue != null) {
            field.setText(currentValue);
        }

        field.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isEmpty() || val.matches("\\d+")) {
                session.setValue(definition.getName(), val);
                if (onChange != null) onChange.accept(session);
            }
        });

        return field;
    }

    private Node createDateControl() {
        DatePicker picker = new DatePicker();
        picker.getStyleClass().add("filter-date");

        String currentValue = session.getValue(definition.getName());
        if (currentValue != null && !currentValue.isEmpty()) {
            try {
                picker.setValue(LocalDate.parse(currentValue));
            } catch (Exception ignored) {
                // некорректный формат — оставляем пустым
            }
        }

        picker.valueProperty().addListener((obs, old, val) -> {
            session.setValue(definition.getName(), val != null ? val.toString() : null);
            if (onChange != null) onChange.accept(session);
        });

        return picker;
    }
}
