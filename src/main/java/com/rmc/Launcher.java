package com.rmc;

/**
 * Отдельная точка входа, которая сама НЕ наследует
 * {@code javafx.application.Application}.
 *
 * <p>Это не лишний класс — без него приложение не запускается из
 * classpath-jar (в том числе из установленного через jpackage
 * приложения). Если main-class сам наследует {@code Application}
 * (как {@link Main}), при запуске JVM выполняет специальную проверку:
 * обнаружив, что точка входа — это JavaFX Application, она требует,
 * чтобы JavaFX был доступен именно как именованный модуль
 * (--module-path/--add-modules), а не просто как библиотека на
 * classpath — даже если все нужные jar-файлы физически присутствуют
 * на classpath. В результате падает с ошибкой:</p>
 *
 * <pre>
 * Error: JavaFX runtime components are missing, and are required to run this application
 * </pre>
 *
 * <p>Обходной путь стандартный и хорошо документированный: точка входа
 * (main-class в манифесте / jpackage --main-class) не должна сама
 * наследовать {@code Application} — она должна быть отдельным простым
 * классом, который лишь вызывает запуск настоящего JavaFX-приложения.
 * Тогда эта специальная проверка не срабатывает, и JavaFX благополучно
 * подгружается как обычные classpath-библиотеки.</p>
 */
public class Launcher {
    
    public static void main(String[] args) {
        Main.main(args);
    }
}
