package com.rmc.ui.dynamic;

import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.parser.FilterOption;
import com.rmc.filters.parser.FilterType;
import javafx.scene.control.Control;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FilterControlFactory.
 */
class FilterControlFactoryTest {
    
    private GridPane gridPane;
    
    @BeforeEach
    void setUp() {
        gridPane = new GridPane();
    }
    
    @Test
    @DisplayName("Should create text field for TEXT type")
    void testCreateTextField() {
        FilterDefinition filter = FilterDefinition.builder()
                .name("search")
                .caption("Search")
                .type(FilterType.TEXT)
                .placeholder("Enter search term")
                .build();
        
        FilterControlFactory.ControlResult result = FilterControlFactory.createControl(filter, gridPane, 0);
        
        assertNotNull(result);
        assertNotNull(result.getControl());
        assertTrue(result.getControl() instanceof javafx.scene.control.TextField);
    }
    
    @Test
    @DisplayName("Should create checkbox for CHECKBOX type")
    void testCreateCheckbox() {
        FilterDefinition filter = FilterDefinition.builder()
                .name("active")
                .caption("Active")
                .type(FilterType.CHECKBOX)
                .value("true")
                .build();
        
        FilterControlFactory.ControlResult result = FilterControlFactory.createControl(filter, gridPane, 0);
        
        assertNotNull(result);
        assertTrue(result.getControl() instanceof javafx.scene.control.CheckBox);
        
        javafx.scene.control.CheckBox checkBox = (javafx.scene.control.CheckBox) result.getControl();
        assertTrue(checkBox.isSelected());
    }
    
    @Test
    @DisplayName("Should create combo box for SELECT type")
    void testCreateComboBox() {
        FilterDefinition filter = FilterDefinition.builder()
                .name("category")
                .caption("Category")
                .type(FilterType.SELECT)
                .addOption(FilterOption.builder().value("all").label("All").build())
                .addOption(FilterOption.builder().value("cat1").label("Category 1").selected(true).build())
                .addOption(FilterOption.builder().value("cat2").label("Category 2").build())
                .build();
        
        FilterControlFactory.ControlResult result = FilterControlFactory.createControl(filter, gridPane, 0);
        
        assertNotNull(result);
        assertTrue(result.getControl() instanceof javafx.scene.control.ComboBox);
        
        @SuppressWarnings("unchecked")
        javafx.scene.control.ComboBox<String> comboBox = (javafx.scene.control.ComboBox<String>) result.getControl();
        assertEquals(3, comboBox.getItems().size());
        assertEquals("Category 1", comboBox.getSelectionModel().getSelectedItem());
    }
    
    @Test
    @DisplayName("Should create text area for TEXTAREA type")
    void testCreateTextArea() {
        FilterDefinition filter = FilterDefinition.builder()
                .name("description")
                .caption("Description")
                .type(FilterType.TEXTAREA)
                .placeholder("Enter description")
                .build();
        
        FilterControlFactory.ControlResult result = FilterControlFactory.createControl(filter, gridPane, 0);
        
        assertNotNull(result);
        assertTrue(result.getControl() instanceof javafx.scene.control.TextArea);
    }
    
    @Test
    @DisplayName("Should create list view for SELECT_MULTIPLE type")
    void testCreateListView() {
        FilterDefinition filter = FilterDefinition.builder()
                .name("tags")
                .caption("Tags")
                .type(FilterType.SELECT_MULTIPLE)
                .multiple(true)
                .addOption(FilterOption.builder().value("tag1").label("Tag 1").selected(true).build())
                .addOption(FilterOption.builder().value("tag2").label("Tag 2").build())
                .build();
        
        FilterControlFactory.ControlResult result = FilterControlFactory.createControl(filter, gridPane, 0);
        
        assertNotNull(result);
        assertTrue(result.getControl() instanceof javafx.scene.control.ListView);
        assertEquals(2, result.getRowIncrement()); // Multiple select uses extra row
    }
    
    @Test
    @DisplayName("Should create radio buttons for RADIO type")
    void testCreateRadioGroup() {
        FilterDefinition filter = FilterDefinition.builder()
                .name("sort")
                .caption("Sort by")
                .type(FilterType.RADIO)
                .addOption(FilterOption.builder().value("name").label("By Name").selected(true).build())
                .addOption(FilterOption.builder().value("date").label("By Date").build())
                .build();
        
        FilterControlFactory.ControlResult result = FilterControlFactory.createControl(filter, gridPane, 0);
        
        assertNotNull(result);
        assertTrue(result.getControl() instanceof javafx.scene.layout.VBox);
    }
    
    @Test
    @DisplayName("Should return null for HIDDEN type")
    void testHiddenType() {
        FilterDefinition filter = FilterDefinition.builder()
                .name("csrf")
                .type(FilterType.HIDDEN)
                .value("token123")
                .build();
        
        FilterControlFactory.ControlResult result = FilterControlFactory.createControl(filter, gridPane, 0);
        
        assertNull(result);
    }
    
    @Test
    @DisplayName("Should handle disabled controls")
    void testDisabledControl() {
        FilterDefinition filter = FilterDefinition.builder()
                .name("disabled")
                .caption("Disabled Field")
                .type(FilterType.TEXT)
                .disabled(true)
                .build();
        
        FilterControlFactory.ControlResult result = FilterControlFactory.createControl(filter, gridPane, 0);
        
        assertNotNull(result);
        Control control = (Control) result.getControl();
        assertTrue(control.isDisabled());
    }
    
    @Test
    @DisplayName("Should use name as caption when caption is null")
    void testNullCaption() {
        FilterDefinition filter = FilterDefinition.builder()
                .name("fieldName")
                .type(FilterType.TEXT)
                .build();
        
        FilterControlFactory.ControlResult result = FilterControlFactory.createControl(filter, gridPane, 0);
        
        assertNotNull(result);
        // Label should contain the name
        assertTrue(gridPane.getChildren().size() >= 2);
    }
    
    @Test
    @DisplayName("Should create DatePicker for DATE type")
    void testCreateDatePicker() {
        FilterDefinition filter = FilterDefinition.builder()
                .name("startDate")
                .caption("Start Date")
                .type(FilterType.DATE)
                .build();
        
        FilterControlFactory.ControlResult result = FilterControlFactory.createControl(filter, gridPane, 0);
        
        assertNotNull(result);
        assertTrue(result.getControl() instanceof javafx.scene.control.DatePicker);
    }
}
