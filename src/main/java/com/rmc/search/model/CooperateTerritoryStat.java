package com.rmc.search.model;

import com.rmc.parser.model.Cooperate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сводка по заявкам на соглашения одной территории (муниципалитет
 * учреждения) за один период (год): сколько всего заявок, сколько из них
 * в каждом статусе и суммарные часы.
 */
public class CooperateTerritoryStat {

    public static final String NO_TERRITORY = "(территория не указана)";
    public static final String NO_PERIOD = "(период не указан)";

    private final String territory;
    private final String period;
    private int total;
    private int newCount;
    private int confirmedCount;
    private int rejectedCount;
    private int unknownCount;
    private long maxHours;
    private long confirmedHours;

    private CooperateTerritoryStat(String territory, String period) {
        this.territory = territory;
        this.period = period;
    }

    public String getTerritory() {
        return territory;
    }

    public String getPeriod() {
        return period;
    }

    public int getTotal() {
        return total;
    }

    public int getNewCount() {
        return newCount;
    }

    public int getConfirmedCount() {
        return confirmedCount;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public int getUnknownCount() {
        return unknownCount;
    }

    public long getMaxHours() {
        return maxHours;
    }

    public long getConfirmedHours() {
        return confirmedHours;
    }

    private void add(Cooperate cooperate) {
        total++;
        switch (cooperate.getStatus()) {
            case NEW -> newCount++;
            case CONFIRMED -> confirmedCount++;
            case REJECTED -> rejectedCount++;
            default -> unknownCount++;
        }
        maxHours += parseHours(cooperate.getMaxHours().orElse(null));
        confirmedHours += parseHours(cooperate.getConfirmedHours().orElse(null));
    }

    private static long parseHours(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Группирует заявки по (территория учреждения, период). Если у
     * учреждения территория не указана — берётся территория
     * уполномоченного органа. Результат отсортирован по территории, затем
     * по периоду.
     */
    public static List<CooperateTerritoryStat> compute(List<Cooperate> cooperates) {
        Map<String, CooperateTerritoryStat> groups = new LinkedHashMap<>();
        for (Cooperate cooperate : cooperates) {
            String territory = cooperate.getSchoolTerritory()
                    .filter(t -> !t.isBlank())
                    .or(() -> cooperate.getPayerTerritory().filter(t -> !t.isBlank()))
                    .orElse(NO_TERRITORY);
            String period = cooperate.getPeriod().filter(p -> !p.isBlank()).orElse(NO_PERIOD);
            groups.computeIfAbsent(territory + "\u0000" + period,
                    k -> new CooperateTerritoryStat(territory, period)).add(cooperate);
        }
        List<CooperateTerritoryStat> result = new ArrayList<>(groups.values());
        result.sort(Comparator.comparing(CooperateTerritoryStat::getTerritory)
                .thenComparing(CooperateTerritoryStat::getPeriod));
        return result;
    }
}
