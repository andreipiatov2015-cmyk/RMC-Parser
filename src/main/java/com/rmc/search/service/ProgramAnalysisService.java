package com.rmc.search.service;

import com.rmc.logging.AppLogger;
import com.rmc.parser.ProgramDetailParser;
import com.rmc.parser.ProgramParser;
import com.rmc.parser.model.Organization;
import com.rmc.parser.model.Program;
import com.rmc.search.model.AnalysisResult;
import com.rmc.search.model.InstitutionAnalysis;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Оркестрирует полный цикл анализа по выбранным фильтрам:
 *
 * <ol>
 *   <li>идёт по страницам списка программ (следуя пагинации), пока
 *       программы не закончатся;</li>
 *   <li>группирует найденные программы по учреждению (по ID, т.к. одно
 *       учреждение обычно ведёт несколько программ);</li>
 *   <li>для каждого учреждения заходит на страницу КАЖДОЙ его программы,
 *       вошедшей в отфильтрованный список, и суммирует их показатели —
 *       не показатели учреждения целиком, а только тех программ, что
 *       реально прошли фильтр (например, только программы для детей с
 *       ОВЗ, если выбран такой фильтр);</li>
 *   <li>суммирует показатели по всем учреждениям и хранит разбивку по
 *       каждому отдельно.</li>
 * </ol>
 *
 * <p>Раньше показатели брались целиком со страницы учреждения
 * ({@code /org/{id}/}), что включало вообще все программы этого
 * учреждения — из-за этого счётчики "программ" и "учреждений" были
 * правильно отфильтрованы, а суммы зачислений — нет. Теперь суммы
 * считаются по отдельным страницам отфильтрованных программ.</p>
 */
public class ProgramAnalysisService {
    
    private static final Logger logger = AppLogger.getLogger();
    private static final int MAX_PAGES = 500; // защита от зацикливания при неожиданной разметке пагинации
    
    /**
     * Обратный вызов для отображения прогресса в UI.
     */
    public interface ProgressListener {
        void onProgress(String message);
    }
    
    private final ProgramSearchService searchService;
    private final String baseUrl;
    
    private ProgramAnalysisService(Builder builder) {
        this.searchService = builder.searchService;
        this.baseUrl = builder.baseUrl;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Выполнить полный анализ.
     *
     * @param queryString query-строка фильтров (без ведущего "?"), как
     *                    возвращает {@code FilterSession.buildQueryString()}
     * @param listener    необязательный слушатель прогресса
     * @param isCancelled проверяется перед обработкой каждой страницы
     *                    списка программ и каждого учреждения — если
     *                    возвращает true, анализ немедленно останавливается
     *                    и возвращается результат с {@code isCancelled() == true}
     *                    и тем, что успело накопиться к этому моменту
     */
    public AnalysisResult analyze(String queryString, ProgressListener listener,
                                   java.util.function.BooleanSupplier isCancelled) {
        List<Program> allPrograms = new ArrayList<>();
        
        String nextRelativeUrl = "/programs/" + (queryString != null && !queryString.isEmpty() ? "?" + queryString : "");
        int pageCount = 0;
        
        while (nextRelativeUrl != null && pageCount < MAX_PAGES) {
            if (isCancelled.getAsBoolean()) {
                return cancelledResult(allPrograms.size(), List.of());
            }
            
            pageCount++;
            report(listener, "Загрузка списка программ, страница " + pageCount + "...");
            
            String fullUrl = resolveUrl(nextRelativeUrl);
            SearchResult pageResult = searchService.search(fullUrl);
            
            if (!pageResult.isSuccess()) {
                logger.error(LOG_PAGE_FETCH_ERROR, pageCount, pageResult.getErrorMessage().orElse(""));
                return AnalysisResult.builder()
                        .success(false)
                        .errorMessage("Не удалось загрузить список программ (страница " + pageCount + "): "
                                + pageResult.getErrorMessage().orElse("неизвестная ошибка"))
                        .build();
            }
            
            ProgramParser.ParseResult parseResult = ProgramParser.parse(pageResult.getHtml());
            if (!parseResult.isSuccess()) {
                logger.error(LOG_PARSE_ERROR, pageCount, parseResult.getErrorMessage().orElse(""));
                return AnalysisResult.builder()
                        .success(false)
                        .errorMessage("Не удалось разобрать список программ (страница " + pageCount + "): "
                                + parseResult.getErrorMessage().orElse("неизвестная ошибка"))
                        .build();
            }
            
            allPrograms.addAll(parseResult.getPrograms());
            nextRelativeUrl = parseResult.hasNextPage() ? parseResult.getNextPageUrl().orElse(null) : null;
        }
        
        logger.info(LOG_PROGRAMS_TOTAL, allPrograms.size(), pageCount);
        
        // Уникальные учреждения по ID (одно учреждение часто ведёт
        // несколько программ из списка), и заодно — какие именно
        // отфильтрованные программы принадлежат каждому учреждению
        // (это и есть основа для правильного подсчёта зачислений).
        Map<String, Organization> uniqueOrganizations = new LinkedHashMap<>();
        Map<String, List<Program>> programsByOrgId = new LinkedHashMap<>();
        for (Program program : allPrograms) {
            program.getOrganization().ifPresent(org ->
                    org.getId().ifPresent(id -> {
                        uniqueOrganizations.putIfAbsent(id, org);
                        programsByOrgId.computeIfAbsent(id, k -> new ArrayList<>()).add(program);
                    }));
        }
        
        logger.info(LOG_INSTITUTIONS_TOTAL, uniqueOrganizations.size());
        
        List<InstitutionAnalysis> institutions = new ArrayList<>();
        Map<String, Integer> totals = new LinkedHashMap<>();
        
        int index = 0;
        int total = uniqueOrganizations.size();
        for (Organization org : uniqueOrganizations.values()) {
            if (isCancelled.getAsBoolean()) {
                return cancelledResult(allPrograms.size(), institutions);
            }
            
            index++;
            String orgName = org.getName() != null ? org.getName() : org.getId().orElse("?");
            report(listener, "Учреждение " + index + " из " + total + ": " + orgName);
            
            List<Program> orgPrograms = programsByOrgId.getOrDefault(
                    org.getId().orElse(""), List.of());
            InstitutionAnalysis analysis = analyzeInstitution(org, orgPrograms, listener, isCancelled);
            institutions.add(analysis);
            
            if (analysis.isSuccess()) {
                for (Map.Entry<String, Integer> entry : analysis.getStats().entrySet()) {
                    totals.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }
        
        return AnalysisResult.builder()
                .success(true)
                .totalPrograms(allPrograms.size())
                .totalInstitutions(uniqueOrganizations.size())
                .totals(totals)
                .institutions(institutions)
                .build();
    }
    
    /**
     * Строит результат для случая, когда пользователь нажал "Отменить" —
     * помечен отдельным флагом (не как ошибка), с суммами по тому, что
     * успело обработаться к моменту отмены.
     */
    private AnalysisResult cancelledResult(int totalPrograms, List<InstitutionAnalysis> institutionsSoFar) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        for (InstitutionAnalysis institution : institutionsSoFar) {
            if (institution.isSuccess()) {
                for (Map.Entry<String, Integer> entry : institution.getStats().entrySet()) {
                    totals.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }
        
        logger.info(LOG_CANCELLED, institutionsSoFar.size());
        
        return AnalysisResult.builder()
                .success(true)
                .cancelled(true)
                .totalPrograms(totalPrograms)
                .totalInstitutions(institutionsSoFar.size())
                .totals(totals)
                .institutions(institutionsSoFar)
                .build();
    }
    
    /**
     * Считает показатели учреждения ТОЛЬКО по тем его программам, что
     * вошли в отфильтрованный список — заходит на страницу каждой такой
     * программы отдельно и суммирует их ".statistic"-показатели. Это и
     * даёт корректные, отфильтрованные суммы (например, зачисления
     * только по программам для детей с ОВЗ), а не показатели учреждения
     * целиком.
     */
    private InstitutionAnalysis analyzeInstitution(Organization org, List<Program> orgPrograms,
                                                     ProgressListener listener,
                                                     java.util.function.BooleanSupplier isCancelled) {
        String orgId = org.getId().orElse("");
        String orgName = org.getName();
        String orgRelativeUrl = org.getUrl().orElse(null);
        String orgFullUrl = orgRelativeUrl != null ? resolveUrl(orgRelativeUrl) : null;
        
        if (orgPrograms.isEmpty()) {
            return InstitutionAnalysis.builder()
                    .organizationId(orgId)
                    .organizationName(orgName)
                    .organizationUrl(orgFullUrl)
                    .success(false)
                    .errorMessage("Нет ни одной программы этого учреждения в отфильтрованном списке")
                    .build();
        }
        
        Map<String, Integer> stats = new LinkedHashMap<>();
        int failedPrograms = 0;
        
        for (Program program : orgPrograms) {
            if (isCancelled.getAsBoolean()) {
                break;
            }
            
            String programRelativeUrl = program.getUrl().orElse(null);
            if (programRelativeUrl == null) {
                failedPrograms++;
                continue;
            }
            
            String programFullUrl = resolveUrl(programRelativeUrl);
            SearchResult programResult = searchService.search(programFullUrl);
            
            if (!programResult.isSuccess()) {
                logger.warn(LOG_PROGRAM_FETCH_ERROR, programFullUrl, programResult.getErrorMessage().orElse(""));
                failedPrograms++;
                continue;
            }
            
            ProgramDetailParser.ParseResult detailResult = ProgramDetailParser.parse(programResult.getHtml());
            if (!detailResult.isSuccess()) {
                logger.warn(LOG_PROGRAM_PARSE_ERROR, programFullUrl, detailResult.getErrorMessage().orElse(""));
                failedPrograms++;
                continue;
            }
            
            for (Map.Entry<String, Integer> entry : detailResult.getStats().entrySet()) {
                stats.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        
        if (failedPrograms > 0) {
            report(listener, "  (" + orgName + ": не удалось получить показатели по "
                    + failedPrograms + " из " + orgPrograms.size() + " программ)");
        }
        
        return InstitutionAnalysis.builder()
                .organizationId(orgId)
                .organizationName(orgName)
                .organizationUrl(orgFullUrl)
                .success(true)
                .stats(stats)
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
    
    // Константы для логирования
    private static final String LOG_PAGE_FETCH_ERROR = "Ошибка загрузки страницы {} списка программ: {}";
    private static final String LOG_PARSE_ERROR = "Ошибка разбора страницы {} списка программ: {}";
    private static final String LOG_PROGRAMS_TOTAL = "Всего найдено программ: {} (страниц: {})";
    private static final String LOG_INSTITUTIONS_TOTAL = "Уникальных учреждений: {}";
    private static final String LOG_CANCELLED = "Анализ отменён пользователем. Обработано учреждений: {}";
    private static final String LOG_PROGRAM_FETCH_ERROR = "Ошибка загрузки страницы программы {}: {}";
    private static final String LOG_PROGRAM_PARSE_ERROR = "Ошибка разбора страницы программы {}: {}";
    
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
        
        public ProgramAnalysisService build() {
            if (searchService == null) {
                searchService = ProgramSearchService.builder().build();
            }
            return new ProgramAnalysisService(this);
        }
    }
}
