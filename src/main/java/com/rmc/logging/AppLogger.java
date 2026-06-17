package com.rmc.logging;

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
        logger.info("Version 0.1.0");
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
        String jarPath = AppLogger.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        File jarFile = new File(jarPath);
        return jarFile.getParentFile() != null ? jarFile.getParentFile().getAbsolutePath() : "Unknown";
    }
}