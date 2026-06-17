package com.rmc.ui.dynamic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FilterBinder.
 */
class FilterBinderTest {
    
    @Test
    @DisplayName("Should create empty binder")
    void testEmptyBinder() {
        FilterBinder binder = new FilterBinder();
        
        assertEquals(0, binder.getControlCount());
        assertFalse(binder.hasControl("test"));
        assertNull(binder.getValue("test"));
        assertTrue(binder.getAllValues().isEmpty());
    }
    
    @Test
    @DisplayName("Should register and retrieve controls")
    void testRegister() {
        FilterBinder binder = new FilterBinder();
        
        javafx.scene.control.TextField textField = new javafx.scene.control.TextField();
        textField.setText("test value");
        
        binder.register("testField", textField);
        
        assertTrue(binder.hasControl("testField"));
        assertEquals(textField, binder.getControl("testField"));
        assertEquals("test value", binder.getValue("testField"));
        assertEquals(1, binder.getControlCount());
    }
    
    @Test
    @DisplayName("Should get all values")
    void testGetAllValues() {
        FilterBinder binder = new FilterBinder();
        
        javafx.scene.control.TextField field1 = new javafx.scene.control.TextField();
        field1.setText("value1");
        
        javafx.scene.control.TextField field2 = new javafx.scene.control.TextField();
        field2.setText("value2");
        
        binder.register("field1", field1);
        binder.register("field2", field2);
        
        var values = binder.getAllValues();
        
        assertEquals(2, values.size());
        assertEquals("value1", values.get("field1"));
        assertEquals("value2", values.get("field2"));
    }
    
    @Test
    @DisplayName("Should convert to query string")
    void testToQueryString() {
        FilterBinder binder = new FilterBinder();
        
        javafx.scene.control.TextField field1 = new javafx.scene.control.TextField();
        field1.setText("value1");
        
        javafx.scene.control.TextField field2 = new javafx.scene.control.TextField();
        field2.setText("value 2");
        
        binder.register("field1", field1);
        binder.register("field2", field2);
        
        String queryString = binder.toQueryString();
        
        assertTrue(queryString.contains("field1=value1"));
        assertTrue(queryString.contains("field2=value+2"));
    }
    
    @Test
    @DisplayName("Should handle checkbox values")
    void testCheckboxValue() {
        FilterBinder binder = new FilterBinder();
        
        javafx.scene.control.CheckBox checkBox = new javafx.scene.control.CheckBox();
        checkBox.setSelected(true);
        
        binder.register("active", checkBox);
        
        assertEquals("true", binder.getValue("active"));
        
        checkBox.setSelected(false);
        assertEquals("false", binder.getValue("active"));
    }
    
    @Test
    @DisplayName("Should set value to control")
    void testSetValue() {
        FilterBinder binder = new FilterBinder();
        
        javafx.scene.control.TextField textField = new javafx.scene.control.TextField();
        
        binder.register("testField", textField);
        binder.setValue("testField", "new value");
        
        assertEquals("new value", textField.getText());
    }
    
    @Test
    @DisplayName("Should clear all values")
    void testClear() {
        FilterBinder binder = new FilterBinder();
        
        javafx.scene.control.TextField textField = new javafx.scene.control.TextField();
        textField.setText("test");
        
        javafx.scene.control.CheckBox checkBox = new javafx.scene.control.CheckBox();
        checkBox.setSelected(true);
        
        binder.register("textField", textField);
        binder.register("checkBox", checkBox);
        
        binder.clear();
        
        assertEquals("", textField.getText());
        assertFalse(checkBox.isSelected());
    }
    
    @Test
    @DisplayName("Should handle missing control gracefully")
    void testMissingControl() {
        FilterBinder binder = new FilterBinder();
        
        assertNull(binder.getValue("missing"));
        binder.setValue("missing", "value"); // Should not throw
    }
}
