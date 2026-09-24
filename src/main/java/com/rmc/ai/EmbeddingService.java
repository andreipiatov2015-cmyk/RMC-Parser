package com.rmc.ai;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.rmc.logging.AppLogger;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.Map;

/**
 * Считает эмбеддинги (векторные представления текста) прямо внутри JVM,
 * без Python в рантайме — модель заранее экспортирована в ONNX на
 * Python-стороне (см. {@code export_embedding_model.py}), здесь она
 * только выполняется.
 *
 * <p><b>Критически важно:</b> pooling (усреднение векторов токенов в
 * один вектор текста) здесь реализован БУКВАЛЬНО так же, как в
 * {@code build_index_for_java.py} на Python-стороне (mean pooling с
 * маскированием паддинга + L2-нормализация). Если эти две реализации
 * разойдутся — вектора из индекса (посчитанные в Python при сборке) и
 * вектор вопроса пользователя (посчитанный здесь в Java при поиске)
 * окажутся в разных "координатах", и поиск будет давать бессмысленные
 * результаты, при этом без единой ошибки в логах — тихая порча
 * качества, а не крэш. При любых изменениях pooling меняйте оба места
 * одновременно.</p>
 */
public class EmbeddingService implements Closeable {

    private static final Logger logger = AppLogger.getLogger();

    private static final int MAX_SEQUENCE_LENGTH = 256;

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final HuggingFaceTokenizer tokenizer;
    private final boolean modelExpectsTokenTypeIds;

    /**
     * @param onnxModelDir папка с {@code model.onnx} и файлами токенизатора
     *                     (та же, что вывел {@code export_embedding_model.py})
     */
    public EmbeddingService(Path onnxModelDir) {
        Path modelFile = onnxModelDir.resolve("model.onnx");
        Path tokenizerFile = onnxModelDir.resolve("tokenizer.json");

        try {
            this.environment = OrtEnvironment.getEnvironment();
            this.session = environment.createSession(modelFile.toString(), new OrtSession.SessionOptions());
            this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerFile);
            this.modelExpectsTokenTypeIds = session.getInputNames().contains("token_type_ids");
        } catch (OrtException | IOException e) {
            throw new IllegalStateException("Не удалось загрузить ONNX-модель эмбеддингов из " + onnxModelDir, e);
        }

        logger.info("EmbeddingService загружен: {}", onnxModelDir);
    }

    /**
     * Считает нормализованный вектор для одного текста.
     */
    public float[] embed(String text) {
        return embedBatch(new String[]{text})[0];
    }

    /**
     * Считает нормализованные вектора для нескольких текстов за один
     * проход модели — заметно быстрее, чем по одному, при построении
     * или обновлении индекса.
     */
    public float[][] embedBatch(String[] texts) {
        Encoding[] encodings = tokenizer.batchEncode(texts);

        int batchSize = texts.length;
        int maxLen = 0;
        for (Encoding e : encodings) {
            maxLen = Math.max(maxLen, e.getIds().length);
        }
        maxLen = Math.min(maxLen, MAX_SEQUENCE_LENGTH);

        long[] inputIds = new long[batchSize * maxLen];
        long[] attentionMask = new long[batchSize * maxLen];
        long[] tokenTypeIds = modelExpectsTokenTypeIds ? new long[batchSize * maxLen] : null;

        for (int row = 0; row < batchSize; row++) {
            long[] ids = encodings[row].getIds();
            long[] mask = encodings[row].getAttentionMask();
            int len = Math.min(ids.length, maxLen);
            for (int col = 0; col < len; col++) {
                inputIds[row * maxLen + col] = ids[col];
                attentionMask[row * maxLen + col] = mask[col];
                // token_type_ids оставляем 0 — модели этого семейства
                // (single-sentence embeddings) не используют сегменты.
            }
            // Остаток строки (если текст короче maxLen) уже 0 по
            // умолчанию — это и есть паддинг с attention_mask=0,
            // ровно как ожидает pooling ниже.
        }

        try (OnnxTensor inputIdsTensor = OnnxTensor.createTensor(environment,
                LongBuffer.wrap(inputIds), new long[]{batchSize, maxLen});
             OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(environment,
                     LongBuffer.wrap(attentionMask), new long[]{batchSize, maxLen})) {

            Map<String, OnnxTensor> inputs = new java.util.HashMap<>();
            inputs.put("input_ids", inputIdsTensor);
            inputs.put("attention_mask", attentionMaskTensor);

            OnnxTensor tokenTypeTensor = null;
            if (modelExpectsTokenTypeIds) {
                tokenTypeTensor = OnnxTensor.createTensor(environment,
                        LongBuffer.wrap(tokenTypeIds), new long[]{batchSize, maxLen});
                inputs.put("token_type_ids", tokenTypeTensor);
            }

            try (OrtSession.Result result = session.run(inputs)) {
                // Первый выход модели — last_hidden_state, форма [batch, seq, hidden]
                float[][][] lastHiddenState = (float[][][]) result.get(0).getValue();
                return meanPoolAndNormalize(lastHiddenState, attentionMask, batchSize, maxLen);
            } finally {
                if (tokenTypeTensor != null) {
                    tokenTypeTensor.close();
                }
            }
        } catch (OrtException e) {
            throw new IllegalStateException("Ошибка при вычислении эмбеддингов", e);
        }
    }

    /**
     * Mean pooling с маскированием паддинга + L2-нормализация — должно
     * совпадать 1-в-1 с {@code mean_pooling}/{@code normalize} в
     * build_index_for_java.py.
     */
    private float[][] meanPoolAndNormalize(float[][][] lastHiddenState, long[] attentionMask,
                                            int batchSize, int seqLen) {
        int hiddenSize = lastHiddenState[0][0].length;
        float[][] pooled = new float[batchSize][hiddenSize];

        for (int row = 0; row < batchSize; row++) {
            float count = 0f;
            for (int col = 0; col < seqLen; col++) {
                long m = attentionMask[row * seqLen + col];
                if (m == 0) {
                    continue;
                }
                count += 1f;
                float[] tokenVector = lastHiddenState[row][col];
                for (int d = 0; d < hiddenSize; d++) {
                    pooled[row][d] += tokenVector[d];
                }
            }
            if (count < 1e-9f) {
                count = 1e-9f;
            }
            for (int d = 0; d < hiddenSize; d++) {
                pooled[row][d] /= count;
            }

            // L2-нормализация
            float norm = 0f;
            for (int d = 0; d < hiddenSize; d++) {
                norm += pooled[row][d] * pooled[row][d];
            }
            norm = (float) Math.sqrt(norm);
            if (norm < 1e-9f) {
                norm = 1e-9f;
            }
            for (int d = 0; d < hiddenSize; d++) {
                pooled[row][d] /= norm;
            }
        }

        return pooled;
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (OrtException e) {
            logger.warn("Ошибка при закрытии ONNX-сессии: {}", e.getMessage());
        }
    }
}
