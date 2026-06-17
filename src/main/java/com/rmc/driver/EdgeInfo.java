package com.rmc.driver;

/**
 * Data class containing Microsoft Edge browser information.
 */
public class EdgeInfo {

    private final String path;
    private final String version;
    private final boolean installed;

    public EdgeInfo(String path, String version, boolean installed) {
        this.path = path;
        this.version = version;
        this.installed = installed;
    }

    public static EdgeInfo notInstalled() {
        return new EdgeInfo(null, null, false);
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
            return "EdgeInfo{path='" + path + "', version='" + version + "', installed=true}";
        }
        return "EdgeInfo{not installed}";
    }
}