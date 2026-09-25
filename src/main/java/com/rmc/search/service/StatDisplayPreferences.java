package com.rmc.search.service;

import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Хранит, какие показатели ("Зачислений по сертификату", "Зачислений по
 * Бюджету" и т.д.) пользователь решил СКРЫТЬ из результатов анализа —
 * настройка переживает перезапуск программы и компьютера (реестр Windows
 * через стандартный Java Preferences API).
 *
 * <p>Хранится именно набор скрытых показателей, а не показанных: так
 * любой новый показатель, который раньше не встречался (например, сайт
 * добавил новую метрику), по умолчанию виден пользователю, а не пропадает
 * молча.</p>
 */
public class StatDisplayPreferences {
    
    private static final Logger logger = AppLogger.getLogger();
    private static final String KEY_HIDDEN_STATS = "hiddenResultStats";
    private static final String SEPARATOR = "\u001F"; // разделитель, который не встретится в названии показателя
    
    private final Preferences prefs;
    
    public StatDisplayPreferences() {
        this.prefs = Preferences.userNodeForPackage(StatDisplayPreferences.class);
    }
    
    /**
     * @return набор названий показателей, которые пользователь скрыл
     */
    public Set<String> getHiddenStats() {
        String raw = prefs.get(KEY_HIDDEN_STATS, "");
        Set<String> hidden = new LinkedHashSet<>();
        if (!raw.isEmpty()) {
            for (String part : raw.split(SEPARATOR)) {
                if (!part.isEmpty()) {
                    hidden.add(part);
                }
            }
        }
        return hidden;
    }
    
    public boolean isVisible(String statName) {
        return !getHiddenStats().contains(statName);
    }
    
    public void setVisible(String statName, boolean visible) {
        Set<String> hidden = getHiddenStats();
        boolean changed = visible ? hidden.remove(statName) : hidden.add(statName);
        if (!changed) {
            return;
        }
        prefs.put(KEY_HIDDEN_STATS, String.join(SEPARATOR, hidden));
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
