package com.rmc.filters.loader;

import com.rmc.http.HttpClientService;
import com.rmc.http.HttpException;
import com.rmc.http.HttpResponse;
import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.net.URI;

/**
 * Загрузчик страницы фильтров.
 * 
 * <p>Выполняет GET запрос к /programs/ для получения HTML страницы фильтров.
 * Сохраняет HTML в памяти для дальнейшей обработки.</p>
 * 
 * <p>Не выполняет парсинг HTML - это будет сделано в следующих задачах.</p>
 */
public class FilterPageLoader {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private final HttpClientService httpClient;
    private final String baseUrl;
    private String lastHtml;
    
    private FilterPageLoader(Builder builder) {
        this.httpClient = builder.httpClient;
        this.baseUrl = builder.baseUrl;
    }
    
    /**
     * Создать Builder для построения FilterPageLoader.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Загрузить страницу фильтров.
     * 
     * <p>Выполняет GET запрос к {baseUrl}/programs/</p>
     * 
     * @return результат загрузки
     */
    public FilterPageResult load() {
        String url = buildProgramsUrl();
        
        logger.info(LOG_LOADING_PAGE);
        
        try {
            HttpResponse response = httpClient.get(url);
            
            String html = response.getBody();
            this.lastHtml = html;
            
            int size = html != null ? html.length() : 0;
            
            logger.info(LOG_PAGE_LOADED);
            logger.info(LOG_HTML_SIZE, size);
            
            return FilterPageResult.builder()
                    .success(true)
                    .html(html)
                    .statusCode(response.getStatusCode())
                    .url(url)
                    .size(size)
                    .build();
            
        } catch (HttpException e) {
            logger.error(LOG_LOAD_ERROR, e.getMessage());
            
            return FilterPageResult.builder()
                    .success(false)
                    .url(url)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
    
    /**
     * Получить последний загруженный HTML.
     * 
     * @return HTML или null если страница не загружалась
     */
    public String getLastHtml() {
        return lastHtml;
    }
    
    /**
     * Проверить, есть ли сохранённый HTML.
     * 
     * @return true если HTML был загружен
     */
    public boolean hasHtml() {
        return lastHtml != null && !lastHtml.isEmpty();
    }
    
    /**
     * Получить размер последнего загруженного HTML.
     * 
     * @return размер в символах или 0
     */
    public int getLastHtmlSize() {
        return lastHtml != null ? lastHtml.length() : 0;
    }
    
    private String buildProgramsUrl() {
        String base = baseUrl;
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return base + "programs/";
    }
    
    @Override
    public String toString() {
        return String.format("FilterPageLoader{hasHtml=%s, size=%d}", 
                hasHtml(), getLastHtmlSize());
    }
    
    /**
     * Builder для создания FilterPageLoader.
     */
    public static class Builder {
        
        private HttpClientService httpClient;
        private String baseUrl = "https://rmc.example.com";
        
        public Builder httpClient(HttpClientService httpClient) {
            this.httpClient = httpClient;
            return this;
        }
        
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }
        
        public FilterPageLoader build() {
            if (httpClient == null) {
                httpClient = HttpClientService.builder().build();
            }
            return new FilterPageLoader(this);
        }
    }
    
    /**
     * Константы для логирования.
     */
    private static final String LOG_LOADING_PAGE = "Получение страницы фильтров...";
    private static final String LOG_PAGE_LOADED = "Страница получена.";
    private static final String LOG_HTML_SIZE = "Размер HTML: {} символов";
    private static final String LOG_LOAD_ERROR = "Ошибка загрузки страницы: {}";
}
