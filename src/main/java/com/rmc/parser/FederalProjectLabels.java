package com.rmc.parser;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Распознаёт отметки программ по справочнику проектов текущей выдачи реестра. */
public final class FederalProjectLabels {
    private final Set<String> options = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    void readFilterOptions(Document page) {
        for (Element option : page.select("select[name^=federal_project] option")) {
            String name = option.text();
            if (isProjectName(name)) {
                options.add(name);
            }
        }
    }

    List<String> readFrom(Element program) {
        Set<String> names = new LinkedHashSet<>();
        for (Element label : program.select("div.left.ui.label")) {
            // В меню «Изменить на» перечислены варианты для выбора, а в фильтре —
            // условия поиска. Обе области могут находиться рядом с отметкой.
            if (label.closest(".menu, .popup, #modern_filter_form, form[class*=filter], [hidden], template") != null) {
                continue;
            }
            String name = label.text();
            if (isProjectName(name) && (options.isEmpty() || options.contains(name))) {
                names.add(name);
            }
        }
        return List.copyOf(names);
    }

    private static boolean isProjectName(String name) {
        return !name.isBlank() && !name.equalsIgnoreCase("Нет") && !name.matches("[-–—]+");
    }
}
