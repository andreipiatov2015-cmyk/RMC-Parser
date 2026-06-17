package com.rmc.driver.validation;

import com.rmc.driver.resolver.Architecture;
import com.rmc.driver.resolver.Platform;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable manifest containing driver metadata.
 * 
 * <p>This is the central object for Driver Manager.
 * Future Driver Update Engine must work only with DriverManifest.</p>
 */
public class DriverManifest {

    private final String browserVersion;
    private final String driverVersion;
    private final String driverPath;
    private final Platform platform;
    private final Architecture architecture;
    private final LocalDateTime installationTime;
    private final LocalDateTime validationTime;
    private final ValidationStatus status;

    private DriverManifest(Builder builder) {
        this.browserVersion = builder.browserVersion;
        this.driverVersion = builder.driverVersion;
        this.driverPath = builder.driverPath;
        this.platform = builder.platform;
        this.architecture = builder.architecture;
        this.installationTime = builder.installationTime;
        this.validationTime = builder.validationTime;
        this.status = builder.status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DriverManifest empty() {
        return builder()
                .status(ValidationStatus.MISSING)
                .build();
    }

    public String getBrowserVersion() {
        return browserVersion;
    }

    public String getDriverVersion() {
        return driverVersion;
    }

    public String getDriverPath() {
        return driverPath;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Architecture getArchitecture() {
        return architecture;
    }

    public LocalDateTime getInstallationTime() {
        return installationTime;
    }

    public LocalDateTime getValidationTime() {
        return validationTime;
    }

    public ValidationStatus getStatus() {
        return status;
    }

    /**
     * Check if this manifest represents a valid driver.
     *
     * @return true if status is VALID
     */
    public boolean isValid() {
        return status == ValidationStatus.VALID;
    }

    /**
     * Check if the driver is missing.
     *
     * @return true if status is MISSING
     */
    public boolean isMissing() {
        return status == ValidationStatus.MISSING;
    }

    /**
     * Check if browser version is set.
     *
     * @return true if browser version is not null
     */
    public boolean hasBrowserVersion() {
        return browserVersion != null && !browserVersion.isEmpty();
    }

    /**
     * Check if driver version is set.
     *
     * @return true if driver version is not null
     */
    public boolean hasDriverVersion() {
        return driverVersion != null && !driverVersion.isEmpty();
    }

    /**
     * Check if driver path is set.
     *
     * @return true if driver path is not null
     */
    public boolean hasDriverPath() {
        return driverPath != null && !driverPath.isEmpty();
    }

    @Override
    public String toString() {
        return "DriverManifest{" +
                "browserVersion='" + browserVersion + '\'' +
                ", driverVersion='" + driverVersion + '\'' +
                ", driverPath='" + driverPath + '\'' +
                ", platform=" + platform +
                ", architecture=" + architecture +
                ", installationTime=" + installationTime +
                ", validationTime=" + validationTime +
                ", status=" + status +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DriverManifest that = (DriverManifest) o;
        return Objects.equals(browserVersion, that.browserVersion) &&
                Objects.equals(driverVersion, that.driverVersion) &&
                Objects.equals(driverPath, that.driverPath) &&
                platform == that.platform &&
                architecture == that.architecture;
    }

    @Override
    public int hashCode() {
        return Objects.hash(browserVersion, driverVersion, driverPath, platform, architecture);
    }

    /**
     * Builder for DriverManifest.
     */
    public static class Builder {
        private String browserVersion;
        private String driverVersion;
        private String driverPath;
        private Platform platform;
        private Architecture architecture;
        private LocalDateTime installationTime;
        private LocalDateTime validationTime;
        private ValidationStatus status;

        public Builder browserVersion(String browserVersion) {
            this.browserVersion = browserVersion;
            return this;
        }

        public Builder driverVersion(String driverVersion) {
            this.driverVersion = driverVersion;
            return this;
        }

        public Builder driverPath(String driverPath) {
            this.driverPath = driverPath;
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

        public Builder installationTime(LocalDateTime installationTime) {
            this.installationTime = installationTime;
            return this;
        }

        public Builder validationTime(LocalDateTime validationTime) {
            this.validationTime = validationTime;
            return this;
        }

        public Builder status(ValidationStatus status) {
            this.status = status;
            return this;
        }

        public DriverManifest build() {
            return new DriverManifest(this);
        }
    }
}