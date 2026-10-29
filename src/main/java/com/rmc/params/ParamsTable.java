package com.rmc.params;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Сводная таблица раздела "Параметры РМЦ": строка = муниципалитет + год
 * (+ группа сертификата, если их несколько), колонки собраны из трёх
 * страниц сайта по подписям показателей.
 */
public final class ParamsTable {

    /** Колонка-показатель: к какой группе относится, как называется и с какой страницы берётся. */
    public record Column(String group, String label, ParamsPage page) {
    }

    /** Строка таблицы: фиксированные поля + значения показателей в порядке {@link #COLUMNS}. */
    public record Row(String territory, String year, String groupName, List<String> values) {
    }

    private static final String EXPENSES = "Расходы";
    private static final String COEFFICIENTS = "Коэффициенты";
    private static final String KIT = "Базовая стоимость восполнения комплекта средств обучения";
    private static final String CERTS = "Сертификаты и дети";
    private static final String GROUPS = "Группы сертификатов";
    private static final String BALANCE = "Потенциальный баланс";

    public static final List<Column> COLUMNS = buildColumns();

    private final List<Row> rows;

    private ParamsTable(List<Row> rows) {
        this.rows = rows;
    }

    public List<Row> getRows() {
        return rows;
    }

    private static List<Column> buildColumns() {
        List<Column> c = new ArrayList<>();
        ParamsPage t = ParamsPage.TERR;
        c.add(new Column(EXPENSES, "Базовая потребность в приобретении услуг (кроме ПК)", t));
        c.add(new Column(EXPENSES, "Средние расходы на обеспечение повышения квалификации одного педагогического работника", t));
        c.add(new Column(EXPENSES, "Средние расходы на обеспечение допуска к работе одного педагогического работника (включая медосмотр)", t));
        c.add(new Column(EXPENSES, "Средняя заработная плата педагогических работников в месяц на период", t));
        c.add(new Column(COEFFICIENTS, "Коэффициент привлечения дополнительных педагогических работников", t));
        c.add(new Column(COEFFICIENTS, "Коэффициент увеличения на прочий персонал", t));
        c.add(new Column(COEFFICIENTS, "Коэффициент отпускных", t));
        c.add(new Column(COEFFICIENTS, "Полезное использование помещений в неделю, часов", t));
        c.add(new Column(COEFFICIENTS, "Среднее количество ставок на одного педагога", t));
        for (String s : new String[]{"Техническая (робототехника)", "Техническая (иная)", "Естественно-научная",
                "Физкультурно-спортивная", "Художественная", "Туристско-краеведческая", "Социально-гуманитарная"}) {
            c.add(new Column(KIT, s, t));
        }
        c.add(new Column(CERTS, "Число действующих сертификатов", t));
        c.add(new Column(CERTS, "Детей в возрасте от 5-ти до 18-ти лет", t));
        ParamsPage g = ParamsPage.CERT_GROUP;
        for (String s : new String[]{"Возможность", "Номинал (ч.)", "Макс. объём (ч.)",
                "Макс. списания средств в 1 мес. (ч.)", "Лимиты УЧЁТ П/З/И", "Лимиты ПФ П/З/И"}) {
            c.add(new Column(GROUPS, s, g));
        }
        for (String s : new String[]{"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август",
                "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"}) {
            c.add(new Column(BALANCE, s, ParamsPage.CERT_BALANCE));
        }
        return List.copyOf(c);
    }

    public static ParamsTable from(ParamsAnalysisResult result) {
        List<Row> rows = new ArrayList<>();
        for (ParamsAnalysisResult.TerritoryYear ty : result.getTerritoryYears()) {
            Map<String, String> terr = valuesOf(ty.getCards(ParamsPage.TERR));
            Map<String, String> bal = valuesOf(ty.getCards(ParamsPage.CERT_BALANCE));
            List<ParamsCard> groups = ty.getCards(ParamsPage.CERT_GROUP);
            if (groups.isEmpty()) {
                rows.add(makeRow(ty, "", terr, Map.of(), bal));
            } else {
                for (ParamsCard group : groups) {
                    rows.add(makeRow(ty, group.getGroupName().orElse(""), terr,
                            valuesOf(List.of(group)), bal));
                }
            }
        }
        return new ParamsTable(rows);
    }

    private static Row makeRow(ParamsAnalysisResult.TerritoryYear ty, String groupName,
                               Map<String, String> terr, Map<String, String> group, Map<String, String> bal) {
        List<String> values = new ArrayList<>();
        for (Column col : COLUMNS) {
            Map<String, String> source = switch (col.page()) {
                case TERR -> terr;
                case CERT_GROUP -> group;
                case CERT_BALANCE -> bal;
            };
            values.add(source.getOrDefault(key(col.label()), ""));
        }
        return new Row(ty.getTerritory(), ty.getYear(), groupName, values);
    }

    private static Map<String, String> valuesOf(List<ParamsCard> cards) {
        Map<String, String> map = new HashMap<>();
        for (ParamsCard card : cards) {
            for (ParamsCard.Section section : card.getSections()) {
                for (ParamsCard.Row r : section.rows()) {
                    map.putIfAbsent(key(r.label()), r.value());
                }
            }
        }
        return map;
    }

    private static String key(String label) {
        return label.replaceAll("[\\s\\u00A0\\u202F]+", " ").trim().toLowerCase(Locale.ROOT);
    }
}
