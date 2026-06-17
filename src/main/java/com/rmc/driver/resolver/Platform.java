package com.rmc.driver.resolver;

/**
 * Supported platforms for Microsoft Edge WebDriver.
 */
public enum Platform {
    WINDOWS,
    LINUX,
    MAC;

    /**
     * Detect the current platform.
     *
     * @return Platform matching the current OS
     */
    public static Platform detect() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        
        if (osName.contains("win")) {
            return WINDOWS;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return MAC;
        } else if (osName.contains("linux")) {
            return LINUX;
        }
        
        return WINDOWS; // Default to Windows for safety
    }

    /**
     * Get the platform suffix for download URLs.
     *
     * @return Suffix used in Microsoft download URLs
     */
    public String getUrlSuffix() {
        switch (this) {
            case WINDOWS:
                return "win64";
            case LINUX:
                return "linux64";
            case MAC:
                return "mac64";
            default:
                return "win64";
        }
    }

    /**
     * Get the archive extension for this platform.
     *
     * @return Archive file extension (including dot)
     */
    public String getArchiveExtension() {
        switch (this) {
            case WINDOWS:
            case LINUX:
                return ".zip";
            case MAC:
                return ".zip";
            default:
                return ".zip";
        }
    }

    /**
     * Get the driver executable name for this platform.
     *
     * @return Driver executable name
     */
    public String getDriverExecutableName() {
        switch (this) {
            case WINDOWS:
                return "msedgedriver.exe";
            case LINUX:
            case MAC:
                return "msedgedriver";
            default:
                return "msedgedriver.exe";
        }
    }
}