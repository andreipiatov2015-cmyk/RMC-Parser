package com.rmc.ui.statusbar;

import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Прогресс кнопки-пасхалки на диске — переживает перезапуск программы и
 * перезагрузку компьютера, как и остальные постоянные данные приложения
 * ({@code %LOCALAPPDATA%\RMCFramework}).
 */
public final class EasterEggState {
    
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
        STATE_FILE = new File(dir, "easter_egg.properties");
    }
    
    private int clickIndex;
    private long lastTauntEpochMillis;
    
    private EasterEggState(int clickIndex, long lastTauntEpochMillis) {
        this.clickIndex = clickIndex;
        this.lastTauntEpochMillis = lastTauntEpochMillis;
    }
    
    public static EasterEggState load() {
        if (!STATE_FILE.exists()) {
            return new EasterEggState(0, 0L);
        }
        try {
            Properties props = new Properties();
            try (var in = Files.newInputStream(STATE_FILE.toPath())) {
                props.load(in);
            }
            int index = Integer.parseInt(props.getProperty("clickIndex", "0"));
            long lastTaunt = Long.parseLong(props.getProperty("lastTaunt", "0"));
            return new EasterEggState(index, lastTaunt);
        } catch (Exception e) {
            logger.warn("Не удалось загрузить состояние пасхалки: {}", e.getMessage());
            return new EasterEggState(0, 0L);
        }
    }
    
    public int getClickIndex() {
        return clickIndex;
    }
    
    public long getLastTauntEpochMillis() {
        return lastTauntEpochMillis;
    }
    
    public boolean isFinished() {
        return clickIndex >= EasterEggMessages.SEQUENCE.size();
    }
    
    /**
     * @return следующая реплика по счёту и продвигает счётчик на один клик вперёд
     */
    public String advanceAndGetMessage() {
        String message = EasterEggMessages.SEQUENCE.get(clickIndex);
        clickIndex++;
        save();
        return message;
    }
    
    public void recordTaunt(long epochMillis) {
        this.lastTauntEpochMillis = epochMillis;
        save();
    }
    
    private void save() {
        try {
            Properties props = new Properties();
            props.setProperty("clickIndex", String.valueOf(clickIndex));
            props.setProperty("lastTaunt", String.valueOf(lastTauntEpochMillis));
            try (var out = Files.newOutputStream(STATE_FILE.toPath())) {
                props.store(out, "RMC Framework — состояние кнопки-пасхалки");
            }
        } catch (Exception e) {
            logger.warn("Не удалось сохранить состояние пасхалки: {}", e.getMessage());
        }
    }
}
