package com.rmc.filters.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FilterOption.
 */
class FilterOptionTest {
    
    @Test
    @DisplayName("Should create FilterOption with builder")
    void testBuilder() {
        FilterOption option = FilterOption.builder()
                .value("value1")
                .label("Label 1")
                .selected(true)
                .disabled(false)
                .build();
        
        assertEquals("value1", option.getValue());
        assertEquals("Label 1", option.getLabel());
        assertTrue(option.isSelected());
        assertFalse(option.isDisabled());
    }
    
    @Test
    @DisplayName("Should create FilterOption from element")
    void testFromOptionElement() {
        FilterOption option = FilterOption.fromOptionElement("opt1", "Option 1", true, false);
        
        assertEquals("opt1", option.getValue());
        assertEquals("Option 1", option.getLabel());
        assertTrue(option.isSelected());
    }
    
    @Test
    @DisplayName("Should return optional value")
    void testGetValueOpt() {
        FilterOption option = FilterOption.builder()
                .value("test")
                .label("Test")
                .build();
        
        assertTrue(option.getValueOpt().isPresent());
        assertEquals("test", option.getValueOpt().get());
    }
    
    @Test
    @DisplayName("Should compare by value")
    void testEquals() {
        FilterOption option1 = FilterOption.builder()
                .value("same")
                .label("Label 1")
                .build();
        
        FilterOption option2 = FilterOption.builder()
                .value("same")
                .label("Different Label")
                .build();
        
        FilterOption option3 = FilterOption.builder()
                .value("different")
                .label("Same Label")
                .build();
        
        assertEquals(option1, option2);
        assertNotEquals(option1, option3);
    }
    
    @Test
    @DisplayName("Should have consistent hashCode")
    void testHashCode() {
        FilterOption option1 = FilterOption.builder()
                .value("test")
                .label("Label")
                .build();
        
        FilterOption option2 = FilterOption.builder()
                .value("test")
                .label("Different")
                .build();
        
        assertEquals(option1.hashCode(), option2.hashCode());
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        FilterOption option = FilterOption.builder()
                .value("val")
                .label("Label")
                .selected(true)
                .build();
        
        String str = option.toString();
        assertTrue(str.contains("val"));
        assertTrue(str.contains("Label"));
        assertTrue(str.contains("selected=true"));
    }
}
