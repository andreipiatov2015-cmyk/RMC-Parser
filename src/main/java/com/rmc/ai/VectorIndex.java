package com.rmc.ai;

import com.rmc.logging.AppLogger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Хранит вектора чанков сайта (посчитанные заранее в Python,
 * {@code build_index_for_java.py}) и ищет среди них ближайшие к вектору
 * вопроса пользователя — простым перебором (косинусная близость через
 * скалярное произведение, вектора уже нормализованы).
 *
 * <p>Почему не FAISS: у него нет официальных биндингов для Java, а
 * городить JNI-обвязку ради этого не оправдано — при текущем и
 * обозримом объёме корпуса (на уровне тысяч, возможно первых десятков
 * тысяч чанков) перебор в чистой Java достаточно быстр (миллисекунды
 * на запрос) и не тянет за собой никаких нативных зависимостей.</p>
 *
 * <p><b>Формат файла вектора ({@code vectors.bin}):</b> float32,
 * little-endian, построчно — сначала все {@code dimension} чисел
 * первого чанка, затем второго, и так далее, в том же порядке, что и
 * записи в {@code meta.json}. Порядок байт указан явно (см.
 * {@link ByteOrder#LITTLE_ENDIAN}), потому что numpy на Python-стороне
 * пишет float32 в little-endian, а {@code DataInputStream.readFloat()}
 * в Java по умолчанию читает big-endian — без явного указания порядка
 * все вектора прочитались бы как бессмысленный шум без единой ошибки.</p>
 */
public class VectorIndex {

    private static final Logger logger = AppLogger.getLogger();

    /**
     * Один проиндексированный кусок текста с сайта.
     */
    public static class Chunk {
        public final String text;
        public final String url;
        public final String title;
        public final String sourceFile;

        Chunk(String text, String url, String title, String sourceFile) {
            this.text = text;
            this.url = url;
            this.title = title;
            this.sourceFile = sourceFile;
        }
    }

    /**
     * Один результат поиска — чанк и его оценка похожести (0..1, чем
     * ближе к 1, тем увереннее совпадение).
     */
    public static class SearchResult {
        public final Chunk chunk;
        public final float score;

        SearchResult(Chunk chunk, float score) {
            this.chunk = chunk;
            this.score = score;
        }
    }

    private final float[][] vectors;
    private final List<Chunk> chunks;
    private final int dimension;

    private VectorIndex(float[][] vectors, List<Chunk> chunks, int dimension) {
        this.vectors = vectors;
        this.chunks = chunks;
        this.dimension = dimension;
    }

    /**
     * Загружает индекс из папки, созданной {@code build_index_for_java.py}
     * (там должны лежать {@code vectors.bin} и {@code meta.json}).
     */
    public static VectorIndex load(Path indexDir) {
        Path vectorsFile = indexDir.resolve("vectors.bin");
        Path metaFile = indexDir.resolve("meta.json");

        if (!Files.exists(vectorsFile) || !Files.exists(metaFile)) {
            throw new IllegalStateException("Индекс не найден в " + indexDir
                    + " — сначала запустите build_index_for_java.py на Python-стороне.");
        }

        try {
            String metaJson = Files.readString(metaFile);
            JSONObject meta = new JSONObject(metaJson);
            int dimension = meta.getInt("dimension");
            int count = meta.getInt("count");

            List<Chunk> chunks = new ArrayList<>(count);
            JSONArray chunksArray = meta.getJSONArray("chunks");
            for (int i = 0; i < chunksArray.length(); i++) {
                JSONObject c = chunksArray.getJSONObject(i);
                chunks.add(new Chunk(
                        c.optString("text", ""),
                        c.optString("url", ""),
                        c.optString("title", ""),
                        c.optString("source_file", "")
                ));
            }

            if (chunks.size() != count) {
                logger.warn("Число чанков в meta.json ({}) не совпадает с полем count ({})",
                        chunks.size(), count);
            }

            byte[] rawBytes = Files.readAllBytes(vectorsFile);
            long expectedBytes = (long) count * dimension * Float.BYTES;
            if (rawBytes.length != expectedBytes) {
                throw new IllegalStateException(String.format(
                        "Размер vectors.bin (%d байт) не совпадает с ожидаемым (%d байт для %d x %d float32). "
                                + "Индекс собран не полностью или файл повреждён.",
                        rawBytes.length, expectedBytes, count, dimension));
            }

            ByteBuffer buffer = ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN);
            float[][] vectors = new float[count][dimension];
            for (int row = 0; row < count; row++) {
                for (int col = 0; col < dimension; col++) {
                    vectors[row][col] = buffer.getFloat();
                }
            }

            logger.info("Индекс загружен: {} векторов, размерность {}", count, dimension);
            return new VectorIndex(vectors, chunks, dimension);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать индекс из " + indexDir, e);
        }
    }

    /**
     * Ищет {@code topK} чанков, ближайших к вектору запроса.
     *
     * @param queryVector нормализованный вектор вопроса (см. {@link EmbeddingService#embed})
     */
    public List<SearchResult> search(float[] queryVector, int topK) {
        if (queryVector.length != dimension) {
            throw new IllegalArgumentException(String.format(
                    "Размерность вектора запроса (%d) не совпадает с размерностью индекса (%d). "
                            + "Использована другая модель эмбеддингов?",
                    queryVector.length, dimension));
        }

        List<SearchResult> results = new ArrayList<>(vectors.length);
        for (int i = 0; i < vectors.length; i++) {
            float score = dotProduct(vectors[i], queryVector);
            results.add(new SearchResult(chunks.get(i), score));
        }

        results.sort(Comparator.comparingDouble((SearchResult r) -> r.score).reversed());
        return results.subList(0, Math.min(topK, results.size()));
    }

    private float dotProduct(float[] a, float[] b) {
        float sum = 0f;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    public int size() {
        return vectors.length;
    }

    public int getDimension() {
        return dimension;
    }
}
