package com.rmc.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AuthenticationException.
 */
class AuthenticationExceptionTest {
    
    @Test
    @DisplayName("Should create invalid credentials exception")
    void testInvalidCredentials() {
        AuthenticationException ex = AuthenticationException.invalidCredentials("Bad password", 401);
        
        assertEquals(AuthenticationException.ErrorType.INVALID_CREDENTIALS, ex.getErrorType());
        assertEquals(401, ex.getStatusCode());
        assertEquals("Bad password", ex.getMessage());
        assertTrue(ex.isInvalidCredentials());
        assertFalse(ex.isNetworkError());
        assertFalse(ex.isTimeout());
    }
    
    @Test
    @DisplayName("Should create account locked exception")
    void testAccountLocked() {
        AuthenticationException ex = AuthenticationException.accountLocked("Account locked", 403);
        
        assertEquals(AuthenticationException.ErrorType.ACCOUNT_LOCKED, ex.getErrorType());
        assertEquals(403, ex.getStatusCode());
    }
    
    @Test
    @DisplayName("Should create timeout exception")
    void testTimeout() {
        AuthenticationException ex = AuthenticationException.timeout("Connection timed out");
        
        assertEquals(AuthenticationException.ErrorType.TIMEOUT, ex.getErrorType());
        assertEquals(0, ex.getStatusCode());
        assertTrue(ex.isTimeout());
        assertFalse(ex.isNetworkError());
    }
    
    @Test
    @DisplayName("Should create network error exception")
    void testNetworkError() {
        AuthenticationException ex = AuthenticationException.networkError("DNS failed");
        
        assertEquals(AuthenticationException.ErrorType.NETWORK_ERROR, ex.getErrorType());
        assertEquals(0, ex.getStatusCode());
        assertTrue(ex.isNetworkError());
        assertFalse(ex.isTimeout());
    }
    
    @Test
    @DisplayName("Should create unknown exception")
    void testUnknown() {
        AuthenticationException ex = AuthenticationException.unknown("Something went wrong");
        
        assertEquals(AuthenticationException.ErrorType.UNKNOWN, ex.getErrorType());
    }
    
    @Test
    @DisplayName("Should handle null message")
    void testNullMessage() {
        AuthenticationException ex = AuthenticationException.invalidCredentials(null, 401);
        
        assertNotNull(ex.getMessage());
        assertEquals("Неверные учётные данные", ex.getMessage());
    }
    
    @Test
    @DisplayName("Should format toString correctly")
    void testToString() {
        AuthenticationException ex = AuthenticationException.invalidCredentials("Bad", 401);
        
        String str = ex.toString();
        assertTrue(str.contains("AuthenticationException"));
        assertTrue(str.contains("INVALID_CREDENTIALS"));
        assertTrue(str.contains("401"));
    }
}
