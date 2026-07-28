package com.rmc.filters.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FilterParser.
 */
class FilterParserTest {
    
    @Test
    @DisplayName("Should parse select element")
    void testParseSelect() {
        String html = """
            <html>
            <body>
                <form>
                    <label for="category">Category</label>
                    <select name="category" id="category">
                        <option value="all">All</option>
                        <option value="cat1" selected>Category 1</option>
                        <option value="cat2">Category 2</option>
                    </select>
                </form>
            </body>
            </html>
            """;
        
        FilterParser.ParseResult result = FilterParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertEquals(1, result.getFilterCount());
        
        FilterDefinition filter = result.getFilters().get(0);
        assertEquals("category", filter.getName());
        assertEquals("Category", filter.getCaption());
        assertEquals(FilterType.SELECT, filter.getType());
        assertEquals(3, filter.getOptions().size());
        assertEquals("cat1", filter.getValue()); // selected option
    }
    
    @Test
    @DisplayName("Should parse input text element")
    void testParseInputText() {
        String html = """
            <html>
            <body>
                <form>
                    <label for="search">Search</label>
                    <input type="text" name="search" id="search" placeholder="Enter search term">
                </form>
            </body>
            </html>
            """;
        
        FilterParser.ParseResult result = FilterParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertEquals(1, result.getFilterCount());
        
        FilterDefinition filter = result.getFilters().get(0);
        assertEquals("search", filter.getName());
        assertEquals("Search", filter.getCaption());
        assertEquals(FilterType.TEXT, filter.getType());
        assertEquals("Enter search term", filter.getPlaceholder());
    }
    
    @Test
    @DisplayName("Should parse checkbox elements")
    void testParseCheckbox() {
        String html = """
            <html>
            <body>
                <form>
                    <input type="checkbox" name="active" value="yes" checked>
                    <label for="active">Active</label>
                    <input type="checkbox" name="featured" value="yes">
                </form>
            </body>
            </html>
            """;
        
        FilterParser.ParseResult result = FilterParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertEquals(2, result.getFilterCount());
        
        FilterDefinition activeFilter = result.getFilters().get(0);
        assertEquals("active", activeFilter.getName());
        assertEquals(FilterType.CHECKBOX, activeFilter.getType());
        assertEquals("yes", activeFilter.getValue());
    }
    
    @Test
    @DisplayName("Should parse radio elements")
    void testParseRadio() {
        String html = """
            <html>
            <body>
                <form>
                    <label>Sort by</label>
                    <input type="radio" name="sort" value="name" checked>
                    <label for="sort-name">By Name</label>
                    <input type="radio" id="sort-name" type="radio" name="sort" value="name">
                    <input type="radio" name="sort" value="date">
                </form>
            </body>
            </html>
            """;
        
        FilterParser.ParseResult result = FilterParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getFilterCount() >= 1);
    }
    
    @Test
    @DisplayName("Should parse textarea element")
    void testParseTextarea() {
        String html = """
            <html>
            <body>
                <form>
                    <label for="description">Description</label>
                    <textarea name="description" id="description" placeholder="Enter description"></textarea>
                </form>
            </body>
            </html>
            """;
        
        FilterParser.ParseResult result = FilterParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertEquals(1, result.getFilterCount());
        
        FilterDefinition filter = result.getFilters().get(0);
        assertEquals("description", filter.getName());
        assertEquals("Description", filter.getCaption());
        assertEquals(FilterType.TEXTAREA, filter.getType());
    }
    
    @Test
    @DisplayName("Should parse multiple select")
    void testParseMultipleSelect() {
        String html = """
            <html>
            <body>
                <form>
                    <select name="tags" multiple>
                        <option value="tag1">Tag 1</option>
                        <option value="tag2" selected>Tag 2</option>
                        <option value="tag3">Tag 3</option>
                    </select>
                </form>
            </body>
            </html>
            """;
        
        FilterParser.ParseResult result = FilterParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertEquals(1, result.getFilterCount());
        
        FilterDefinition filter = result.getFilters().get(0);
        assertEquals("tags", filter.getName());
        assertEquals(FilterType.SELECT_MULTIPLE, filter.getType());
        assertTrue(filter.isMultiple());
        assertEquals(3, filter.getOptions().size());
    }
    
    @Test
    @DisplayName("Should return error for null HTML")
    void testNullHtml() {
        FilterParser.ParseResult result = FilterParser.parse(null);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().isPresent());
    }
    
    @Test
    @DisplayName("Should return error for empty HTML")
    void testEmptyHtml() {
        FilterParser.ParseResult result = FilterParser.parse("");
        
        assertFalse(result.isSuccess());
    }
    
    @Test
    @DisplayName("Should skip elements without name")
    void testSkipWithoutName() {
        String html = """
            <html>
            <body>
                <form>
                    <input type="text">
                    <select></select>
                    <input type="text" name="withname" value="test">
                </form>
            </body>
            </html>
            """;
        
        FilterParser.ParseResult result = FilterParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertEquals(1, result.getFilterCount());
        assertEquals("withname", result.getFilters().get(0).getName());
    }
    
    @Test
    @DisplayName("Should parse hidden input")
    void testParseHiddenInput() {
        String html = """
            <html>
            <body>
                <form>
                    <input type="hidden" name="csrf" value="token123">
                </form>
            </body>
            </html>
            """;
        
        FilterParser.ParseResult result = FilterParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertEquals(1, result.getFilterCount());
        
        FilterDefinition filter = result.getFilters().get(0);
        assertEquals("csrf", filter.getName());
        assertEquals(FilterType.HIDDEN, filter.getType());
        assertEquals("token123", filter.getValue());
    }
    
    @Test
    @DisplayName("Should parse complex form with all elements")
    void testParseComplexForm() {
        String html = """
            <html>
            <body>
                <form>
                    <label for="query">Search</label>
                    <input type="text" name="query" id="query">
                    
                    <label for="category">Category</label>
                    <select name="category" id="category">
                        <option value="all">All</option>
                    </select>
                    
                    <input type="checkbox" name="active" value="1">
                    <label>Active Only</label>
                    
                    <textarea name="notes" id="notes"></textarea>
                </form>
            </body>
            </html>
            """;
        
        FilterParser.ParseResult result = FilterParser.parse(html);
        
        assertTrue(result.isSuccess());
        assertEquals(4, result.getFilterCount());
    }
}
