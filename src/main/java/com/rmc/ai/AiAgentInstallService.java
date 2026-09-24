package com.rmc.ai;

import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Управляет наличием "тяжёлой" AI-части программы на компьютере
 * пользователя — модель не входит в основную поставку (чтобы не раздувать
 * вес установщика для тех, кто ей не пользуется), а загружается отдельно,
 * по явному согласию, в фоне.
 *
 * <p>Хранилище — по тому же принципу, что и {@code AccountStorageService}:
 * {@code %LOCALAPPDATA%\RMCFramework\ai\}, отдельно от самой программы,
 * переживает обновления/переустановку основного приложения.</p>
 *
 * <p><b>Важно:</b> метод {@link #installAsync} сейчас — заглушка,
 * имитирующая процесс загрузки и распаковки (этапы, проценты, тайминги
 * подобраны правдоподобно). Реальную загрузку конкретных файлов модели
 * (ONNX для грамматики, GGUF для чата — см. обсуждение архитектуры)
 * нужно подставить сюда, когда эти файлы будут подготовлены и выложены
 * куда-то, откуда программа сможет их скачать. Место для этого явно
 * помечено ниже комментарием TODO.</p>
 */
public class AiAgentInstallService {

    private static final Logger logger = AppLogger.getLogger();

    private static final File AI_DIR;
    private static final File INSTALLED_FLAG_FILE;
    private static final File MODELS_DIR;

    static {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null || localAppData.isEmpty()) {
            localAppData = System.getProperty("user.home") + File.separator + ".local" + File.separator + "share";
        }
        AI_DIR = new File(localAppData + File.separator + "RMCFramework" + File.separator + "ai");
        if (!AI_DIR.exists()) {
            AI_DIR.mkdirs();
        }
        INSTALLED_FLAG_FILE = new File(AI_DIR, "installed.flag");
        MODELS_DIR = new File(AI_DIR, "models");
    }

    /**
     * Обратный вызов прогресса установки.
     */
    public interface ProgressListener {
        /**
         * @param stage    человекочитаемое название текущего этапа
         *                 (например, "Загрузка модели грамматики")
         * @param percent  0..100, общий прогресс по всем этапам
         */
        void onProgress(String stage, int percent);

        void onComplete();

        void onError(String message);
    }

    /**
     * @return установлен ли AI-агент на этом компьютере
     */
    public boolean isInstalled() {
        return INSTALLED_FLAG_FILE.exists();
    }

    /**
     * Запускает установку в отдельном потоке — не блокирует UI. Прогресс
     * приходит через {@code listener}, вызовы которого уже должны быть
     * перенаправлены в UI-поток на стороне вызывающего кода (см.
     * {@code Platform.runLater} в {@link com.rmc.ui.workspace.views.RmcAiView}).
     */
    public void installAsync(ProgressListener listener) {
        new Thread(() -> {
            try {
                if (!MODELS_DIR.exists()) {
                    Files.createDirectories(MODELS_DIR.toPath());
                }

                // TODO: заменить имитацию на реальную загрузку файлов модели.
                //
                // Когда модели будут готовы (экспортированная в ONNX модель
                // грамматики + GGUF-модель для чата, см. обсуждение моста
                // Java <-> Python), здесь должно быть по одному блоку на
                // каждый файл вида:
                //
                //   downloadFile(URL_МОДЕЛИ, MODELS_DIR/"grammar.onnx", (downloaded, total) -> {
                //       int percent = ...;
                //       listener.onProgress("Загрузка модели грамматики", percent);
                //   });
                //
                // Ниже — только имитация этапов и прогресса, чтобы можно
                // было проверить весь путь (уведомление -> согласие ->
                // фоновая загрузка -> прогресс в статус-баре -> готово)
                // до того, как сами файлы модели существуют.
                runFakeStage(listener, "Подготовка к загрузке", 0, 10, 400);
                runFakeStage(listener, "Загрузка модели грамматики", 10, 55, 2200);
                runFakeStage(listener, "Загрузка модели чата", 55, 90, 2200);
                runFakeStage(listener, "Установка и проверка", 90, 100, 600);

                Files.writeString(INSTALLED_FLAG_FILE.toPath(), String.valueOf(System.currentTimeMillis()));
                logger.info("AI-агент установлен (заглушка): {}", AI_DIR);
                listener.onComplete();
            } catch (IOException | InterruptedException e) {
                logger.error("Ошибка установки AI-агента: {}", e.getMessage());
                listener.onError(e.getMessage());
            }
        }, "ai-agent-install").start();
    }

    private void runFakeStage(ProgressListener listener, String stage, int fromPercent, int toPercent,
                               long durationMs) throws InterruptedException {
        int steps = 20;
        long stepDelay = durationMs / steps;
        for (int i = 0; i <= steps; i++) {
            int percent = fromPercent + (toPercent - fromPercent) * i / steps;
            listener.onProgress(stage, percent);
            Thread.sleep(stepDelay);
        }
    }

    /**
     * Удаляет установленного AI-агента (например, если пользователь
     * захочет освободить место). Не используется пока нигде в UI —
     * добавлено на будущее, раз уж сервис всё равно про установку/статус.
     */
    public void uninstall() {
        deleteRecursively(MODELS_DIR);
        INSTALLED_FLAG_FILE.delete();
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
