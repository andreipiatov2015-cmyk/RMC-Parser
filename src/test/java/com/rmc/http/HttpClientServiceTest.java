package com.rmc.http;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HttpClientService.
 */
class HttpClientServiceTest {
    
    @Test
    @DisplayName("Should create HttpClientService with defaults")
    void testDefaultBuilder() {
        HttpClientService client = HttpClientService.builder().build();
        
        assertNotNull(client);
        assertNotNull(client.getSessionManager());
    }
    
    @Test
    @DisplayName("Should create HttpClientService with custom timeout")
    void testCustomRequestTimeout() {
        HttpClientService client = HttpClientService.builder()
                .requestTimeout(Duration.ofSeconds(60))
                .build();
        
        assertNotNull(client);
    }
    
    @Test
    @DisplayName("Should create HttpClientService with custom timeout in seconds")
    void testCustomRequestTimeoutSeconds() {
        HttpClientService client = HttpClientService.builder()
                .requestTimeoutSeconds(45)
                .build();
        
        assertNotNull(client);
    }
    
    @Test
    @DisplayName("Should create HttpClientService with custom SessionManager")
    void testCustomSessionManager() {
        SessionManager session = SessionManager.builder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        
        HttpClientService client = HttpClientService.builder()
                .sessionManager(session)
                .build();
        
        assertNotNull(client);
        assertEquals(session, client.getSessionManager());
    }
    
    @Test
    @DisplayName("Should disable request logging")
    void testDisableRequestLogging() {
        HttpClientService client = HttpClientService.builder()
                .logRequests(false)
                .logResponses(false)
                .build();
        
        assertNotNull(client);
    }
    
    @Test
    @DisplayName("Should check cookies availability")
    void testHasCookies() {
        HttpClientService client = HttpClientService.builder().build();
        
        // No cookies yet
        assertFalse(client.hasCookies("https://example.com"));
        
        // Add cookie through session
        client.getSessionManager().addCookie(
                java.net.URI.create("https://example.com"), 
                "test", 
                "value"
        );
        
        assertTrue(client.hasCookies("https://example.com"));
    }
    
    @Test
    @DisplayName("Should clear cookies")
    void testClearCookies() {
        HttpClientService client = HttpClientService.builder().build();
        
        // Add cookie
        client.getSessionManager().addCookie(
                java.net.URI.create("https://example.com"), 
                "test", 
                "value"
        );
        assertTrue(client.hasCookies("https://example.com"));
        
        // Clear cookies
        client.clearCookies();
        assertFalse(client.hasCookies("https://example.com"));
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        HttpClientService client = HttpClientService.builder().build();
        String str = client.toString();
        
        assertTrue(str.contains("HttpClientService"));
    }
    
    @Test
    @DisplayName("Should use provided session manager if not null")
    void testSessionManagerNotOverwritten() {
        SessionManager customManager = SessionManager.builder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        HttpClientService client = HttpClientService.builder()
                .sessionManager(customManager)
                .build();
        
        // The session manager should be the custom one
        assertSame(customManager, client.getSessionManager());
    }
}
