package com.rmc.params;

import java.util.List;
import java.util.Optional;

/**
 * Одна карточка со страницы раздела "ПАРАМЕТРЫ" (муниципалитет, группа
 * сертификатов или потенциальный баланс — см. {@link ParamsPage}).
 */
public class ParamsCard {

    /** Одна строка "показатель — значение". */
    public record Row(String label, String value) {
    }

    /** Блок строк под общим заголовком (например, "Расходы"); заголовок может быть пустым. */
    public record Section(String title, List<Row> rows) {
    }

    private final ParamsPage page;
    private final String id;
    private final String url;
    private final String title;
    private final String territory;
    private final String year;
    private final String groupName;
    private final String statusText;
    private final boolean active;
    private final List<Section> sections;

    public ParamsCard(ParamsPage page, String id, String url, String title, String territory, String year,
                      String groupName, String statusText, boolean active, List<Section> sections) {
        this.page = page;
        this.id = id;
        this.url = url;
        this.title = title;
        this.territory = territory;
        this.year = year;
        this.groupName = groupName;
        this.statusText = statusText;
        this.active = active;
        this.sections = List.copyOf(sections);
    }

    public ParamsPage getPage() {
        return page;
    }

    public String getId() {
        return id;
    }

    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    public String getTitle() {
        return title;
    }

    /** Территория из заголовка; если заголовок не удалось разобрать — весь заголовок. */
    public String getTerritory() {
        return territory;
    }

    /** Год из заголовка, либо пустая строка. */
    public String getYear() {
        return year;
    }

    /** Название группы сертификата (только для страницы групп сертификатов). */
    public Optional<String> getGroupName() {
        return Optional.ofNullable(groupName);
    }

    /** Статус карточки — подпись зелёной отметки (например, "Действуют") либо текст о её отсутствии. */
    public String getStatusText() {
        return statusText;
    }

    /** Есть ли на карточке отметка "Действуют" (зелёная галочка в углу). */
    public boolean isActive() {
        return active;
    }

    public List<Section> getSections() {
        return sections;
    }
}
