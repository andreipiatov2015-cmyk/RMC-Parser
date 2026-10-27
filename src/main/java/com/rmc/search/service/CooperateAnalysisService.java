package com.rmc.search.service;

import com.rmc.logging.AppLogger;
import com.rmc.parser.CooperateParser;
import com.rmc.parser.model.Cooperate;
import com.rmc.search.model.CooperateAnalysisResult;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Оркестрирует анализ раздела "Соглашения" (/cooperates/): идёт по
 * страницам списка заявок (следуя пагинации), собирает все заявки и
 * считает суммарные показатели по статусам.
 *
 * <p>В отличие от {@link ProgramAnalysisService}, здесь не нужно заходить
 * на отдельные страницы заявок/учреждений — все нужные данные уже есть
 * прямо в таблице списка.</p>
 */
public class CooperateAnalysisService {

    private static final Logger logger = AppLogger.getLogger();
    private static final int MAX_PAGES = 500; // защита от зацикливания при неожиданной разметке пагинации

    /**
     * Обратный вызов для отображения прогресса в UI.
     */
    public interface ProgressListener {
        void onProgress(String message, int current, int total);
    }

    private final ProgramSearchService searchService;
    private final String baseUrl;

    private CooperateAnalysisService(Builder builder) {
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
     *                    списка — если возвращает true, анализ немедленно
     *                    останавливается и возвращается результат с
     *                    {@code isCancelled() == true} и тем, что успело
     *                    накопиться к этому моменту
     */
    public CooperateAnalysisResult analyze(String queryString, ProgressListener listener,
                                            java.util.function.BooleanSupplier isCancelled) {
        List<Cooperate> allCooperates = new ArrayList<>();

        String nextRelativeUrl = "/cooperates/" + (queryString != null && !queryString.isEmpty() ? "?" + queryString : "");
        int pageCount = 0;

        while (nextRelativeUrl != null && pageCount < MAX_PAGES) {
            if (isCancelled.getAsBoolean()) {
                return cancelledResult(allCooperates);
            }

            pageCount++;
            report(listener, "Загрузка списка соглашений, страница " + pageCount + "...", 0, 0);

            String fullUrl = resolveUrl(nextRelativeUrl);
            SearchResult pageResult = searchService.search(fullUrl);

            if (!pageResult.isSuccess()) {
                logger.error(LOG_PAGE_FETCH_ERROR, pageCount, pageResult.getErrorMessage().orElse(""));
                return CooperateAnalysisResult.builder()
                        .success(false)
                        .errorMessage("Не удалось загрузить список соглашений (страница " + pageCount + "): "
                                + pageResult.getErrorMessage().orElse("неизвестная ошибка"))
                        .build();
            }

            CooperateParser.ParseResult parseResult = CooperateParser.parse(pageResult.getHtml());
            if (!parseResult.isSuccess()) {
                logger.error(LOG_PARSE_ERROR, pageCount, parseResult.getErrorMessage().orElse(""));
                return CooperateAnalysisResult.builder()
                        .success(false)
                        .errorMessage("Не удалось разобрать список соглашений (страница " + pageCount + "): "
                                + parseResult.getErrorMessage().orElse("неизвестная ошибка"))
                        .build();
            }

            allCooperates.addAll(parseResult.getCooperates());
            nextRelativeUrl = parseResult.hasNextPage() ? parseResult.getNextPageUrl().orElse(null) : null;

            report(listener, "Загружено заявок: " + allCooperates.size(), pageCount, 0);
        }

        logger.info(LOG_COOPERATES_TOTAL, allCooperates.size(), pageCount);

        return CooperateAnalysisResult.builder()
                .success(true)
                .totalCooperates(allCooperates.size())
                .statusTotals(computeStatusTotals(allCooperates))
                .cooperates(allCooperates)
                .build();
    }

    private CooperateAnalysisResult cancelledResult(List<Cooperate> soFar) {
        logger.info(LOG_CANCELLED, soFar.size());
        return CooperateAnalysisResult.builder()
                .success(true)
                .cancelled(true)
                .totalCooperates(soFar.size())
                .statusTotals(computeStatusTotals(soFar))
                .cooperates(soFar)
                .build();
    }

    private Map<String, Integer> computeStatusTotals(List<Cooperate> cooperates) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        int newCount = 0;
        int confirmedCount = 0;
        int rejectedCount = 0;
        int unknownCount = 0;
        long confirmedHoursSum = 0;

        for (Cooperate cooperate : cooperates) {
            switch (cooperate.getStatus()) {
                case NEW -> newCount++;
                case CONFIRMED -> confirmedCount++;
                case REJECTED -> rejectedCount++;
                default -> unknownCount++;
            }
            cooperate.getConfirmedHours().ifPresent(hours -> {
                try {
                    confirmedHoursSum += Long.parseLong(hours.trim());
                } catch (NumberFormatException ignored) {
                    // неразбираемое значение часов пропускаем, не ломая подсчёт
                }
            });
        }

        if (newCount > 0) {
            totals.put(CooperateAnalysisResult.STATUS_NEW_LABEL, newCount);
        }
        if (confirmedCount > 0) {
            totals.put(CooperateAnalysisResult.STATUS_CONFIRMED_LABEL, confirmedCount);
        }
        if (rejectedCount > 0) {
            totals.put(CooperateAnalysisResult.STATUS_REJECTED_LABEL, rejectedCount);
        }
        if (unknownCount > 0) {
            totals.put(CooperateAnalysisResult.STATUS_UNKNOWN_LABEL, unknownCount);
        }
        if (confirmedHoursSum > 0) {
            totals.put(CooperateAnalysisResult.CONFIRMED_HOURS_TOTAL_LABEL, (int) confirmedHoursSum);
        }

        return totals;
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
    private static final String LOG_PAGE_FETCH_ERROR = "Ошибка загрузки страницы {} списка соглашений: {}";
    private static final String LOG_PARSE_ERROR = "Ошибка разбора страницы {} списка соглашений: {}";
    private static final String LOG_COOPERATES_TOTAL = "Всего найдено соглашений: {} (страниц: {})";
    private static final String LOG_CANCELLED = "Анализ отменён пользователем. Обработано заявок: {}";

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

        public CooperateAnalysisService build() {
            if (searchService == null) {
                searchService = ProgramSearchService.builder().build();
            }
            return new CooperateAnalysisService(this);
        }
    }
}
