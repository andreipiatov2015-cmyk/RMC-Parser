package com.rmc.auth;

import java.util.Optional;

/**
 * Исключение авторизации.
 */
public class AuthenticationException extends RuntimeException {
    
    private final ErrorType errorType;
    private final int statusCode;
    private final String responseBody;
    
    /**
     * Тип ошибки авторизации.
     */
    public enum ErrorType {
        /** Неверные учётные данные */
        INVALID_CREDENTIALS,
        /** Аккаунт заблокирован */
        ACCOUNT_LOCKED,
        /** Аккаунт отключен */
        ACCOUNT_DISABLED,
        /** Сессия истекла */
        SESSION_EXPIRED,
        /** Ошибка сети */
        NETWORK_ERROR,
        /** Таймаут */
        TIMEOUT,
        /** Неизвестная ошибка */
        UNKNOWN
    }
    
    private AuthenticationException(String message, ErrorType errorType, int statusCode, String responseBody) {
        super(message);
        this.errorType = errorType;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
    
    /**
     * Создать исключение для неверных учётных данных.
     */
    public static AuthenticationException invalidCredentials(String message, int statusCode) {
        return new AuthenticationException(
            message != null ? message : "Неверные учётные данные",
            ErrorType.INVALID_CREDENTIALS,
            statusCode,
            null
        );
    }
    
    /**
     * Создать исключение для заблокированного аккаунта.
     */
    public static AuthenticationException accountLocked(String message, int statusCode) {
        return new AuthenticationException(
            message != null ? message : "Аккаунт заблокирован",
            ErrorType.ACCOUNT_LOCKED,
            statusCode,
            null
        );
    }
    
    /**
     * Создать исключение для таймаута.
     */
    public static AuthenticationException timeout(String message) {
        return new AuthenticationException(
            message != null ? message : "Таймаут авторизации",
            ErrorType.TIMEOUT,
            0,
            null
        );
    }
    
    /**
     * Создать исключение для ошибки сети.
     */
    public static AuthenticationException networkError(String message) {
        return new AuthenticationException(
            message != null ? message : "Ошибка сети",
            ErrorType.NETWORK_ERROR,
            0,
            null
        );
    }
    
    /**
     * Создать исключение для неизвестной ошибки.
     */
    public static AuthenticationException unknown(String message) {
        return new AuthenticationException(
            message != null ? message : "Неизвестная ошибка авторизации",
            ErrorType.UNKNOWN,
            0,
            null
        );
    }
    
    /**
     * @return Тип ошибки
     */
    public ErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * @return HTTP статус код
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * @return Тело ответа
     */
    public Optional<String> getResponseBody() {
        return Optional.ofNullable(responseBody);
    }
    
    /**
     * @return true если это ошибка неверных учётных данных
     */
    public boolean isInvalidCredentials() {
        return errorType == ErrorType.INVALID_CREDENTIALS;
    }
    
    /**
     * @return true если это ошибка сети
     */
    public boolean isNetworkError() {
        return errorType == ErrorType.NETWORK_ERROR;
    }
    
    /**
     * @return true если это таймаут
     */
    public boolean isTimeout() {
        return errorType == ErrorType.TIMEOUT;
    }
    
    @Override
    public String toString() {
        return String.format("AuthenticationException{type=%s, status=%d}", errorType, statusCode);
    }
}
