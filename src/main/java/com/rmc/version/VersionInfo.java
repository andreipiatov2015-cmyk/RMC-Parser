package com.rmc.version;

/**
 * Data class containing version information with optional metadata.
 */
public class VersionInfo {

    private final Version version;
    private final String application;
    private final String releaseDate;
    private final boolean required;
    private final String changelog;
    private final String downloadZip;
    private final String downloadMsi;

    public VersionInfo(Version version, String application, String releaseDate,
                       boolean required, String changelog, String downloadZip, String downloadMsi) {
        this.version = version;
        this.application = application;
        this.releaseDate = releaseDate;
        this.required = required;
        this.changelog = changelog;
        this.downloadZip = downloadZip;
        this.downloadMsi = downloadMsi;
    }

    public Version getVersion() {
        return version;
    }

    public String getApplication() {
        return application;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public boolean isRequired() {
        return required;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getDownloadZip() {
        return downloadZip;
    }

    public String getDownloadMsi() {
        return downloadMsi;
    }

    @Override
    public String toString() {
        return "VersionInfo{" +
                "version=" + version +
                ", application='" + application + '\'' +
                ", releaseDate='" + releaseDate + '\'' +
                ", required=" + required +
                ", changelog='" + changelog + '\'' +
                ", downloadZip='" + downloadZip + '\'' +
                ", downloadMsi='" + downloadMsi + '\'' +
                '}';
    }
}