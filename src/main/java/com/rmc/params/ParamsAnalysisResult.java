package com.rmc.params;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Итог загрузки раздела "Параметры РМЦ": карточки со всех трёх страниц и
 * их группировка по паре "территория + год".
 */
public class ParamsAnalysisResult {

    /** Все карточки одной территории за один год — с трёх страниц. */
    public static final class TerritoryYear {
        private final String territory;
        private final String year;
        private final Map<ParamsPage, List<ParamsCard>> cards = new EnumMap<>(ParamsPage.class);

        TerritoryYear(String territory, String year) {
            this.territory = territory;
            this.year = year;
            for (ParamsPage page : ParamsPage.values()) {
                cards.put(page, new ArrayList<>());
            }
        }

        public String getTerritory() {
            return territory;
        }

        public String getYear() {
            return year;
        }

        public List<ParamsCard> getCards(ParamsPage page) {
            return cards.get(page);
        }

        public int getTotalCards() {
            return cards.values().stream().mapToInt(List::size).sum();
        }
    }

    private final boolean success;
    private final boolean cancelled;
    private final String errorMessage;
    private final Map<ParamsPage, List<ParamsCard>> cardsByPage;
    private final List<TerritoryYear> territoryYears;

    private ParamsAnalysisResult(boolean success, boolean cancelled, String errorMessage,
                                 Map<ParamsPage, List<ParamsCard>> cardsByPage) {
        this.success = success;
        this.cancelled = cancelled;
        this.errorMessage = errorMessage;
        this.cardsByPage = cardsByPage;
        this.territoryYears = group(cardsByPage);
    }

    public static ParamsAnalysisResult success(Map<ParamsPage, List<ParamsCard>> cards, boolean cancelled) {
        return new ParamsAnalysisResult(true, cancelled, null, cards);
    }

    public static ParamsAnalysisResult failure(String errorMessage) {
        return new ParamsAnalysisResult(false, false, errorMessage, new EnumMap<>(ParamsPage.class));
    }

    private static List<TerritoryYear> group(Map<ParamsPage, List<ParamsCard>> cardsByPage) {
        Map<String, TerritoryYear> groups = new LinkedHashMap<>();
        for (ParamsPage page : ParamsPage.values()) {
            for (ParamsCard card : cardsByPage.getOrDefault(page, List.of())) {
                String territory = card.getTerritory().trim().replaceAll("\\s+", " ");
                String key = territory + "\u0000" + card.getYear();
                groups.computeIfAbsent(key, k -> new TerritoryYear(territory, card.getYear()))
                        .getCards(page).add(card);
            }
        }
        List<TerritoryYear> result = new ArrayList<>(groups.values());
        result.sort(Comparator.comparing(TerritoryYear::getTerritory).thenComparing(TerritoryYear::getYear));
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    public List<ParamsCard> getCards(ParamsPage page) {
        return cardsByPage.getOrDefault(page, List.of());
    }

    public List<TerritoryYear> getTerritoryYears() {
        return territoryYears;
    }

    public int getTotalCards() {
        return cardsByPage.values().stream().mapToInt(List::size).sum();
    }
}
