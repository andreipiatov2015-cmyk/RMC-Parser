package com.rmc.update;

import com.rmc.config.UpdateConfig;
import com.rmc.logging.AppLogger;
import com.rmc.model.UpdateCheckResult;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

public class UpdateService {

    private static final Logger logger = AppLogger.getLogger();
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 15000;

    public UpdateCheckResult checkForUpdates() {
        logger.info("=================================================");
        logger.info("Update Service");
        logger.info("=================================================");

        long startTime = System.currentTimeMillis();

        try {
            UpdateConfig config = UpdateConfig.load();
            logger.info("Loading configuration...");
            logger.info("JSON URL: {}", config.getJsonUrl());

            return downloadLatestJson(config.getJsonUrl(), startTime);

        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
            return UpdateCheckResult.error(-1, "Configuration error: " + e.getMessage(), e);
        }
    }

    private UpdateCheckResult downloadLatestJson(String jsonUrl, long startTime) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(jsonUrl);
            logger.info("Connecting to: {}", jsonUrl);
            
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Accept", "application/json");

            int httpStatus = connection.getResponseCode();
            long responseLength = connection.getContentLengthLong();
            long downloadTime = System.currentTimeMillis() - startTime;

            logger.info("HTTP Status: {}", httpStatus);
            logger.info("Response Length: {} bytes", responseLength);
            logger.info("Download Time: {} ms", downloadTime);

            if (httpStatus == 200) {
                logger.info("Connection successful");
                String jsonContent = readResponseBody(connection);
                logger.info("JSON Content:");
                logger.info(jsonContent);
                logger.info("JSON downloaded successfully");
            } else {
                String responseBody = readResponseBody(connection);
                logger.warn("Unexpected HTTP Status: {}", httpStatus);
                logger.warn("Response Body: {}", responseBody);
            }

            logger.info("Update check completed");
            logger.info("=================================================");

            return UpdateCheckResult.success(httpStatus, responseLength);

        } catch (SocketTimeoutException e) {
            logger.error("Connection timeout", e);
            return UpdateCheckResult.error(-1, "Connection timeout", e);

        } catch (UnknownHostException e) {
            logger.error("Unknown host - network unavailable", e);
            return UpdateCheckResult.error(-1, "UnknownHostException: " + e.getMessage(), e);

        } catch (IOException e) {
            logger.error("Connection error", e);
            return UpdateCheckResult.error(-1, "IOException: " + e.getMessage(), e);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponseBody(HttpURLConnection connection) {
        try {
            int responseCode = connection.getResponseCode();
            java.io.InputStream stream = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (stream == null) {
                return "No content";
            }
            
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }
            return content.toString();
        } catch (IOException e) {
            logger.warn("Could not read response body", e);
            return "Unable to read response body";
        }
    }
}