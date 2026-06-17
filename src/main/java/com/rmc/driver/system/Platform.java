package com.rmc.driver.system;

/**
 * Перечисление поддерживаемых платформ.
 */
public enum Platform {
    WINDOWS("win64", "windows"),
    LINUX("linux64", "linux"),
    MAC("mac64", "macos"),
    UNKNOWN("unknown", "unknown");

    private final String osName;
    private final String urlSuffix;

    Platform(String osName, String urlSuffix) {
        this.osName = osName;
        this.urlSuffix = urlSuffix;
    }

    /**
     * Определить платформу текущей системы.
     */
    public static Platform detect() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        
        if (osName.contains("win")) {
            return WINDOWS;
        } else if (osName.contains("linux")) {
            return LINUX;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return MAC;
        }
        
        return UNKNOWN;
    }

    public String getOsName() {
        return osName;
    }

    public String getUrlSuffix() {
        return urlSuffix;
    }

    public String getDriverExecutableName() {
        return this == WINDOWS ? "msedgedriver.exe" : "msedgedriver";
    }
}
