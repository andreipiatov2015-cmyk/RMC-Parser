package com.rmc.ui.workspace.components;

import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.parser.FilterOption;
import com.rmc.filters.parser.FilterType;
import com.rmc.filters.session.FilterSession;
import com.rmc.filters.ui.Chip;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

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

    /**
     * Составной элемент "поле поиска + список результатов" — заменяет
     * редактируемый ComboBox с фильтрацией. У ComboBox с "живым" поиском
     * по вводу оказался неустранимый на практике баг: клик по варианту в
     * попапе не регистрировался (список тут же переоткрывался заново из-за
     * собственного слушателя изменения текста). Обычный ListView под полем
     * ввода — то же самое поведение, что уже проверено и точно работает в
     * экране "Избранные учреждения".
     */
    private static class SearchablePicker {
        final TextField field;
        final ListView<String> list;
        final FilteredList<String> filtered;
        private boolean suppressFilter;

        SearchablePicker(ObservableList<String> sourceItems, String promptText) {
            field = new TextField();
            field.getStyleClass().add("filter-text");
            field.setPromptText(promptText);

            filtered = new FilteredList<>(sourceItems, s -> true);
            list = new ListView<>(filtered);
            list.getStyleClass().add("filter-select-list");
            list.setPrefHeight(160);
            hideNow();

            field.textProperty().addListener((obs, oldText, newText) -> {
                if (suppressFilter) {
                    return;
                }
                String query = newText == null ? "" : newText.trim().toLowerCase();
                filtered.setPredicate(item -> query.isEmpty() || item.toLowerCase().contains(query));
                show();
            });

            // Показываем список только по вводу текста или явному клику по
            // полю — НЕ по самому факту получения фокуса. Иначе, когда
            // список после выбора прячется (setManaged(false)), JavaFX
            // передаёт фокус следующему по порядку элементу интерфейса
            // (соседнее поле), у того сразу срабатывал бы такой же
            // слушатель "получил фокус -> покажи список" — и список
            // соседнего поля открывался бы сам, каскадом.
            field.setOnMouseClicked(e -> show());
            field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (!isFocused) {
                    hideWithDelay();
                }
            });
        }

        /** Задать текст поля без повторного показа/фильтрации списка — используется после выбора варианта. */
        void setTextSilently(String text) {
            suppressFilter = true;
            field.setText(text);
            suppressFilter = false;
        }

        void show() {
            list.setVisible(true);
            list.setManaged(true);
        }

        void hideNow() {
            list.setVisible(false);
            list.setManaged(false);
        }

        /** Скрыть с небольшой задержкой — чтобы клик по варианту в списке успел обработаться раньше, чем список спрячется при потере фокуса полем. */
        void hideWithDelay() {
            PauseTransition pause = new PauseTransition(Duration.millis(180));
            pause.setOnFinished(e -> hideNow());
            pause.play();
        }
    }

    private Node createSelectControl() {
        List<FilterOption> options = definition.getOptions();
        ObservableList<String> availableItems = FXCollections.observableArrayList();
        if (options != null) {
            for (FilterOption opt : options) {
                availableItems.add(opt.getLabel());
            }
        }

        SearchablePicker picker = new SearchablePicker(availableItems, "Выберите...");

        // Set value from session
        String currentValue = session.getValue(definition.getName());
        if (currentValue != null && options != null) {
            for (FilterOption opt : options) {
                if (currentValue.equals(opt.getValue())) {
                    picker.setTextSilently(opt.getLabel());
                    break;
                }
            }
        }

        picker.list.setOnMouseClicked(e -> {
            String selectedLabel = picker.list.getSelectionModel().getSelectedItem();
            if (selectedLabel == null || options == null) {
                return;
            }
            for (FilterOption opt : options) {
                if (opt.getLabel().equals(selectedLabel)) {
                    session.setValue(definition.getName(), opt.getValue());
                    break;
                }
            }
            picker.setTextSilently(selectedLabel);
            picker.hideNow();
            picker.field.requestFocus();
            if (onChange != null) onChange.accept(session);
        });

        VBox container = new VBox(2, picker.field, picker.list);
        return container;
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

        ObservableList<String> availableItems = FXCollections.observableArrayList();
        SearchablePicker picker = new SearchablePicker(availableItems, "Добавить...");

        List<FilterOption> options = definition.getOptions();

        picker.list.setOnMouseClicked(e -> {
            String selectedLabel = picker.list.getSelectionModel().getSelectedItem();
            if (selectedLabel == null || options == null) {
                return;
            }
            for (FilterOption opt : options) {
                if (selectedLabel.equals(opt.getLabel())) {
                    session.addMultiValue(definition.getName(), opt.getValue());
                    break;
                }
            }
            picker.setTextSilently("");
            picker.hideNow();
            picker.field.requestFocus();
            // ВАЖНО: нельзя мутировать availableItems синхронно здесь — сам
            // ListView в этот момент ещё обрабатывает свой собственный клик
            // (обновление модели выделения/прокрутки), и обновление списка
            // "из-под него" сбивает выделение на соседний пункт (эффект
            // "перепрыгивает на следующее"). Откладываем на следующий pulse.
            javafx.application.Platform.runLater(() -> refreshMultiSelect(chipsPane, availableItems, options));
            if (onChange != null) onChange.accept(session);
        });

        container.getChildren().addAll(chipsPane, picker.field, picker.list);

        // Переносим варианты, уже отмеченные как selected в исходном HTML,
        // в FilterSession — иначе будет учтён только первый (см. FilterParser).
        if (options != null) {
            for (FilterOption opt : options) {
                if (opt.isSelected() && opt.getValue() != null) {
                    session.addMultiValue(definition.getName(), opt.getValue());
                }
            }
        }

        refreshMultiSelect(chipsPane, availableItems, options);

        return container;
    }

    /**
     * Перестраивает чипы выбранных значений и список доступных для
     * выбора опций (исключая уже выбранные) одним проходом.
     */
    private void refreshMultiSelect(FlowPane chipsPane, ObservableList<String> availableItems,
                                     List<FilterOption> options) {
        Set<String> selectedValues = session.getMultiValues(definition.getName());

        chipsPane.getChildren().clear();
        for (String value : selectedValues) {
            String label = findLabelByValue(value, options);
            Chip chip = new Chip(label != null ? label : value);
            chip.getStyleClass().add("filter-chip");
            chip.setOnCloseHandler(e -> {
                session.removeMultiValue(definition.getName(), value);
                refreshMultiSelect(chipsPane, availableItems, options);
                if (onChange != null) onChange.accept(session);
            });
            chipsPane.getChildren().add(chip);
        }

        availableItems.clear();
        if (options != null) {
            for (FilterOption opt : options) {
                if (!selectedValues.contains(opt.getValue())) {
                    availableItems.add(opt.getLabel());
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
