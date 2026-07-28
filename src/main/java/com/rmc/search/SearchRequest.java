package com.rmc.search;

import java.util.Collections;
import java.util.Map;

/**
 * Запрос на поиск.
 * 
 * <p>Содержит:</p>
 * <ul>
 *   <li>Базовый URL</li>
 *   <li>Параметры поиска (name → value)</li>
 *   <li>Полный URL для запроса</li>
 * </ul>
 */
public class SearchRequest {
    
    private final String baseUrl;
    private final Map<String, String> parameters;
    private final String fullUrl;
    
    private SearchRequest(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.parameters = Collections.unmodifiableMap(builder.parameters);
        this.fullUrl = buildFullUrl(builder.baseUrl, builder.parameters);
    }
    
    /**
     * Создать Builder для построения запроса.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Построить запрос из параметров.
     */
    public static SearchRequest from(Map<String, String> parameters) {
        return builder()
                .baseUrl("https://rmc.example.com/search")
                .parameters(parameters)
                .build();
    }
    
    private String buildFullUrl(String baseUrl, Map<String, String> params) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return "";
        }
        
        StringBuilder url = new StringBuilder(baseUrl);
        
        if (!params.isEmpty()) {
            url.append("?");
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    if (!first) {
                        url.append("&");
                    }
                    url.append(urlEncode(entry.getKey()));
                    url.append("=");
                    url.append(urlEncode(entry.getValue()));
                    first = false;
                }
            }
        }
        
        return url.toString();
    }
    
    /**
     * @return Базовый URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * @return Параметры запроса
     */
    public Map<String, String> getParameters() {
        return parameters;
    }
    
    /**
     * @return Полный URL с параметрами
     */
    public String getFullUrl() {
        return fullUrl;
    }
    
    /**
     * @param name имя параметра
     * @return значение параметра или null
     */
    public String getParameter(String name) {
        return parameters.get(name);
    }
    
    /**
     * @param name имя параметра
     * @return true если параметр установлен
     */
    public boolean hasParameter(String name) {
        String value = parameters.get(name);
        return value != null && !value.isEmpty();
    }
    
    /**
     * @return Количество параметров
     */
    public int getParameterCount() {
        return parameters.size();
    }
    
    /**
     * @return Query string (без URL)
     */
    public String getQueryString() {
        String url = fullUrl;
        int questionIndex = url.indexOf('?');
        if (questionIndex >= 0) {
            return url.substring(questionIndex + 1);
        }
        return "";
    }
    
    /**
     * @return true если есть параметры
     */
    public boolean hasParameters() {
        return !parameters.isEmpty();
    }
    
    @Override
    public String toString() {
        return "SearchRequest{" +
                "baseUrl='" + baseUrl + '\'' +
                ", parameters=" + parameters +
                ", fullUrl='" + fullUrl + '\'' +
                '}';
    }
    
    /**
     * URL encode строки.
     */
    private String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return value;
        }
    }
    
    /**
     * Builder для создания SearchRequest.
     */
    public static class Builder {
        
        private String baseUrl = "";
        private Map<String, String> parameters = Collections.emptyMap();
        
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl != null ? baseUrl : "";
            return this;
        }
        
        public Builder parameters(Map<String, String> parameters) {
            this.parameters = parameters != null ? parameters : Collections.emptyMap();
            return this;
        }
        
        public Builder addParameter(String name, String value) {
            if (this.parameters == Collections.<String, String>emptyMap()) {
                this.parameters = new java.util.HashMap<>();
            }
            if (this.parameters instanceof java.util.Map) {
                ((java.util.Map<String, String>) this.parameters).put(name, value);
            }
            return this;
        }
        
        public SearchRequest build() {
            return new SearchRequest(this);
        }
    }
}
