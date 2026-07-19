package com.rmc.filters.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FilterType.
 */
class FilterTypeTest {
    
    @Test
    @DisplayName("Should identify text type")
    void testTextType() {
        assertEquals(FilterType.TEXT, FilterType.fromHtmlType("text"));
        assertEquals(FilterType.TEXT, FilterType.fromHtmlType("TEXT"));
    }
    
    @Test
    @DisplayName("Should identify checkbox type")
    void testCheckboxType() {
        assertEquals(FilterType.CHECKBOX, FilterType.fromHtmlType("checkbox"));
    }
    
    @Test
    @DisplayName("Should identify radio type")
    void testRadioType() {
        assertEquals(FilterType.RADIO, FilterType.fromHtmlType("radio"));
    }
    
    @Test
    @DisplayName("Should identify select type")
    void testSelectType() {
        assertEquals(FilterType.SELECT, FilterType.fromHtmlType("select"));
    }
    
    @Test
    @DisplayName("Should identify unknown type")
    void testUnknownType() {
        assertEquals(FilterType.UNKNOWN, FilterType.fromHtmlType("custom-type"));
    }
    
    @Test
    @DisplayName("Should return TEXT for null or empty type")
    void testDefaultType() {
        assertEquals(FilterType.TEXT, FilterType.fromHtmlType(null));
        assertEquals(FilterType.TEXT, FilterType.fromHtmlType(""));
    }
    
    @Test
    @DisplayName("Should identify text based types")
    void testIsTextBased() {
        assertTrue(FilterType.TEXT.isTextBased());
        assertTrue(FilterType.PASSWORD.isTextBased());
        assertTrue(FilterType.EMAIL.isTextBased());
        assertTrue(FilterType.TEXTAREA.isTextBased());
        
        assertFalse(FilterType.CHECKBOX.isTextBased());
        assertFalse(FilterType.SELECT.isTextBased());
    }
    
    @Test
    @DisplayName("Should identify selection based types")
    void testIsSelectionBased() {
        assertTrue(FilterType.SELECT.isSelectionBased());
        assertTrue(FilterType.SELECT_MULTIPLE.isSelectionBased());
        assertTrue(FilterType.CHECKBOX.isSelectionBased());
        assertTrue(FilterType.RADIO.isSelectionBased());
        
        assertFalse(FilterType.TEXT.isSelectionBased());
        assertFalse(FilterType.PASSWORD.isSelectionBased());
    }
    
    @Test
    @DisplayName("Should identify select types")
    void testIsSelect() {
        assertTrue(FilterType.SELECT.isSelect());
        assertTrue(FilterType.SELECT_MULTIPLE.isSelect());
        
        assertFalse(FilterType.TEXT.isSelect());
        assertFalse(FilterType.CHECKBOX.isSelect());
    }
    
    @Test
    @DisplayName("Should identify multiple selection")
    void testIsMultiple() {
        assertTrue(FilterType.SELECT_MULTIPLE.isMultiple());
        
        assertFalse(FilterType.SELECT.isMultiple());
        assertFalse(FilterType.CHECKBOX.isMultiple());
    }
    
    @Test
    @DisplayName("Should return display name")
    void testDisplayName() {
        assertEquals("Текстовое поле", FilterType.TEXT.getDisplayName());
        assertEquals("Флажок", FilterType.CHECKBOX.getDisplayName());
        assertEquals("Выпадающий список", FilterType.SELECT.getDisplayName());
    }
}
