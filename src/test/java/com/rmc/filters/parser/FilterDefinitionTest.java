package com.rmc.filters.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FilterDefinition.
 */
class FilterDefinitionTest {
    
    @Test
    @DisplayName("Should create FilterDefinition with builder")
    void testBuilder() {
        FilterOption option = FilterOption.builder()
                .value("opt1")
                .label("Option 1")
                .build();
        
        FilterDefinition filter = FilterDefinition.builder()
                .name("field1")
                .caption("Field Caption")
                .type(FilterType.SELECT)
                .value("default")
                .options(List.of(option))
                .placeholder("Enter value")
                .required(true)
                .multiple(false)
                .disabled(false)
                .id("field-id")
                .build();
        
        assertEquals("field1", filter.getName());
        assertEquals("Field Caption", filter.getCaption());
        assertEquals(FilterType.SELECT, filter.getType());
        assertEquals("default", filter.getValue());
        assertEquals(1, filter.getOptions().size());
        assertEquals("Enter value", filter.getPlaceholder());
        assertTrue(filter.isRequired());
        assertFalse(filter.isMultiple());
        assertFalse(filter.isDisabled());
        assertEquals("field-id", filter.getId());
    }
    
    @Test
    @DisplayName("Should track options")
    void testOptions() {
        FilterDefinition filter = FilterDefinition.builder()
                .name("multi")
                .type(FilterType.SELECT_MULTIPLE)
                .addOption(FilterOption.builder().value("1").label("One").build())
                .addOption(FilterOption.builder().value("2").label("Two").build())
                .build();
        
        assertTrue(filter.hasOptions());
        assertEquals(2, filter.getOptionCount());
    }
    
    @Test
    @DisplayName("Should check select type")
    void testIsSelect() {
        FilterDefinition selectFilter = FilterDefinition.builder()
                .name("select")
                .type(FilterType.SELECT)
                .build();
        
        FilterDefinition textFilter = FilterDefinition.builder()
                .name("text")
                .type(FilterType.TEXT)
                .build();
        
        assertTrue(selectFilter.isSelect());
        assertFalse(textFilter.isSelect());
    }
    
    @Test
    @DisplayName("Should return optional name")
    void testGetNameOpt() {
        FilterDefinition withName = FilterDefinition.builder()
                .name("test")
                .type(FilterType.TEXT)
                .build();
        
        FilterDefinition withoutName = FilterDefinition.builder()
                .type(FilterType.TEXT)
                .build();
        
        assertTrue(withName.getNameOpt().isPresent());
        assertTrue(withoutName.getNameOpt().isEmpty());
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        FilterDefinition filter = FilterDefinition.builder()
                .name("test")
                .caption("Test Field")
                .type(FilterType.TEXT)
                .build();
        
        String str = filter.toString();
        assertTrue(str.contains("test"));
        assertTrue(str.contains("Test Field"));
        assertTrue(str.contains("TEXT"));
    }
}
