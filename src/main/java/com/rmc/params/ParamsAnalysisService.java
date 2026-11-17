package com.rmc.params;

import com.rmc.config.ServerConfig;
import com.rmc.logging.AppLogger;
import com.rmc.search.service.ProgramSearchService;
import com.rmc.search.service.SearchResult;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Загружает карточки со всех трёх страниц раздела "ПАРАМЕТРЫ" (каждая со
 * своим запросом фильтров) и собирает их в {@link ParamsAnalysisResult}.
 *
 * <p>Сначала берётся первая страница каждого источника — из неё
 * становится известно общее число страниц и можно честно показывать
 * полосу прогресса; затем дозагружаются остальные. Адреса следующих
 * страниц строятся самим приложением ({@code ?<фильтры>&page=N}), а не
 * берутся из ссылок пагинации, чтобы фильтры гарантированно сохранялись.</p>
 */
public class ParamsAnalysisService {

    private static final Logger logger = AppLogger.getLogger();
    private static final int MAX_PAGES_PER_SOURCE = 200;

    public interface ProgressListener {
        void onProgress(String message, int current, int total);
    }

    private static final class Source {
        final ParamsPage page;
        final String query;
        final List<ParamsCard> cards = new ArrayList<>();
        int totalPages = 1;
        int loadedPages = 0;

        Source(ParamsPage page, String query) {
            this.page = page;
            this.query = query == null ? "" : query;
        }

        boolean hasMore() {
            return loadedPages < totalPages && loadedPages < MAX_PAGES_PER_SOURCE;
        }
    }

    private final java.util.function.Function<String, SearchResult> fetch;

    public ParamsAnalysisService(ProgramSearchService searchService) {
        this(searchService::search);
    }

    ParamsAnalysisService(java.util.function.Function<String, SearchResult> fetch) {
        this.fetch = fetch;
    }

    public ParamsAnalysisResult analyze(Map<ParamsPage, String> queries, ProgressListener listener,
                                        BooleanSupplier isCancelled) {
        List<Source> sources = new ArrayList<>();
        for (ParamsPage page : ParamsPage.values()) {
            sources.add(new Source(page, queries.get(page)));
        }

        // Этап 1: первые страницы всех источников — чтобы узнать общее число страниц.
        for (Source source : sources) {
            if (isCancelled.getAsBoolean()) {
                return cancelled(sources);
            }
            report(listener, source.page.getTitle() + ": страница 1...", 0, 0);
            String error = loadNext(source);
            if (error != null) {
                return ParamsAnalysisResult.failure(error);
            }
        }

        // Этап 2: остальные страницы.
        while (true) {
            int total = sources.stream().mapToInt(s -> s.totalPages).sum();
            int done = sources.stream().mapToInt(s -> s.loadedPages).sum();
            Source next = sources.stream().filter(Source::hasMore).findFirst().orElse(null);
            if (next == null) {
                report(listener, "Загружено карточек: " + sources.stream().mapToInt(s -> s.cards.size()).sum(),
                        total, total);
                break;
            }
            if (isCancelled.getAsBoolean()) {
                return cancelled(sources);
            }
            report(listener, next.page.getTitle() + ": страница " + (next.loadedPages + 1)
                    + " из " + next.totalPages + "...", done, total);
            String error = loadNext(next);
            if (error != null) {
                return ParamsAnalysisResult.failure(error);
            }
        }

        Source territories = sources.stream().filter(source -> source.page == ParamsPage.TERR).findFirst().orElseThrow();
        for (int i = 0; i < territories.cards.size(); i++) {
            if (isCancelled.getAsBoolean()) return cancelled(sources);
            ParamsCard card = territories.cards.get(i);
            report(listener, "Параметры муниципалитета: " + card.getTitle(), i, territories.cards.size());
            try {
                String url = TerritoryPrices.detailUrl(card);
                SearchResult detail = fetch.apply(url);
                if (!detail.isSuccess() || detail.getStatusCode() != 200) {
                    throw new IllegalStateException(detail.getErrorMessage().orElse("HTTP " + detail.getStatusCode()));
                }
                territories.cards.set(i, card.withSection(TerritoryPrices.parse(detail.getHtml())));
            } catch (IllegalArgumentException | IllegalStateException ex) {
                return ParamsAnalysisResult.failure("Карточка «" + card.getTitle() + "»: " + ex.getMessage());
            }
        }
        return ParamsAnalysisResult.success(collect(sources), false);
    }

    /** @return текст ошибки либо null, если страница загружена и разобрана */
    private String loadNext(Source source) {
        int pageNumber = source.loadedPages + 1;
        String url = buildUrl(source, pageNumber);
        SearchResult result = fetch.apply(url);
        if (!result.isSuccess()) {
            logger.error("Ошибка загрузки {} (страница {}): {}", source.page.getTitle(), pageNumber,
                    result.getErrorMessage().orElse(""));
            return "Не удалось загрузить «" + source.page.getTitle() + "» (страница " + pageNumber + "): "
                    + result.getErrorMessage().orElse("неизвестная ошибка");
        }
        if (result.getStatusCode() != 200 || org.jsoup.Jsoup.parse(result.getHtml()).selectFirst("input[type=password]") != null) {
            return "Страница «" + source.page.getTitle() + "» недоступна. Проверьте вход в РМЦ.";
        }
        ParamsCardParser.PageResult parsed = ParamsCardParser.parse(source.page, result.getHtml());
        source.cards.addAll(parsed.cards());
        source.totalPages = Math.max(source.totalPages, parsed.totalPages());
        source.loadedPages = pageNumber;
        return null;
    }

    private static String buildUrl(Source source, int pageNumber) {
        StringBuilder url = new StringBuilder(ServerConfig.BASE_URL).append('/').append(source.page.getPath());
        boolean hasQuery = !source.query.isEmpty();
        if (hasQuery) {
            url.append('?').append(source.query);
        }
        if (pageNumber > 1) {
            url.append(hasQuery ? '&' : '?').append("page=").append(pageNumber);
        }
        return url.toString();
    }

    private static Map<ParamsPage, List<ParamsCard>> collect(List<Source> sources) {
        Map<ParamsPage, List<ParamsCard>> cards = new EnumMap<>(ParamsPage.class);
        for (Source source : sources) {
            cards.put(source.page, source.cards);
        }
        return cards;
    }

    private ParamsAnalysisResult cancelled(List<Source> sources) {
        logger.info("Загрузка раздела «Параметры РМЦ» отменена пользователем");
        return ParamsAnalysisResult.success(collect(sources), true);
    }

    private void report(ProgressListener listener, String message, int current, int total) {
        logger.info(message);
        if (listener != null) {
            listener.onProgress(message, current, total);
        }
    }
}
