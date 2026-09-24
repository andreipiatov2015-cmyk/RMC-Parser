package com.rmc.ai;

import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;

/**
 * Объединяет {@link EmbeddingService} (считает вектор вопроса) и
 * {@link VectorIndex} (ищет похожие куски текста сайта) в один простой
 * вызов — это и есть мост между Java-приложением и моделью, полностью
 * работающий внутри JVM, без Python в рантайме.
 */
public class RagSearchService implements AutoCloseable {

    private static final Logger logger = AppLogger.getLogger();

    /** Ниже этого порога совпадения считаем, что уверенного ответа нет. */
    public static final float DEFAULT_CONFIDENCE_THRESHOLD = 0.40f;

    private final EmbeddingService embeddingService;
    private final VectorIndex vectorIndex;

    public RagSearchService(Path onnxModelDir, Path indexDir) {
        this.embeddingService = new EmbeddingService(onnxModelDir);
        this.vectorIndex = VectorIndex.load(indexDir);
        logger.info("RagSearchService готов: {} чанков в индексе", vectorIndex.size());
    }

    /**
     * Результат поиска по вопросу: топ найденных кусков плюс явный
     * признак, найдено ли что-то достаточно уверенное.
     */
    public static class Answer {
        public final List<VectorIndex.SearchResult> results;
        public final boolean confident;

        Answer(List<VectorIndex.SearchResult> results, boolean confident) {
            this.results = results;
            this.confident = confident;
        }
    }

    public Answer search(String question, int topK) {
        return search(question, topK, DEFAULT_CONFIDENCE_THRESHOLD);
    }

    public Answer search(String question, int topK, float confidenceThreshold) {
        float[] queryVector = embeddingService.embed(question);
        List<VectorIndex.SearchResult> results = vectorIndex.search(queryVector, topK);

        boolean confident = !results.isEmpty() && results.get(0).score >= confidenceThreshold;
        if (!confident) {
            logger.info("Низкая уверенность по вопросу \"{}\" (лучший score={})", question,
                    results.isEmpty() ? "н/д" : results.get(0).score);
        }

        return new Answer(results, confident);
    }

    @Override
    public void close() {
        embeddingService.close();
    }
}
