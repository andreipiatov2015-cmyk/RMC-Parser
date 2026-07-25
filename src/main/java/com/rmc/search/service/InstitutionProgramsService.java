package com.rmc.search.service;

import com.rmc.logging.AppLogger;
import com.rmc.parser.ProgramDetailParser;
import com.rmc.parser.ProgramParser;
import com.rmc.parser.model.Program;
import com.rmc.parser.model.ProgramDetail;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Для одного учреждения: список его программ (через фильтр
 * {@code school_id__in}, с обходом пагинации) и детальные данные по
 * каждой программе — показатели зачислений, действующие группы и
 * количество детей в каждой.
 */
public class InstitutionProgramsService {
    
    private static final Logger logger = AppLogger.getLogger();
    private static final int MAX_PAGES = 200;
    
    public interface ProgressListener {
        void onProgress(String message);
    }
    
    private final ProgramSearchService searchService;
    private final String baseUrl;
    
    private InstitutionProgramsService(Builder builder) {
        this.searchService = builder.searchService;
        this.baseUrl = builder.baseUrl;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public List<ProgramDetail> loadPrograms(String institutionId, ProgressListener listener) {
        List<Program> programs = new ArrayList<>();
        
        String nextRelativeUrl = "/programs/?school_id__in=" + institutionId;
        int pageCount = 0;
        
        while (nextRelativeUrl != null && pageCount < MAX_PAGES) {
            pageCount++;
            report(listener, "Загрузка списка программ, страница " + pageCount + "...");
            
            SearchResult pageResult = searchService.search(resolveUrl(nextRelativeUrl));
            if (!pageResult.isSuccess()) {
                logger.error("Не удалось загрузить список программ учреждения {}: {}",
                        institutionId, pageResult.getErrorMessage().orElse(""));
                break;
            }
            
            ProgramParser.ParseResult parseResult = ProgramParser.parse(pageResult.getHtml());
            if (!parseResult.isSuccess()) {
                logger.error("Не удалось разобрать список программ учреждения {}: {}",
                        institutionId, parseResult.getErrorMessage().orElse(""));
                break;
            }
            
            programs.addAll(parseResult.getPrograms());
            nextRelativeUrl = parseResult.hasNextPage() ? parseResult.getNextPageUrl().orElse(null) : null;
        }
        
        List<ProgramDetail> details = new ArrayList<>();
        int index = 0;
        int total = programs.size();
        
        for (Program program : programs) {
            index++;
            report(listener, "Программа " + index + " из " + total + ": " + program.getTitle());
            
            details.add(loadProgramDetail(program));
        }
        
        return details;
    }
    
    private ProgramDetail loadProgramDetail(Program program) {
        Optional<String> relativeUrl = program.getUrl();
        if (relativeUrl.isEmpty()) {
            return ProgramDetail.builder()
                    .programId(program.getId())
                    .title(program.getTitle())
                    .success(false)
                    .errorMessage("У программы нет ссылки на страницу")
                    .build();
        }
        
        SearchResult detailResult = searchService.search(resolveUrl(relativeUrl.get()));
        if (!detailResult.isSuccess()) {
            return ProgramDetail.builder()
                    .programId(program.getId())
                    .title(program.getTitle())
                    .success(false)
                    .errorMessage(detailResult.getErrorMessage().orElse("Ошибка HTTP-запроса"))
                    .build();
        }
        
        ProgramDetailParser.ParseResult parseResult = ProgramDetailParser.parse(detailResult.getHtml());
        
        return ProgramDetail.builder()
                .programId(program.getId())
                .title(program.getTitle())
                .success(parseResult.isSuccess())
                .errorMessage(parseResult.getErrorMessage().orElse(null))
                .stats(parseResult.getStats())
                .activeGroupsCount(parseResult.getActiveGroupsCount().orElse(null))
                .groups(parseResult.getGroups())
                .build();
    }
    
    private String resolveUrl(String hrefOrUrl) {
        if (hrefOrUrl == null || hrefOrUrl.isEmpty()) {
            return null;
        }
        if (hrefOrUrl.startsWith("http://") || hrefOrUrl.startsWith("https://")) {
            return hrefOrUrl;
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = hrefOrUrl.startsWith("/") ? hrefOrUrl : "/" + hrefOrUrl;
        return base + path;
    }
    
    private void report(ProgressListener listener, String message) {
        logger.info(message);
        if (listener != null) {
            listener.onProgress(message);
        }
    }
    
    public static class Builder {
        
        private ProgramSearchService searchService;
        private String baseUrl;
        
        public Builder searchService(ProgramSearchService searchService) {
            this.searchService = searchService;
            return this;
        }
        
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }
        
        public InstitutionProgramsService build() {
            if (searchService == null) {
                searchService = ProgramSearchService.builder().build();
            }
            return new InstitutionProgramsService(this);
        }
    }
}
