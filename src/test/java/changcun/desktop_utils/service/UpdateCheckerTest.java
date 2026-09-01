package changcun.desktop_utils.service;

import changcun.desktop_utils.model.AppSettings;
import changcun.desktop_utils.model.UpdateCheckResult;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateCheckerTest {

    private HttpServer server;
    private String responseBody = "{}";
    private int responseStatus = 200;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/latest";
    }

    private static String releaseJson(String tagName, String downloadUrl) {
        return "{"
                + "\"tag_name\":\"" + tagName + "\","
                + "\"html_url\":\"https://github.com/Carnation-queen/desktop_utils/releases\","
                + "\"body\":\"修复若干问题\","
                + "\"assets\":[{\"browser_download_url\":\"" + downloadUrl + "\"}]"
                + "}";
    }

    @Test
    void constructorUsesDefaultUrlWhenBlank() {
        UpdateChecker checker = new UpdateChecker("   ");
        assertEquals(AppSettings.DEFAULT_UPDATE_URL, checker.getUpdateUrl());
    }

    @Test
    void constructorUsesDefaultUrlWhenNull() {
        UpdateChecker checker = new UpdateChecker(null);
        assertEquals(AppSettings.DEFAULT_UPDATE_URL, checker.getUpdateUrl());
    }

    @Test
    void constructorTrimsUrl() {
        UpdateChecker checker = new UpdateChecker("  http://example.com/latest  ");
        assertEquals("http://example.com/latest", checker.getUpdateUrl());
    }

    @Test
    void checkForUpdateReportsNewerVersion() {
        // 用“当前版本 + .1”构造一个必然更新的版本，避免与项目版本号耦合。
        String current = AppVersion.current();
        String newer = current + ".1";
        responseBody = releaseJson("v" + newer,
                "https://github.com/Carnation-queen/desktop_utils/releases/download/v"
                        + newer + "/desktop_utils.exe");

        UpdateCheckResult result = new UpdateChecker(url()).checkForUpdate();

        assertEquals(UpdateCheckResult.Status.UPDATE_AVAILABLE, result.getStatus());
        assertNotNull(result.getLatest());
        assertEquals(newer, result.getLatest().getVersion());
        assertEquals(
                "https://github.com/Carnation-queen/desktop_utils/releases/download/v"
                        + newer + "/desktop_utils.exe",
                result.getLatest().getDownloadUrl());
        assertNotNull(result.getCurrentVersion());
    }

    @Test
    void checkForUpdateReportsUpToDateWhenSameVersion() {
        String current = AppVersion.current();
        responseBody = releaseJson(current, "https://example.com/app.exe");

        UpdateCheckResult result = new UpdateChecker(url()).checkForUpdate();

        assertEquals(UpdateCheckResult.Status.UP_TO_DATE, result.getStatus());
    }

    @Test
    void checkForUpdateReportsUpToDateWhenOlderVersion() {
        responseBody = releaseJson("0.9.0", "https://example.com/app.exe");

        UpdateCheckResult result = new UpdateChecker(url()).checkForUpdate();

        assertEquals(UpdateCheckResult.Status.UP_TO_DATE, result.getStatus());
    }

    @Test
    void checkForUpdateReturnsErrorOn404() {
        responseStatus = 404;
        responseBody = "{\"message\":\"Not Found\"}";

        UpdateCheckResult result = new UpdateChecker(url()).checkForUpdate();

        assertEquals(UpdateCheckResult.Status.ERROR, result.getStatus());
        assertTrue(result.getErrorMessage().contains("404"));
    }

    @Test
    void checkForUpdateReturnsErrorOnInvalidJson() {
        responseBody = "not a json";

        UpdateCheckResult result = new UpdateChecker(url()).checkForUpdate();

        assertEquals(UpdateCheckResult.Status.ERROR, result.getStatus());
        assertFalse(result.getErrorMessage().isBlank());
    }

    @Test
    void checkForUpdateReturnsErrorOnMissingTagName() {
        responseBody = "{\"html_url\":\"https://example.com\"}";

        UpdateCheckResult result = new UpdateChecker(url()).checkForUpdate();

        assertEquals(UpdateCheckResult.Status.ERROR, result.getStatus());
    }
}
