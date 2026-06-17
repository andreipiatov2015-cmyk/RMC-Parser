package com.rmc.driver.resolver;

import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves Microsoft Edge WebDriver download information based on browser version.
 * 
 * <p>This resolver is responsible ONLY for resolving download metadata.
 * It does NOT download files or install drivers.</p>
 */
public class DriverResolver {

    private static final Logger logger = AppLogger.getLogger();

    private static final String DRIVER_INFO_API = 
        "https://msedgedriver虚webdriverinfo.azurewebsites.net/api/Downloadinfo";

    private static final Pattern VERSION_PATTERN = 
        Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(\\.\\d+)?$");

    private DriverResolver() {
        // Utility class
    }

    /**
     * Resolve WebDriver download information for the given Edge browser version.
     *
     * @param browserVersion The installed Edge browser version
     * @return DriverDownloadInfo containing download URL and metadata
     * @throws DriverResolverException if resolution fails
     */
    public static DriverDownloadInfo resolve(String browserVersion) throws DriverResolverException {
        logger.info("=================================================");
        logger.info("Driver Resolver Engine");
        logger.info("=================================================");
        logger.info("Browser Version: {}", browserVersion);

        // Validate version format
        if (browserVersion == null || browserVersion.isEmpty()) {
            throw DriverResolverException.invalidVersion(browserVersion);
        }

        String normalizedVersion = normalizeVersion(browserVersion);
        logger.info("Normalized Version: {}", normalizedVersion);

        // Detect platform and architecture
        Platform platform = Platform.detect();
        Architecture architecture = Architecture.detect();
        logger.info("Platform: {}", platform);
        logger.info("Architecture: {}", architecture);

        // Check platform support
        if (platform != Platform.WINDOWS) {
            throw DriverResolverException.unsupportedPlatform(platform);
        }

        // Check architecture support
        if (!architecture.isSupported()) {
            throw DriverResolverException.unsupportedArchitecture(architecture);
        }

        // Extract driver version from browser version
        String driverVersion = extractDriverVersion(normalizedVersion);
        logger.info("Resolved Driver Version: {}", driverVersion);

        // Build download info
        String downloadUrl = buildDownloadUrl(driverVersion, platform, architecture);
        String archiveName = buildArchiveName(driverVersion, platform);
        String driverFileName = platform.getDriverExecutableName();

        logger.info("Resolved URL: {}", downloadUrl);
        logger.info("=================================================");

        return DriverDownloadInfo.builder()
                .browserVersion(browserVersion)
                .driverVersion(driverVersion)
                .downloadUrl(downloadUrl)
                .platform(platform)
                .architecture(architecture)
                .driverFileName(driverFileName)
                .archiveName(archiveName)
                .build();
    }

    /**
     * Normalize version string by extracting major.minor.build.
     * Example: "146.0.3856.109" -> "146.0.3856"
     */
    static String normalizeVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "";
        }

        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.matches()) {
            String major = matcher.group(1);
            String minor = matcher.group(2);
            String build = matcher.group(3);
            return major + "." + minor + "." + build;
        }

        // Fallback: just return the original
        String[] parts = version.split("\\.");
        if (parts.length >= 3) {
            return parts[0] + "." + parts[1] + "." + parts[2];
        }
        return version;
    }

    /**
     * Extract driver version from browser version.
     * For Edge, driver version = browser version (first 3 segments).
     */
    static String extractDriverVersion(String browserVersion) {
        return normalizeVersion(browserVersion);
    }

    /**
     * Build the Microsoft CDN download URL.
     */
    private static String buildDownloadUrl(String version, Platform platform, Architecture architecture) {
        String platformSuffix = platform.getUrlSuffix();
        return String.format("https://msedgedriver.azureedge.net/%s/edgedriver_%s.zip",
                version, platformSuffix);
    }

    /**
     * Build the expected archive name.
     */
    private static String buildArchiveName(String version, Platform platform) {
        return String.format("edgedriver_%s.zip", platform.getUrlSuffix());
    }

    /**
     * Validate that a version string is properly formatted.
     *
     * @param version Version string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidVersion(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        return VERSION_PATTERN.matcher(version).matches();
    }

    /**
     * Get platform and architecture for the current system.
     *
     * @return Array containing [platform, architecture]
     */
    public static Object[] getCurrentPlatformAndArchitecture() {
        return new Object[] { Platform.detect(), Architecture.detect() };
    }
}