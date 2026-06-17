package com.rmc.http;

import java.net.URI;
import java.util.Optional;

/**
 * Исключение для HTTP операций.
 * 
 * <p>Инкапсулирует информацию об ошибках HTTP:</p>
 * <ul>
 *   <li>URI запроса</li>
 *   <li>HTTP метод</li>
 *   <li>Статус код (если получен)</li>
 *   <li>Тело ошибки</li>
 *   <li>Причина (IOException, Timeout и т.д.)</li>
 * </ul>
 */
public class HttpException extends RuntimeException {
    
    private final URI requestUri;
    private final String method;
    private final Integer statusCode;
    private final String responseBody;
    private final ErrorType errorType;
    
    /**
     * Тип ошибки.
     */
    public enum ErrorType {
        /** Превышен таймаут соединения */
        TIMEOUT,
        /** Невозможно подключиться к хосту */
        CONNECTION_FAILED,
        /** Ошибка DNS (UnknownHostException) */
        DNS_ERROR,
        /** HTTP ошибка (4xx, 5xx) */
        HTTP_ERROR,
        /** Ошибка SSL/TLS */
        SSL_ERROR,
        /** Неизвестная ошибка */
        UNKNOWN
    }
    
    private HttpException(String message, Throwable cause, ErrorType errorType,
                         URI requestUri, String method, Integer statusCode, String responseBody) {
        super(buildMessage(message, requestUri, method, statusCode, errorType), cause);
        this.errorType = errorType;
        this.requestUri = requestUri;
        this.method = method;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
    
    private static String buildMessage(String message, URI uri, String method, 
                                       Integer statusCode, ErrorType errorType) {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP Error");
        
        if (errorType != null) {
            sb.append(" [").append(errorType).append("]");
        }
        
        if (uri != null) {
            sb.append(" for ").append(uri);
        }
        
        if (method != null) {
            sb.append(" (").append(method).append(")");
        }
        
        if (statusCode != null) {
            sb.append(" - Status: ").append(statusCode);
        }
        
        if (message != null && !message.isEmpty()) {
            sb.append(" - ").append(message);
        }
        
        return sb.toString();
    }
    
    /**
     * Создать исключение для таймаута.
     */
    public static HttpException timeout(URI uri, String method, Throwable cause) {
        return new HttpException(
            "Request timed out",
            cause,
            ErrorType.TIMEOUT,
            uri,
            method,
            null,
            null
        );
    }
    
    /**
     * Создать исключение для ошибки соединения.
     */
    public static HttpException connectionFailed(URI uri, String method, Throwable cause) {
        ErrorType type = cause.getClass().getSimpleName().contains("UnknownHost") 
            ? ErrorType.DNS_ERROR 
            : ErrorType.CONNECTION_FAILED;
        
        return new HttpException(
            cause.getMessage(),
            cause,
            type,
            uri,
            method,
            null,
            null
        );
    }
    
    /**
     * Создать исключение для HTTP ошибки.
     */
    public static HttpException httpError(URI uri, String method, int statusCode, String body) {
        return new HttpException(
            "HTTP request failed",
            null,
            ErrorType.HTTP_ERROR,
            uri,
            method,
            statusCode,
            body
        );
    }
    
    /**
     * Создать исключение для SSL ошибки.
     */
    public static HttpException sslError(URI uri, String method, Throwable cause) {
        return new HttpException(
            "SSL/TLS error",
            cause,
            ErrorType.SSL_ERROR,
            uri,
            method,
            null,
            null
        );
    }
    
    /**
     * @return URI запроса
     */
    public Optional<URI> getRequestUri() {
        return Optional.ofNullable(requestUri);
    }
    
    /**
     * @return HTTP метод
     */
    public Optional<String> getMethod() {
        return Optional.ofNullable(method);
    }
    
    /**
     * @return HTTP статус код (если получен)
     */
    public Optional<Integer> getStatusCode() {
        return Optional.ofNullable(statusCode);
    }
    
    /**
     * @return Тело ошибки
     */
    public Optional<String> getResponseBody() {
        return Optional.ofNullable(responseBody);
    }
    
    /**
     * @return Тип ошибки
     */
    public ErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * @return true если это ошибка таймаута
     */
    public boolean isTimeout() {
        return errorType == ErrorType.TIMEOUT;
    }
    
    /**
     * @return true если это DNS ошибка
     */
    public boolean isDnsError() {
        return errorType == ErrorType.DNS_ERROR;
    }
    
    /**
     * @return true если это HTTP ошибка (4xx, 5xx)
     */
    public boolean isHttpError() {
        return errorType == ErrorType.HTTP_ERROR;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HttpException{");
        sb.append("type=").append(errorType);
        
        if (requestUri != null) {
            sb.append(", uri=").append(requestUri);
        }
        if (method != null) {
            sb.append(", method=").append(method);
        }
        if (statusCode != null) {
            sb.append(", status=").append(statusCode);
        }
        
        sb.append("}");
        return sb.toString();
    }
}
