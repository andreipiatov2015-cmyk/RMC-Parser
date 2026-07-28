package com.rmc.update;

import com.rmc.logging.AppLogger;
import com.rmc.version.VersionComparison;
import com.rmc.version.VersionParseException;
import com.rmc.version.VersionService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

/**
 * Проверка и скачивание обновлений через GitHub Releases API.
 *
 * <p>Источник — публичный репозиторий релизов:
 * https://github.com/andreipiatov2015-cmyk/RMC-Parser-public</p>
 *
 * <p>Не подменяет запущенный jar/exe самостоятельно — скачивает файл
 * релиза в выбранное пользователем место. Самообновление "на лету"
 * потребовало бы отдельного инсталлятора/лаунчера, которого в проекте
 * пока нет.</p>
 */
public class UpdateCheckService {
    
    private static final Logger logger = AppLogger.getLogger();
    
    private static final String REPO_OWNER = "andreipiatov2015-cmyk";
    private static final String REPO_NAME = "RMC-Parser-public";
    private static final String RELEASES_API_URL =
            "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/releases/latest";
    
    private final HttpClient httpClient;
    
    public UpdateCheckService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                // ВАЖНО: по умолчанию HttpClient НЕ следует редиректам.
                // browser_download_url у GitHub всегда отдаёт 302 на реальное
                // хранилище файла (objects.githubusercontent.com и т.п.) —
                // без этой настройки скачивание падало с "код 302".
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }
    
    /**
     * Проверить, есть ли более новый релиз, чем текущая версия приложения.
     * Никогда не бросает исключение — все ошибки (нет сети, нет релизов,
     * репозиторий недоступен) отражены в {@code UpdateCheckResult}.
     */
    public UpdateCheckResult checkForUpdates() {
        String currentVersion = VersionService.getCurrentVersionString();
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RELEASES_API_URL))
                    .header("Accept", "application/vnd.github+json")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 404) {
                return UpdateCheckResult.builder()
                        .success(false)
                        .currentVersion(currentVersion)
                        .errorMessage("В репозитории обновлений пока нет ни одного релиза")
                        .build();
            }
            
            if (response.statusCode() != 200) {
                return UpdateCheckResult.builder()
                        .success(false)
                        .currentVersion(currentVersion)
                        .errorMessage("GitHub API вернул код " + response.statusCode())
                        .build();
            }
            
            JSONObject release = new JSONObject(response.body());
            String rawTag = release.optString("tag_name", "");
            String normalizedTag = normalizeVersionTag(rawTag);
            
            if (normalizedTag == null) {
                return UpdateCheckResult.builder()
                        .success(false)
                        .currentVersion(currentVersion)
                        .errorMessage("Не удалось разобрать версию релиза: \"" + rawTag + "\"")
                        .build();
            }
            
            boolean isNewer;
            try {
                isNewer = VersionService.compare(normalizedTag, currentVersion) == VersionComparison.NEWER;
            } catch (VersionParseException e) {
                return UpdateCheckResult.builder()
                        .success(false)
                        .currentVersion(currentVersion)
                        .errorMessage("Ошибка сравнения версий: " + e.getMessage())
                        .build();
            }
            
            String downloadUrl = null;
            String assetName = null;
            JSONArray assets = release.optJSONArray("assets");
            if (assets != null && assets.length() > 0) {
                JSONObject firstAsset = assets.getJSONObject(0);
                downloadUrl = firstAsset.optString("browser_download_url", null);
                assetName = firstAsset.optString("name", null);
            }
            
            return UpdateCheckResult.builder()
                    .success(true)
                    .updateAvailable(isNewer)
                    .currentVersion(currentVersion)
                    .latestVersion(normalizedTag)
                    .downloadUrl(downloadUrl)
                    .assetName(assetName)
                    .releaseNotes(release.optString("body", ""))
                    .releaseUrl(release.optString("html_url", null))
                    .build();
            
        } catch (IOException | InterruptedException e) {
            logger.warn("Не удалось проверить обновления: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return UpdateCheckResult.builder()
                    .success(false)
                    .currentVersion(currentVersion)
                    .errorMessage("Нет соединения с GitHub: " + e.getMessage())
                    .build();
        } catch (Exception e) {
            logger.error("Ошибка проверки обновлений: {}", e.getMessage());
            return UpdateCheckResult.builder()
                    .success(false)
                    .currentVersion(currentVersion)
                    .errorMessage("Ошибка: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * Слушатель прогресса скачивания — вызывается после каждого
     * прочитанного блока данных. {@code totalBytes} может быть {@code -1},
     * если сервер не прислал Content-Length (тогда прогресс неопределим,
     * вызывающий код должен показать индикатор без процентов).
     */
    @FunctionalInterface
    public interface DownloadProgressListener {
        void onProgress(long bytesRead, long totalBytes);
    }
    
    public void download(String downloadUrl, Path destination) throws IOException, InterruptedException {
        download(downloadUrl, destination, null);
    }
    
    /**
     * Скачать файл релиза по прямой ссылке в указанное место.
     */
    public void download(String downloadUrl, Path destination, DownloadProgressListener listener)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();
        
        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (java.net.http.HttpConnectTimeoutException e) {
            // Сообщение самого исключения ("HTTP connect timed out") не
            // говорит, к какому адресу шло обращение — добавляем URL, иначе
            // по одной этой фразе не понять, что именно недоступно.
            throw new IOException("Не удалось подключиться к серверу загрузки (" + downloadUrl + "): "
                    + e.getMessage(), e);
        } catch (IOException e) {
            throw new IOException("Ошибка сети при скачивании (" + downloadUrl + "): " + e.getMessage(), e);
        }
        
        if (response.statusCode() != 200) {
            throw new IOException("Сервер вернул код " + response.statusCode() + " при скачивании файла");
        }
        
        long totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        
        try (InputStream in = response.body();
             OutputStream out = Files.newOutputStream(destination,
                     StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[8192];
            long totalRead = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                totalRead += read;
                if (listener != null) {
                    listener.onProgress(totalRead, totalBytes);
                }
            }
        }
    }
    
    /**
     * Приводит тег релиза к формату "major.minor.patch", ожидаемому
     * {@link com.rmc.version.VersionParser} — убирает возможный префикс "v"/"V".
     */
    private String normalizeVersionTag(String tag) {
        if (tag == null || tag.isEmpty()) {
            return null;
        }
        String trimmed = tag.trim();
        if (trimmed.startsWith("v") || trimmed.startsWith("V")) {
            trimmed = trimmed.substring(1);
        }
        return com.rmc.version.VersionParser.tryParse(trimmed) != null ? trimmed : null;
    }
}
