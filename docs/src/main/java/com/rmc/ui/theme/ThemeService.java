package com.rmc.ui.theme;

import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Хранит и применяет тему оформления (светлая/тёмная).
 *
 * <p>Настройка сохраняется на диск ({@code %LOCALAPPDATA%\RMCFramework\theme.properties})
 * и переживает перезапуск программы. Переключение применяется сразу же во
 * всех открытых окнах через подписчиков.</p>
 */
public final class ThemeService {
    
    private static final Logger logger = AppLogger.getLogger();
    private static final File STATE_FILE;
    
    static {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isEmpty()) {
            localAppData = System.getProperty("user.home") + File.separator + ".local" + File.separator + "share";
        }
        File dir = new File(localAppData + File.separator + "RMCFramework");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        STATE_FILE = new File(dir, "theme.properties");
    }
    
    private static volatile boolean darkMode = load();
    private static final List<Consumer<Boolean>> listeners = new ArrayList<>();
    
    private ThemeService() {
        // Утилитарный класс
    }
    
    public static boolean isDarkMode() {
        return darkMode;
    }
    
    public static void toggle() {
        setDarkMode(!darkMode);
    }
    
    public static void setDarkMode(boolean dark) {
        darkMode = dark;
        save(dark);
        for (Consumer<Boolean> listener : listeners) {
            try {
                listener.accept(dark);
            } catch (Exception e) {
                logger.warn("Ошибка в слушателе смены темы: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Подписаться на смену темы — вызывается сразу после
     * {@link #setDarkMode}/{@link #toggle}, с новым значением.
     */
    public static void addListener(Consumer<Boolean> listener) {
        listeners.add(listener);
    }
    
    private static boolean load() {
        if (!STATE_FILE.exists()) {
            return false;
        }
        try {
            String content = Files.readString(STATE_FILE.toPath()).trim();
            return "dark".equalsIgnoreCase(content);
        } catch (Exception e) {
            logger.warn("Не удалось загрузить настройку темы: {}", e.getMessage());
            return false;
        }
    }
    
    private static void save(boolean dark) {
        try {
            Files.writeString(STATE_FILE.toPath(), dark ? "dark" : "light");
        } catch (Exception e) {
            logger.warn("Не удалось сохранить настройку темы: {}", e.getMessage());
        }
    }
}
