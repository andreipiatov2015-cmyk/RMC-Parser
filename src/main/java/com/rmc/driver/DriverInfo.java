package com.rmc.driver;

/**
 * Data class containing Microsoft Edge WebDriver information.
 */
public class DriverInfo {

    private final String path;
    private final String version;
    private final boolean installed;

    public DriverInfo(String path, String version, boolean installed) {
        this.path = path;
        this.version = version;
        this.installed = installed;
    }

    public static DriverInfo notInstalled() {
        return new DriverInfo(null, null, false);
    }

    public String getPath() {
        return path;
    }

    public String getVersion() {
        return version;
    }

    public boolean isInstalled() {
        return installed;
    }

    @Override
    public String toString() {
        if (installed) {
            return "DriverInfo{path='" + path + "', version='" + version + "', installed=true}";
        }
        return "DriverInfo{not installed}";
    }
}