package com.rmc.params;

import java.util.regex.Pattern;

/**
 * Три страницы сайта раздела "ПАРАМЕТРЫ", данные которых раздел
 * "Параметры РМЦ" собирает вместе. Карточки на всех трёх одинаковой
 * структуры, различается только формулировка заголовка (из него и берутся
 * территория и год, по которым данные трёх страниц связываются друг с
 * другом).
 */
public enum ParamsPage {

    /** /params/terr/ — "Параметры муниципалитета". */
    TERR("params/terr/", "Параметры муниципалитета", "муниципалитет", "Муниципалитет",
            Pattern.compile("^Параметры территории\\s+(?<terr>.+?)\\s+для\\s+(?<year>\\d{4})\\s*$")),

    /** /params/certgroup/ — "Параметры групп сертификатов". */
    CERT_GROUP("params/certgroup/", "Параметры групп сертификатов", "группы сертификатов", "Группы сертификатов",
            Pattern.compile("^Параметры группы сертификата\\s+[\"«“](?<group>.*?)[\"»”]\\s+(?<terr>.+?)\\s+для\\s+(?<year>\\d{4})\\s*$")),

    /** /params/certpotentialbalance/ — "Потенциальный баланс сертификатов". */
    CERT_BALANCE("params/certpotentialbalance/", "Потенциальный баланс сертификатов", "потенциальный баланс", "Потенц. баланс",
            Pattern.compile("^Настройки потенциального баланса\\s+(?<terr>.+?)\\s+на\\s+(?<year>\\d{4})\\s*$"));

    private final String path;
    private final String title;
    private final String shortName;
    private final String sheetName;
    private final Pattern titlePattern;

    ParamsPage(String path, String title, String shortName, String sheetName, Pattern titlePattern) {
        this.path = path;
        this.title = title;
        this.shortName = shortName;
        this.sheetName = sheetName;
        this.titlePattern = titlePattern;
    }

    /** Путь страницы относительно корня сайта, без ведущего "/" (с завершающим "/"). */
    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    /** Короткое название для пометок вроде "только: группы сертификатов". */
    public String getShortName() {
        return shortName;
    }

    public String getSheetName() {
        return sheetName;
    }

    Pattern getTitlePattern() {
        return titlePattern;
    }
}
