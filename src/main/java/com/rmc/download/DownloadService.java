package com.rmc.download;

import com.rmc.driver.EdgeDetector;
import com.rmc.driver.EdgeInfo;
import com.rmc.driver.resolver.DriverDownloadInfo;
import com.rmc.driver.resolver.DriverResolver;
import com.rmc.driver.resolver.DriverResolverException;
import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service for downloading Microsoft Edge WebDriver.
 * Downloads the correct version matching the installed Edge browser.
 * 
 * <p>Uses DriverResolver to obtain download URLs instead of building them directly.</p>
 */
public class DownloadService {

    private static final Logger logger = AppLogger.getLogger();
    
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;
    private static final int BUFFER_SIZE = 8192;
    
    private static final String VERSION_FILENAME = "version.txt";

    private DownloadService() {
        // Utility class
    }

    /**
     * Download Edge WebDriver matching the installed Edge version.
     *
     * @return DownloadResult indicating success or failure
     */
    public static DownloadResult downloadDriver() {
        logger.info("=================================================");
        logger.info("Driver Download Engine");
        logger.info("=================================================");

        try {
            // Step 1: Detect Edge version
            EdgeInfo edgeInfo = EdgeDetector.detect();
            if (!edgeInfo.isInstalled()) {
                logger.error("Microsoft Edge is not installed");
                return DownloadResult.edgeNotInstalled();
            }

            String edgeVersion = edgeInfo.getVersion();
            logger.info("Detected Edge version: {}", edgeVersion);

            // Step 2: Resolve download info using DriverResolver
            DriverDownloadInfo downloadInfo = DriverResolver.resolve(edgeVersion);
            String driverVersion = downloadInfo.getDriverVersion();
            String downloadUrl = downloadInfo.getDownloadUrl();
            String driverFileName = downloadInfo.getDriverFileName();

            // Step 3: Check if driver already exists
            Path driverDir = getDriverDirectory();
            Path existingDriver = driverDir.resolve(driverFileName);
            if (Files.exists(existingDriver)) {
                String existingVersion = readInstalledVersion(driverDir);
                if (driverVersion.equals(existingVersion)) {
                    logger.info("Driver already exists with matching version");
                    return DownloadResult.alreadyExists(existingDriver.toString(), existingVersion);
                }
            }

            logger.info("Download URL: {}", downloadUrl);

            // Step 4: Download ZIP archive
            Path zipPath = downloadFile(downloadUrl);
            if (zipPath == null) {
                return DownloadResult.httpError(404);
            }

            // Step 5: Extract and install driver
            Path installedDriver = extractAndInstall(zipPath, driverVersion, downloadInfo);
            
            // Step 6: Cleanup
            cleanup(zipPath);

            logger.info("Finished successfully");
            logger.info("=================================================");

            return DownloadResult.success(installedDriver.toString(), driverVersion);

        } catch (DriverResolverException e) {
            logger.error("Failed to resolve driver: {}", e.getMessage());
            return DownloadResult.resolverError(e.getErrorType().name(), e.getMessage());
        } catch (Exception e) {
            logger.error("Download failed", e);
            return handleException(e);
        }
    }

    /**
     * Download file from URL with progress logging.
     */
    private static Path downloadFile(String urlString) {
        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;
        
        try {
            URL url = new URL(urlString);
            logger.info("Downloading...");
            
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", "RMC-Parser");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                logger.error("HTTP error: {}", responseCode);
                return null;
            }
            
            long totalSize = connection.getContentLengthLong();
            
            Path tempDir = Files.createTempDirectory("rmc-driver-");
            Path zipPath = tempDir.resolve("edgedriver.zip");
            
            input = connection.getInputStream();
            output = new FileOutputStream(zipPath.toFile());
            
            byte[] buffer = new byte[BUFFER_SIZE];
            long downloaded = 0;
            int bytesRead;
            int lastPercent = 0;
            
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                downloaded += bytesRead;
                
                if (totalSize > 0) {
                    int percent = (int) (downloaded * 100 / totalSize);
                    if (percent > lastPercent && percent % 10 == 0) {
                        logger.info("Download progress: {}%", percent);
                        lastPercent = percent;
                    }
                }
            }
            
            logger.info("Download completed ({} bytes)", downloaded);
            return zipPath;
            
        } catch (SocketTimeoutException e) {
            logger.error("Connection timeout", e);
            return null;
        } catch (UnknownHostException e) {
            logger.error("Unknown host - network unavailable", e);
            return null;
        } catch (IOException e) {
            logger.error("Download failed", e);
            return null;
        } finally {
            closeQuietly(input);
            closeQuietly(output);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Extract ZIP and install driver to the driver directory.
     */
    private static Path extractAndInstall(Path zipPath, String version, DriverDownloadInfo downloadInfo) throws IOException {
        logger.info("Extracting archive...");
        
        Path driverDir = getDriverDirectory();
        Files.createDirectories(driverDir);
        
        Path extractDir = Files.createTempDirectory("rmc-extract-");
        Path driverInZip = null;
        String driverFileName = downloadInfo.getDriverFileName();
        
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path targetPath = extractDir.resolve(entry.getName());
                
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath);
                    
                    if (entry.getName().endsWith(driverFileName)) {
                        driverInZip = targetPath;
                    }
                }
                zis.closeEntry();
            }
        }
        
        if (driverInZip == null) {
            throw new IOException(driverFileName + " not found in ZIP archive");
        }
        
        logger.info("Copying driver to {}", driverDir);
        Path installedDriver = driverDir.resolve(driverFileName);
        Files.copy(driverInZip, installedDriver, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Driver copied");
        
        // Create version.txt
        Path versionFile = driverDir.resolve(VERSION_FILENAME);
        Files.writeString(versionFile, version);
        logger.info("version.txt created");
        
        return installedDriver;
    }

    /**
     * Get the driver storage directory.
     * Uses %LOCALAPPDATA%\RMCParser\drivers\edge\
     */
    static Path getDriverDirectory() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null) {
            localAppData = System.getProperty("user.home");
        }
        return Paths.get(localAppData, "RMCParser", "drivers", "edge");
    }

    /**
     * Read the installed driver version from version.txt.
     */
    private static String readInstalledVersion(Path driverDir) {
        Path versionFile = driverDir.resolve(VERSION_FILENAME);
        try {
            return Files.readString(versionFile).trim();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Cleanup temporary files.
     */
    private static void cleanup(Path zipPath) {
        logger.info("Removing temporary files...");
        try {
            // Remove parent temp directory (contains the zip)
            Path tempDir = zipPath.getParent();
            if (tempDir != null && Files.exists(tempDir)) {
                Files.walk(tempDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
            }
            logger.info("Temporary files removed");
        } catch (IOException e) {
            logger.warn("Failed to cleanup temporary files", e);
        }
    }

    /**
     * Handle exceptions and return appropriate error result.
     */
    private static DownloadResult handleException(Exception e) {
        if (e instanceof SocketTimeoutException || e instanceof UnknownHostException) {
            return DownloadResult.internetUnavailable(e);
        }
        if (e instanceof java.net.ConnectException) {
            return DownloadResult.internetUnavailable(e);
        }
        if (e instanceof AccessDeniedException) {
            return DownloadResult.permissionDenied(e.getMessage());
        }
        if (e instanceof IOException) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("permission")) {
                return DownloadResult.permissionDenied(msg);
            }
            return DownloadResult.unknownError(e);
        }
        return DownloadResult.unknownError(e);
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {}
        }
    }
}