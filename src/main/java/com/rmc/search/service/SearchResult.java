package com.rmc.search.service;

import java.util.Optional;

/**
 * Результат поиска программ.
 * 
 * <p>Содержит:</p>
 * <ul>
 *   <li>HTML страницы результатов</li>
 *   <li>URL запроса</li>
 *   <li>Статус код</li>
 *   <li>Размер контента</li>
 *   <li>Ошибка (если есть)</li>
 * </ul>
 */
public class SearchResult {
    
    private final boolean success;
    private final String html;
    private final String requestUrl;
    private final int statusCode;
    private final long responseTimeMs;
    private final int contentLength;
    private final String errorMessage;
    
    private SearchResult(Builder builder) {
        this.success = builder.success;
        this.html = builder.html;
        this.requestUrl = builder.requestUrl;
        this.statusCode = builder.statusCode;
        this.responseTimeMs = builder.responseTimeMs;
        this.contentLength = builder.contentLength;
        this.errorMessage = builder.errorMessage;
    }
    
    /**
     * Создать Builder для построения результата.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * @return true если поиск успешен
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * @return HTML страницы результатов
     */
    public String getHtml() {
        return html;
    }
    
    /**
     * @return URL запроса
     */
    public String getRequestUrl() {
        return requestUrl;
    }
    
    /**
     * @return HTTP статус код
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * @return Время ответа в миллисекундах
     */
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    /**
     * @return Размер контента в символах
     */
    public int getContentLength() {
        return contentLength;
    }
    
    /**
     * @return Сообщение об ошибке или empty
     */
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }
    
    /**
     * @return true если есть HTML
     */
    public boolean hasHtml() {
        return html != null && !html.isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("SearchResult{success=%s, url=%s, status=%d, size=%d, time=%dms}", 
                success, requestUrl, statusCode, contentLength, responseTimeMs);
    }
    
    /**
     * Builder для создания SearchResult.
     */
    public static class Builder {
        
        private boolean success;
        private String html;
        private String requestUrl;
        private int statusCode;
        private long responseTimeMs;
        private int contentLength;
        private String errorMessage;
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder html(String html) {
            this.html = html;
            this.contentLength = html != null ? html.length() : 0;
            return this;
        }
        
        public Builder requestUrl(String url) {
            this.requestUrl = url;
            return this;
        }
        
        public Builder statusCode(int code) {
            this.statusCode = code;
            return this;
        }
        
        public Builder responseTimeMs(long timeMs) {
            this.responseTimeMs = timeMs;
            return this;
        }
        
        public Builder errorMessage(String message) {
            this.errorMessage = message;
            return this;
        }
        
        public SearchResult build() {
            return new SearchResult(this);
        }
    }
}
