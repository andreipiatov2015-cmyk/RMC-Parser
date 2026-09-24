package com.rmc.search.service;

import com.rmc.logging.AppLogger;
import com.rmc.parser.OrganizationStatsParser;
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
 *   <li>для каждого учреждения считает ОБА набора показателей сразу:
 *       <ul>
 *         <li><b>по фильтру</b> — заходит на страницу КАЖДОЙ программы
 *             учреждения, вошедшей в отфильтрованный список, и суммирует
 *             их показатели (например, только программы для детей с ОВЗ,
 *             если выбран такой фильтр), плюс считает сколько таких
 *             программ у учреждения нашлось;</li>
 *         <li><b>по учреждению целиком</b> — заходит на страницу самого
 *             учреждения ({@code /org/{id}/}) и берёт показатели оттуда,
 *             без учёта фильтра, как это было в самой первой версии
 *             программы;</li>
 *       </ul>
 *   </li>
 *   <li>суммирует оба набора показателей по всем учреждениям и хранит
 *       разбивку по каждому отдельно.</li>
 * </ol>
 *
 * <p>Какой из двух наборов показывать пользователю на экране — решает UI
 * (список галочек в результатах), здесь же всегда считаются оба, чтобы
 * переключение галочек не требовало повторного обхода сайта.</p>
 */
public class ProgramAnalysisService {
    
    private static final Logger logger = AppLogger.getLogger();
    private static final int MAX_PAGES = 500; // защита от зацикливания при неожиданной разметке пагинации
    
    /**
     * Обратный вызов для отображения прогресса в UI. {@code current}/
     * {@code total} — номер текущего учреждения и их общее число (для
     * полосы прогресса с процентом); {@code total <= 0} означает, что
     * общее число шагов на этом этапе ещё не известно (например, во
     * время загрузки страниц списка программ, до того как стал известен
     * список учреждений).
     */
    public interface ProgressListener {
        void onProgress(String message, int current, int total);
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
            report(listener, "Загрузка списка программ, страница " + pageCount + "...", 0, 0);
            
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
        // отфильтрованные программы принадлежат каждому учреждению.
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
        Map<String, Integer> filteredTotals = new LinkedHashMap<>();
        Map<String, Integer> overallTotals = new LinkedHashMap<>();
        
        int index = 0;
        int total = uniqueOrganizations.size();
        for (Organization org : uniqueOrganizations.values()) {
            if (isCancelled.getAsBoolean()) {
                return cancelledResult(allPrograms.size(), institutions);
            }
            
            index++;
            String orgName = org.getName() != null ? org.getName() : org.getId().orElse("?");
            report(listener, "Учреждение " + index + " из " + total + ": " + orgName, index, total);
            
            List<Program> orgPrograms = programsByOrgId.getOrDefault(
                    org.getId().orElse(""), List.of());
            InstitutionAnalysis analysis = analyzeInstitution(org, orgPrograms, listener, isCancelled, index, total);
            institutions.add(analysis);
            
            if (analysis.isSuccess()) {
                for (Map.Entry<String, Integer> entry : analysis.getFilteredStats().entrySet()) {
                    filteredTotals.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
                for (Map.Entry<String, Integer> entry : analysis.getOverallStats().entrySet()) {
                    overallTotals.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }
        
        return AnalysisResult.builder()
                .success(true)
                .totalPrograms(allPrograms.size())
                .totalInstitutions(uniqueOrganizations.size())
                .filteredTotals(filteredTotals)
                .overallTotals(overallTotals)
                .institutions(institutions)
                .build();
    }
    
    /**
     * Строит результат для случая, когда пользователь нажал "Отменить" —
     * помечен отдельным флагом (не как ошибка), с суммами по тому, что
     * успело обработаться к моменту отмены.
     */
    private AnalysisResult cancelledResult(int totalPrograms, List<InstitutionAnalysis> institutionsSoFar) {
        Map<String, Integer> filteredTotals = new LinkedHashMap<>();
        Map<String, Integer> overallTotals = new LinkedHashMap<>();
        for (InstitutionAnalysis institution : institutionsSoFar) {
            if (institution.isSuccess()) {
                for (Map.Entry<String, Integer> entry : institution.getFilteredStats().entrySet()) {
                    filteredTotals.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
                for (Map.Entry<String, Integer> entry : institution.getOverallStats().entrySet()) {
                    overallTotals.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }
        
        logger.info(LOG_CANCELLED, institutionsSoFar.size());
        
        return AnalysisResult.builder()
                .success(true)
                .cancelled(true)
                .totalPrograms(totalPrograms)
                .totalInstitutions(institutionsSoFar.size())
                .filteredTotals(filteredTotals)
                .overallTotals(overallTotals)
                .institutions(institutionsSoFar)
                .build();
    }
    
    /**
     * Считает ОБА набора показателей учреждения:
     * <ul>
     *   <li>по фильтру — заходит на страницу каждой отфильтрованной
     *       программы учреждения отдельно и суммирует их
     *       ".statistic"-показатели, плюс сам count таких программ;</li>
     *   <li>по учреждению целиком — заходит на страницу учреждения и
     *       берёт показатели оттуда, без учёта фильтра.</li>
     * </ul>
     * Если один из источников не удалось получить, это не мешает
     * показать данные из другого — учреждение считается успешно
     * обработанным, если получилось хотя бы что-то одно.
     */
    private InstitutionAnalysis analyzeInstitution(Organization org, List<Program> orgPrograms,
                                                     ProgressListener listener,
                                                     java.util.function.BooleanSupplier isCancelled,
                                                     int index, int total) {
        String orgId = org.getId().orElse("");
        String orgName = org.getName();
        String orgRelativeUrl = org.getUrl().orElse(null);
        String orgFullUrl = orgRelativeUrl != null ? resolveUrl(orgRelativeUrl) : null;
        
        // 1) Показатели по учреждению целиком (старая логика).
        Map<String, Integer> overallStats = new LinkedHashMap<>();
        boolean overallOk = false;
        if (orgFullUrl != null) {
            SearchResult orgResult = searchService.search(orgFullUrl);
            if (orgResult.isSuccess()) {
                OrganizationStatsParser.ParseResult statsResult = OrganizationStatsParser.parse(orgResult.getHtml());
                if (statsResult.isSuccess()) {
                    overallStats.putAll(statsResult.getStats());
                    overallOk = true;
                } else {
                    logger.warn(LOG_ORG_PARSE_ERROR, orgFullUrl, statsResult.getErrorMessage().orElse(""));
                }
            } else {
                logger.warn(LOG_ORG_FETCH_ERROR, orgFullUrl, orgResult.getErrorMessage().orElse(""));
            }
        }
        
        // 2) Показатели только по отфильтрованным программам этого учреждения.
        Map<String, Integer> filteredStats = new LinkedHashMap<>();
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
                filteredStats.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        
        // Синтетический показатель: сколько программ этого учреждения
        // прошло фильтр — считается всегда, даже если показатели с их
        // страниц получить не удалось (сам список программ у нас уже есть).
        if (!orgPrograms.isEmpty()) {
            filteredStats.put(InstitutionAnalysis.FILTERED_PROGRAM_COUNT_LABEL, orgPrograms.size());
        }
        
        if (failedPrograms > 0) {
            report(listener, "  (" + orgName + ": не удалось получить показатели по "
                    + failedPrograms + " из " + orgPrograms.size() + " программ)", index, total);
        }
        
        boolean filteredOk = !filteredStats.isEmpty();
        boolean success = overallOk || filteredOk;
        
        InstitutionAnalysis.Builder resultBuilder = InstitutionAnalysis.builder()
                .organizationId(orgId)
                .organizationName(orgName)
                .organizationUrl(orgFullUrl)
                .filteredStats(filteredStats)
                .overallStats(overallStats)
                .success(success);
        
        if (!success) {
            resultBuilder.errorMessage("Не удалось получить ни показатели учреждения, "
                    + "ни показатели его программ из отфильтрованного списка");
        }
        
        return resultBuilder.build();
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
    
    private void report(ProgressListener listener, String message, int current, int total) {
        logger.info(message);
        if (listener != null) {
            listener.onProgress(message, current, total);
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
    private static final String LOG_ORG_FETCH_ERROR = "Ошибка загрузки страницы учреждения {}: {}";
    private static final String LOG_ORG_PARSE_ERROR = "Ошибка разбора страницы учреждения {}: {}";
    
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
