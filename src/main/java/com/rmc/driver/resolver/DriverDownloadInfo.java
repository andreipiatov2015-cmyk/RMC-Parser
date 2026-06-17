package com.rmc.driver.resolver;

import java.util.Objects;

/**
 * Immutable container for Edge WebDriver download information.
 */
public class DriverDownloadInfo {

    private final String browserVersion;
    private final String driverVersion;
    private final String downloadUrl;
    private final Platform platform;
    private final Architecture architecture;
    private final String driverFileName;
    private final String archiveName;

    private DriverDownloadInfo(Builder builder) {
        this.browserVersion = builder.browserVersion;
        this.driverVersion = builder.driverVersion;
        this.downloadUrl = builder.downloadUrl;
        this.platform = builder.platform;
        this.architecture = builder.architecture;
        this.driverFileName = builder.driverFileName;
        this.archiveName = builder.archiveName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public String getDriverVersion() {
        return driverVersion;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Architecture getArchitecture() {
        return architecture;
    }

    public String getDriverFileName() {
        return driverFileName;
    }

    public String getArchiveName() {
        return archiveName;
    }

    @Override
    public String toString() {
        return "DriverDownloadInfo{" +
                "browserVersion='" + browserVersion + '\'' +
                ", driverVersion='" + driverVersion + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", platform=" + platform +
                ", architecture=" + architecture +
                ", driverFileName='" + driverFileName + '\'' +
                ", archiveName='" + archiveName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DriverDownloadInfo that = (DriverDownloadInfo) o;
        return Objects.equals(browserVersion, that.browserVersion) &&
                Objects.equals(driverVersion, that.driverVersion) &&
                Objects.equals(downloadUrl, that.downloadUrl) &&
                platform == that.platform &&
                architecture == that.architecture;
    }

    @Override
    public int hashCode() {
        return Objects.hash(browserVersion, driverVersion, downloadUrl, platform, architecture);
    }

    /**
     * Builder for DriverDownloadInfo.
     */
    public static class Builder {
        private String browserVersion;
        private String driverVersion;
        private String downloadUrl;
        private Platform platform;
        private Architecture architecture;
        private String driverFileName;
        private String archiveName;

        public Builder browserVersion(String browserVersion) {
            this.browserVersion = browserVersion;
            return this;
        }

        public Builder driverVersion(String driverVersion) {
            this.driverVersion = driverVersion;
            return this;
        }

        public Builder downloadUrl(String downloadUrl) {
            this.downloadUrl = downloadUrl;
            return this;
        }

        public Builder platform(Platform platform) {
            this.platform = platform;
            return this;
        }

        public Builder architecture(Architecture architecture) {
            this.architecture = architecture;
            return this;
        }

        public Builder driverFileName(String driverFileName) {
            this.driverFileName = driverFileName;
            return this;
        }

        public Builder archiveName(String archiveName) {
            this.archiveName = archiveName;
            return this;
        }

        public DriverDownloadInfo build() {
            return new DriverDownloadInfo(this);
        }
    }
}