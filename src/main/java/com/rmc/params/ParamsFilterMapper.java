package com.rmc.params;

import com.rmc.filters.builder.FilterQueryBuilder;
import com.rmc.filters.model.FilterGroup;
import com.rmc.filters.parser.FilterDefinition;
import com.rmc.filters.parser.FilterOption;
import com.rmc.filters.session.FilterSession;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Склеивает формы фильтров трёх страниц раздела "ПАРАМЕТРЫ" в один общий
 * набор фильтров и обратно — раскладывает выбранные значения в отдельный
 * запрос для каждой страницы.
 *
 * <p>Поля разных страниц сводятся по подписи ("Муниципалитет", "Год"):
 * имена полей на страницах могут отличаться (например, муниципалитет —
 * {@code territory__in} на одной странице и
 * {@code cert_group__territory_id__in} на другой), поэтому при запросе
 * каждая страница получает значение под своим именем поля. Варианты
 * выбора сопоставляются по подписи, а не по номеру, чтобы не зависеть от
 * совпадения id между страницами. Поле, которого на странице нет
 * ("Принцип сертификата", "Группы сертификатов"), к этой странице просто
 * не применяется.</p>
 */
public final class ParamsFilterMapper {

    private static final class Unified {
        final String name;
        final String caption;
        final FilterDefinition first;
        final List<FilterOption> options = new ArrayList<>();
        final Set<String> optionLabels = new LinkedHashSet<>();
        final Map<ParamsPage, FilterDefinition> sources = new EnumMap<>(ParamsPage.class);
        boolean multiple;

        Unified(FilterDefinition first) {
            this.name = first.getName();
            this.caption = first.getCaption() != null && !first.getCaption().isBlank()
                    ? first.getCaption().trim() : first.getName();
            this.first = first;
        }

        void addOptions(FilterDefinition def) {
            for (FilterOption option : def.getOptions()) {
                if (option.getValue() == null || option.getValue().isEmpty()) {
                    continue; // пустой "--------" вариант не нужен
                }
                if (optionLabels.add(normalize(option.getLabel()))) {
                    options.add(option);
                }
            }
        }
    }

    private final Map<String, Unified> unifiedByName = new LinkedHashMap<>();
    private final List<FilterGroup> groups;

    private ParamsFilterMapper(Map<String, Unified> unified) {
        this.unifiedByName.putAll(unified);
        List<FilterDefinition> definitions = new ArrayList<>();
        for (Unified u : unified.values()) {
            String caption = u.caption;
            if (u.sources.size() < ParamsPage.values().length) {
                List<String> pages = new ArrayList<>();
                for (ParamsPage page : u.sources.keySet()) {
                    pages.add(page.getShortName());
                }
                caption = caption + " (только: " + String.join(", ", pages) + ")";
            }
            definitions.add(FilterDefinition.builder()
                    .name(u.name)
                    .caption(caption)
                    .type(u.multiple ? com.rmc.filters.parser.FilterType.SELECT_MULTIPLE : u.first.getType())
                    .multiple(u.multiple)
                    .options(u.options)
                    .placeholder(u.first.getPlaceholder())
                    .id(u.first.getId())
                    .build());
        }
        this.groups = List.of(new FilterGroup("params", "Фильтры", definitions));
    }

    /**
     * @param groupsByPage разобранные формы фильтров каждой страницы
     */
    public static ParamsFilterMapper build(Map<ParamsPage, List<FilterGroup>> groupsByPage) {
        Map<String, Unified> byKey = new LinkedHashMap<>();
        for (ParamsPage page : ParamsPage.values()) {
            List<FilterGroup> pageGroups = groupsByPage.get(page);
            if (pageGroups == null) {
                continue;
            }
            for (FilterGroup group : pageGroups) {
                for (FilterDefinition def : group.getFilters()) {
                    if (def.getName() == null || def.getName().isEmpty()) {
                        continue;
                    }
                    if (def.getType() != null && def.getType().name().equals("HIDDEN")) {
                        continue;
                    }
                    String caption = def.getCaption() != null && !def.getCaption().isBlank()
                            ? def.getCaption() : def.getName();
                    String key = normalize(caption);
                    Unified unified = byKey.computeIfAbsent(key, k -> new Unified(def));
                    unified.sources.put(page, def);
                    unified.multiple |= def.isMultiple();
                    unified.addOptions(def);
                }
            }
        }

        // Имена общих полей должны быть уникальными (по ним работает FilterSession).
        Map<String, Unified> byName = new LinkedHashMap<>();
        for (Unified unified : byKey.values()) {
            byName.putIfAbsent(unified.name, unified);
        }
        return new ParamsFilterMapper(byName);
    }

    /** @return общий набор фильтров для экрана выбора (одна группа) */
    public List<FilterGroup> getUnifiedGroups() {
        return groups;
    }

    /**
     * Раскладывает выбранные в общей сессии значения в query-строку для
     * каждой страницы (без ведущего "?"; пустая строка — без фильтров).
     */
    public Map<ParamsPage, String> buildQueries(FilterSession session) {
        Map<ParamsPage, FilterQueryBuilder> builders = new EnumMap<>(ParamsPage.class);
        for (ParamsPage page : ParamsPage.values()) {
            builders.put(page, new FilterQueryBuilder());
        }

        for (Unified unified : unifiedByName.values()) {
            List<String> selected = new ArrayList<>();
            String single = session.getValue(unified.name);
            if (single != null && !single.isEmpty()) {
                selected.add(single);
            }
            selected.addAll(session.getMultiValues(unified.name));
            if (selected.isEmpty()) {
                continue;
            }

            for (Map.Entry<ParamsPage, FilterDefinition> source : unified.sources.entrySet()) {
                FilterDefinition pageDef = source.getValue();
                List<String> pageValues = new ArrayList<>();
                for (String value : selected) {
                    String translated = translate(unified, pageDef, value);
                    if (translated != null) {
                        pageValues.add(translated);
                    }
                }
                if (pageValues.isEmpty()) {
                    continue;
                }
                FilterQueryBuilder builder = builders.get(source.getKey());
                if (pageValues.size() == 1) {
                    builder.addValue(pageDef.getName(), pageValues.get(0));
                } else {
                    builder.addValues(pageDef.getName(), pageValues);
                }
            }
        }

        Map<ParamsPage, String> queries = new EnumMap<>(ParamsPage.class);
        builders.forEach((page, builder) -> queries.put(page, builder.toQueryString()));
        return queries;
    }

    /**
     * Значение общего фильтра -> значение того же варианта на конкретной
     * странице: через подпись варианта. Если у поля этой страницы нет
     * списка вариантов (не select) или варианта с такой подписью нет —
     * значение отдаётся как есть, если списка нет, и пропускается, если
     * список есть, а такого варианта в нём нет.
     */
    private static String translate(Unified unified, FilterDefinition pageDef, String value) {
        if (pageDef.getOptions().isEmpty()) {
            return value;
        }
        String label = null;
        for (FilterOption option : unified.options) {
            if (value.equals(option.getValue())) {
                label = option.getLabel();
                break;
            }
        }
        if (label == null) {
            return value;
        }
        String wanted = normalize(label);
        for (FilterOption option : pageDef.getOptions()) {
            if (option.getValue() != null && !option.getValue().isEmpty()
                    && wanted.equals(normalize(option.getLabel()))) {
                return option.getValue();
            }
        }
        return null;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
