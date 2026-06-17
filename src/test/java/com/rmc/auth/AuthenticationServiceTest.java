package com.rmc.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthenticationService.
 */
class AuthenticationServiceTest {
    
    @Test
    @DisplayName("Should create AuthenticationService with builder")
    void testBuilder() {
        AuthenticationService service = AuthenticationService.builder().build();
        
        assertNotNull(service);
        assertFalse(service.isAuthenticated());
        assertEquals(0, service.getCookieCount());
    }
    
    @Test
    @DisplayName("Should track authentication status")
    void testAuthenticationStatus() {
        AuthenticationService service = AuthenticationService.builder().build();
        
        assertFalse(service.isAuthenticated());
    }
    
    @Test
    @DisplayName("Should track cookie count")
    void testCookieCount() {
        AuthenticationService service = AuthenticationService.builder().build();
        
        assertEquals(0, service.getCookieCount());
    }
    
    @Test
    @DisplayName("Should create LoginRequest correctly")
    void testLoginRequestCreation() {
        LoginRequest request = LoginRequest.builder()
                .loginUrl("https://example.com/login")
                .username("testuser")
                .password("testpass")
                .build();
        
        assertEquals("https://example.com/login", request.getLoginUrl());
        assertEquals("testuser", request.getUsername());
        assertEquals("testpass", request.getPassword());
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        AuthenticationService service = AuthenticationService.builder().build();
        String str = service.toString();
        
        assertTrue(str.contains("AuthenticationService"));
        assertTrue(str.contains("false"));
        assertTrue(str.contains("0"));
    }
    
    @Test
    @DisplayName("Should handle logout when not authenticated")
    void testLogoutWhenNotAuthenticated() {
        AuthenticationService service = AuthenticationService.builder().build();
        
        assertFalse(service.isAuthenticated());
        
        // Should not throw
        assertDoesNotThrow(() -> service.logout());
        
        assertFalse(service.isAuthenticated());
    }
    
    @Test
    @DisplayName("Should check cookies availability")
    void testHasCookies() {
        AuthenticationService service = AuthenticationService.builder().build();
        
        assertFalse(service.hasCookies("https://example.com"));
    }
}
