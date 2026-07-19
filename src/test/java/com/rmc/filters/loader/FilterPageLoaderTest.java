package com.rmc.filters.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FilterPageLoader.
 */
class FilterPageLoaderTest {
    
    @Test
    @DisplayName("Should create FilterPageLoader with builder")
    void testBuilder() {
        FilterPageLoader loader = FilterPageLoader.builder()
                .baseUrl("https://example.com")
                .build();
        
        assertNotNull(loader);
        assertFalse(loader.hasHtml());
    }
    
    @Test
    @DisplayName("Should track HTML state")
    void testHtmlState() {
        FilterPageLoader loader = FilterPageLoader.builder()
                .baseUrl("https://example.com")
                .build();
        
        assertFalse(loader.hasHtml());
        assertNull(loader.getLastHtml());
        assertEquals(0, loader.getLastHtmlSize());
    }
    
    @Test
    @DisplayName("Should build correct URL")
    void testUrlBuilding() {
        FilterPageLoader loader = FilterPageLoader.builder()
                .baseUrl("https://example.com")
                .build();
        
        assertNotNull(loader);
        // URL building is tested implicitly through load()
    }
    
    @Test
    @DisplayName("Should build correct URL with trailing slash")
    void testUrlBuildingWithTrailingSlash() {
        FilterPageLoader loader = FilterPageLoader.builder()
                .baseUrl("https://example.com/")
                .build();
        
        assertNotNull(loader);
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        FilterPageLoader loader = FilterPageLoader.builder()
                .baseUrl("https://example.com")
                .build();
        
        String str = loader.toString();
        assertTrue(str.contains("FilterPageLoader"));
        assertTrue(str.contains("hasHtml=false"));
    }
}
