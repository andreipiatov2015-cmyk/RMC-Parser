package com.rmc.ai;

import java.nio.file.Path;
import java.util.List;

/**
 * Автономная проверка моста Java ↔ модель — без UI, из командной строки.
 * Аналог query.py, но полностью на Java: эмбеддинг вопроса считается
 * через ONNX Runtime внутри JVM, поиск — простым перебором по индексу.
 *
 * <p>Запуск:</p>
 * <pre>
 * java -cp target/classes com.rmc.ai.RagQueryRunner &lt;путь_к_onnx_модели&gt; &lt;путь_к_индексу&gt; "вопрос"
 * </pre>
 *
 * <p>Например:</p>
 * <pre>
 * java -cp target/classes com.rmc.ai.RagQueryRunner D:/model_onnx data/index_java "как получить сертификат"
 * </pre>
 */
public final class RagQueryRunner {

    private RagQueryRunner() {
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Использование: RagQueryRunner <путь_к_onnx_модели> <путь_к_индексу> \"вопрос\"");
            return;
        }

        Path onnxModelDir = Path.of(args[0]);
        Path indexDir = Path.of(args[1]);
        String question = args[2];

        System.out.println("Загрузка модели и индекса...");
        try (RagSearchService service = new RagSearchService(onnxModelDir, indexDir)) {
            System.out.println("Готово.\n");
            System.out.println("Вопрос: " + question);
            System.out.println("=".repeat(70));

            RagSearchService.Answer answer = service.search(question, 5);

            if (!answer.confident) {
                System.out.println("Уверенного совпадения нет — в реальном UI здесь будет");
                System.out.println("предложение пользователю собрать данные по разделу подробнее.");
                System.out.println("=".repeat(70));
            }

            List<VectorIndex.SearchResult> results = answer.results;
            for (int i = 0; i < results.size(); i++) {
                VectorIndex.SearchResult r = results.get(i);
                System.out.printf("%n[%d] score=%.3f%n", i + 1, r.score);
                if (!r.chunk.title.isEmpty()) {
                    System.out.println("    Заголовок: " + r.chunk.title);
                }
                if (!r.chunk.url.isEmpty()) {
                    System.out.println("    URL: " + r.chunk.url);
                }
                System.out.println("    Файл: " + r.chunk.sourceFile);
                String snippet = r.chunk.text.length() > 300 ? r.chunk.text.substring(0, 300) : r.chunk.text;
                System.out.println("    Текст: " + snippet + "...");
            }

            System.out.println("\n" + "=".repeat(70));
        } catch (Exception e) {
            System.out.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
