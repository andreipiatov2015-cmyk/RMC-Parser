package com.rmc.logging;

import com.rmc.version.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public final class AppLogger {

    private static final Logger logger = LoggerFactory.getLogger(AppLogger.class);
    private static final String LOG_DIR;

    static {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isEmpty()) {
            localAppData = System.getProperty("user.home") + File.separator + ".local" + File.separator + "share";
        }
        LOG_DIR = localAppData + File.separator + "RMCFramework" + File.separator + "logs";
        
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
    }

    private AppLogger() {
    }

    public static Logger getLogger() {
        return logger;
    }

    public static String getLogDirectory() {
        return LOG_DIR;
    }

    public static void logStartupInfo() {
        logger.info("=================================================");
        logger.info("RMC Framework");
        logger.info("Version {}", VersionService.getCurrentVersionString());
        logger.info("=================================================");
        logger.info("Java Version: {}", System.getProperty("java.version"));
        logger.info("Java Vendor: {}", System.getProperty("java.vendor"));
        logger.info("Operating System: {}", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        logger.info("Current User: {}", System.getProperty("user.name"));
        logger.info("Working Directory: {}", System.getProperty("user.dir"));
        logger.info("Application Directory: {}", getApplicationDirectory());
        logger.info("Log Directory: {}", LOG_DIR);
        logger.info("Application Started: {}", java.time.LocalDateTime.now());
        logger.info("=================================================");
    }

    private static String getApplicationDirectory() {
        try {
            // .getPath() отдаёт "сырой" путь URL, где пробелы закодированы
            // как %20 (у нас так и есть — "RMC Framework" с пробелом).
            // Через .toURI() -> new File(URI) путь корректно раскодируется.
            File jarFile = new File(AppLogger.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            return jarFile.getParentFile() != null ? jarFile.getParentFile().getAbsolutePath() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }
}