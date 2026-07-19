package com.rmc.filters.loader;

import java.util.Optional;

/**
 * Результат загрузки страницы фильтров.
 */
public class FilterPageResult {
    
    private final boolean success;
    private final String html;
    private final int statusCode;
    private final String url;
    private final int size;
    private final String errorMessage;
    
    private FilterPageResult(Builder builder) {
        this.success = builder.success;
        this.html = builder.html;
        this.statusCode = builder.statusCode;
        this.url = builder.url;
        this.size = builder.size;
        this.errorMessage = builder.errorMessage;
    }
    
    /**
     * Создать Builder для построения результата.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * @return true если загрузка успешна
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * @return HTML страницы
     */
    public String getHtml() {
        return html;
    }
    
    /**
     * @return HTTP статус код
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * @return URL загруженной страницы
     */
    public String getUrl() {
        return url;
    }
    
    /**
     * @return Размер HTML в символах
     */
    public int getSize() {
        return size;
    }
    
    /**
     * @return Сообщение об ошибке или empty
     */
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }
    
    @Override
    public String toString() {
        return String.format("FilterPageResult{success=%s, url=%s, size=%d}", 
                success, url, size);
    }
    
    /**
     * Builder для создания FilterPageResult.
     */
    public static class Builder {
        
        private boolean success;
        private String html;
        private int statusCode;
        private String url;
        private int size;
        private String errorMessage;
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder html(String html) {
            this.html = html;
            return this;
        }
        
        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }
        
        public Builder url(String url) {
            this.url = url;
            return this;
        }
        
        public Builder size(int size) {
            this.size = size;
            return this;
        }
        
        public Builder errorMessage(String message) {
            this.errorMessage = message;
            return this;
        }
        
        public FilterPageResult build() {
            return new FilterPageResult(this);
        }
    }
}
