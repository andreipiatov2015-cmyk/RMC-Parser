package com.rmc.http;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Обёртка для HTTP ответа.
 * 
 * <p>Инкапсулирует все данные ответа HTTP запроса:</p>
 * <ul>
 *   <li>Статус код</li>
 *   <li>Тело ответа</li>
 *   <li>Заголовки</li>
 *   <li>Время выполнения</li>
 * </ul>
 */
public class HttpResponse {
    
    private final int statusCode;
    private final String body;
    private final Map<String, String> headers;
    private final URI requestUri;
    private final String requestMethod;
    private final Duration responseTime;
    private final boolean successful;
    
    private HttpResponse(Builder builder) {
        this.statusCode = builder.statusCode;
        this.body = builder.body;
        this.headers = Map.copyOf(builder.headers);
        this.requestUri = builder.requestUri;
        this.requestMethod = builder.requestMethod;
        this.responseTime = builder.responseTime;
        this.successful = statusCode >= 200 && statusCode < 400;
    }
    
    /**
     * Создать Builder для построения ответа.
     */
    public static Builder builder() {
        return new Builder();
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
    public String getBody() {
        return body;
    }
    
    /**
     * @return Тело ответа как bytes
     */
    public byte[] getBodyAsBytes() {
        return body != null ? body.getBytes() : new byte[0];
    }
    
    /**
     * @return Заголовки ответа
     */
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    /**
     * Получить значение заголовка.
     * 
     * @param name имя заголовка
     * @return значение заголовка или empty если не найден
     */
    public Optional<String> getHeader(String name) {
        return Optional.ofNullable(headers.get(name));
    }
    
    /**
     * @return URI запроса
     */
    public URI getRequestUri() {
        return requestUri;
    }
    
    /**
     * @return HTTP метод запроса
     */
    public String getRequestMethod() {
        return requestMethod;
    }
    
    /**
     * @return Время выполнения запроса
     */
    public Duration getResponseTime() {
        return responseTime;
    }
    
    /**
     * @return true если запрос успешный (2xx статус код)
     */
    public boolean isSuccessful() {
        return successful;
    }
    
    /**
     * @return true если статус код 2xx
     */
    public boolean isOk() {
        return statusCode == 200;
    }
    
    /**
     * @return true если статус код 3xx (редирект)
     */
    public boolean isRedirect() {
        return statusCode >= 300 && statusCode < 400;
    }
    
    /**
     * @return true если статус код 4xx (ошибка клиента)
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }
    
    /**
     * @return true если статус код 5xx (ошибка сервера)
     */
    public boolean isServerError() {
        return statusCode >= 500;
    }
    
    @Override
    public String toString() {
        return String.format("HttpResponse{status=%d, uri=%s, time=%dms}", 
                statusCode, requestUri, responseTime.toMillis());
    }
    
    /**
     * Builder для создания HttpResponse.
     */
    public static class Builder {
        
        private int statusCode;
        private String body;
        private Map<String, String> headers = Map.of();
        private URI requestUri;
        private String requestMethod;
        private Duration responseTime = Duration.ZERO;
        
        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }
        
        public Builder body(String body) {
            this.body = body;
            return this;
        }
        
        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }
        
        public Builder requestUri(URI requestUri) {
            this.requestUri = requestUri;
            return this;
        }
        
        public Builder requestMethod(String requestMethod) {
            this.requestMethod = requestMethod;
            return this;
        }
        
        public Builder responseTime(Duration responseTime) {
            this.responseTime = responseTime;
            return this;
        }
        
        public HttpResponse build() {
            return new HttpResponse(this);
        }
    }
}
