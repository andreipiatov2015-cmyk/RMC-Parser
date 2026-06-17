package com.rmc.search.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProgramSearchService.
 */
class ProgramSearchServiceTest {
    
    @Test
    @DisplayName("Should create service with builder")
    void testBuilder() {
        ProgramSearchService service = ProgramSearchService.builder().build();
        
        assertNotNull(service);
        assertNotNull(service.getHttpClient());
    }
    
    @Test
    @DisplayName("Should create service with custom HTTP client")
    void testCustomHttpClient() {
        com.rmc.http.HttpClientService httpClient = com.rmc.http.HttpClientService.builder()
                .requestTimeoutSeconds(60)
                .build();
        
        ProgramSearchService service = ProgramSearchService.builder()
                .httpClient(httpClient)
                .build();
        
        assertNotNull(service);
        assertEquals(httpClient, service.getHttpClient());
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        ProgramSearchService service = ProgramSearchService.builder().build();
        
        String str = service.toString();
        assertTrue(str.contains("ProgramSearchService"));
    }
}
