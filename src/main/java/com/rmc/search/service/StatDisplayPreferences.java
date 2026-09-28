package com.rmc.search.service;

import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Хранит, какие показатели ("Зачислений по сертификату", "Зачислений по
 * Бюджету" и т.д.) пользователь ОТМЕТИЛ для показа в результатах анализа —
 * настройка переживает перезапуск программы и компьютера (реестр Windows
 * через стандартный Java Preferences API).
 *
 * <p>Хранится именно набор отмеченных показателей, и по умолчанию он
 * пустой: при первом запуске не отмечено ничего, пользователь сам
 * выбирает, что ему нужно (раньше было наоборот — хранился набор скрытых,
 * и по умолчанию показывалось всё). Соответственно, новый показатель,
 * которого раньше не встречалось, тоже по умолчанию не отмечен.</p>
 */
public class StatDisplayPreferences {
    
    private static final Logger logger = AppLogger.getLogger();
    private static final String KEY_VISIBLE_STATS = "visibleResultStats";
    private static final String SEPARATOR = "\u001F"; // разделитель, который не встретится в названии показателя
    
    private final Preferences prefs;
    
    public StatDisplayPreferences() {
        this.prefs = Preferences.userNodeForPackage(StatDisplayPreferences.class);
    }
    
    /**
     * @return набор названий показателей, которые пользователь отметил
     */
    public Set<String> getVisibleStats() {
        String raw = prefs.get(KEY_VISIBLE_STATS, "");
        Set<String> visible = new LinkedHashSet<>();
        if (!raw.isEmpty()) {
            for (String part : raw.split(SEPARATOR)) {
                if (!part.isEmpty()) {
                    visible.add(part);
                }
            }
        }
        return visible;
    }
    
    public boolean isVisible(String statName) {
        return getVisibleStats().contains(statName);
    }
    
    public void setVisible(String statName, boolean visible) {
        Set<String> selected = getVisibleStats();
        boolean changed = visible ? selected.add(statName) : selected.remove(statName);
        if (!changed) {
            return;
        }
        prefs.put(KEY_VISIBLE_STATS, String.join(SEPARATOR, selected));
        flush();
    }
    
    private void flush() {
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            logger.warn("Не удалось сохранить настройки отображения показателей: {}", e.getMessage());
        }
    }
}
