package com.rmc.ui.workspace;

import com.rmc.http.HttpClientService;
import com.rmc.state.ApplicationState;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.rmc.ui.workspace.WorkspaceState.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "DISPLAY", matches = ".+")
class SessionNavigationTest {
    @Test
    void requiresLoginAndRestoresSearchScreensUntilAccountChange() throws Exception {
        Platform.startup(() -> {});
        var requests = new AtomicInteger();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            byte[] html = "<form id='modern_filter_form'><input type='text' name='city'></form>"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, html.length);
            try (var out = exchange.getResponseBody()) { out.write(html); }
        });
        server.start();
        try {
            var workspace = fx(() -> new WorkspaceContainer("http://127.0.0.1:" + server.getAddress().getPort()));
            fx(() -> {
                var initial = workspace.getCurrentState();
                workspace.showParamsSection();
                workspace.showCooperatesSection();
                workspace.runAnalysis("", List.of());
                assertEquals(initial, workspace.getCurrentState());
                assertEquals(0, requests.get());
                ApplicationState.getInstance().login("test", HttpClientService.builder().build());
                workspace.onLoginSuccess("test");
                return null;
            });
            awaitScreen(workspace, FILTERS_READY);
            fx(() -> {
                workspace.getFiltersView().getFilterSession().setValue("city", "A");
                workspace.showCooperatesSection();
                return null;
            });
            awaitScreen(workspace, COOPERATES_FILTERS_READY);
            fx(() -> {
                workspace.runCooperatesAnalysis("", List.of());
                workspace.showRegistryHome();
                assertEquals("A", workspace.getFiltersView().getFilterSession().getValue("city"));
                workspace.showCooperatesSection();
                return null;
            });
            awaitScreen(workspace, COOPERATES_RESULTS);
            fx(() -> {
                workspace.showRegistryHome();
                workspace.showCooperatesSection();
                assertEquals(COOPERATES_RESULTS, workspace.getCurrentState());
                workspace.showCooperatesFilters();
                assertEquals(COOPERATES_FILTERS_READY, workspace.getCurrentState());
                assertEquals(3, requests.get());
                workspace.showAccountPicker();
                workspace.onAuthStateChanged();
                workspace.showParamsSection();
                assertFalse(ApplicationState.getInstance().isAuthenticated());
                assertNotEquals("A", workspace.getFiltersView().getFilterSession().getValue("city"));
                return null;
            });
        } finally {
            server.stop(0);
            fx(() -> { ApplicationState.getInstance().logout(); return null; });
        }
    }

    private static void awaitScreen(WorkspaceContainer workspace, WorkspaceState expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (fx(workspace::getCurrentState) != expected && System.nanoTime() < deadline) Thread.sleep(20);
        assertEquals(expected, fx(workspace::getCurrentState));
    }

    private static <T> T fx(Callable<T> action) throws Exception {
        var result = new FutureTask<>(action);
        Platform.runLater(result);
        return result.get(15, TimeUnit.SECONDS);
    }
}
