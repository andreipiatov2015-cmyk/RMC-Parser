package com.rmc.update;

import com.rmc.config.UpdateConfig;
import com.rmc.i18n.Messages;
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
        logger.info(Messages.LOG_UPDATE_SERVICE);

        long startTime = System.currentTimeMillis();

        try {
            UpdateConfig config = UpdateConfig.load();
            logger.info(Messages.LOG_LOADING_CONFIG);
            logger.info(Messages.LOG_JSON_URL, config.getJsonUrl());

            return downloadLatestJson(config.getJsonUrl(), startTime);

        } catch (IOException e) {
            logger.error(Messages.LOG_CONFIG_ERROR_PREFIX + e.getMessage(), e);
            return UpdateCheckResult.error(-1, Messages.LOG_CONFIG_ERROR_PREFIX + e.getMessage(), e);
        }
    }

    private UpdateCheckResult downloadLatestJson(String jsonUrl, long startTime) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(jsonUrl);
            logger.info(Messages.LOG_CONNECTING, jsonUrl);
            
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Accept", "application/json");

            int httpStatus = connection.getResponseCode();
            long responseLength = connection.getContentLengthLong();
            long downloadTime = System.currentTimeMillis() - startTime;

            logger.info(Messages.LOG_HTTP_STATUS, httpStatus);
            logger.info(Messages.LOG_RESPONSE_LENGTH, responseLength);
            logger.info(Messages.LOG_DOWNLOAD_TIME, downloadTime);

            if (httpStatus == 200) {
                logger.info(Messages.LOG_CONNECTION_SUCCESS);
                String jsonContent = readResponseBody(connection);
                logger.info("JSON содержимое:");
                logger.info(jsonContent);
                logger.info(Messages.LOG_JSON_DOWNLOADED);
            } else {
                String responseBody = readResponseBody(connection);
                logger.warn(Messages.LOG_UNEXPECTED_STATUS, httpStatus);
                logger.warn(Messages.LOG_RESPONSE_BODY, responseBody);
            }

            logger.info(Messages.LOG_UPDATE_CHECK_COMPLETE);
            logger.info(Messages.LOG_UPDATE_CHECK_END);

            return UpdateCheckResult.success(httpStatus, responseLength);

        } catch (SocketTimeoutException e) {
            logger.error(Messages.LOG_TIMEOUT, e);
            return UpdateCheckResult.error(-1, Messages.LOG_TIMEOUT, e);

        } catch (UnknownHostException e) {
            logger.error(Messages.LOG_UNKNOWN_HOST, e);
            return UpdateCheckResult.error(-1, Messages.LOG_UNKNOWN_HOST, e);

        } catch (IOException e) {
            logger.error(Messages.LOG_CONNECTION_ERROR, e);
            return UpdateCheckResult.error(-1, Messages.LOG_CONNECTION_ERROR, e);

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
                return "Нет содержимого";
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
            logger.warn("Не удалось прочитать тело ответа", e);
            return "Не удалось прочитать тело ответа";
        }
    }
}