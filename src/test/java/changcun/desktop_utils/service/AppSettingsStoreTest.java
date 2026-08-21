package changcun.desktop_utils.service;

import changcun.desktop_utils.model.AppSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppSettingsStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void loadReturnsDefaultsWhenFileMissing() {
        AppSettingsStore store = new AppSettingsStore(tempDir.resolve("app.properties"));
        AppSettings settings = store.load();
        assertFalse(settings.isAutoStart());
        assertTrue(settings.isAutoUpdate());
        assertEquals(AppSettings.DEFAULT_UPDATE_URL, settings.getUpdateUrl());
    }

    @Test
    void saveAndLoadRoundTrip() {
        AppSettingsStore store = new AppSettingsStore(tempDir.resolve("app.properties"));
        AppSettings settings = new AppSettings();
        settings.setAutoStart(true);
        settings.setAutoUpdate(false);
        settings.setUpdateUrl("https://example.com/latest");

        store.save(settings);

        AppSettings loaded = store.load();
        assertTrue(loaded.isAutoStart());
        assertFalse(loaded.isAutoUpdate());
        assertEquals("https://example.com/latest", loaded.getUpdateUrl());
    }

    @Test
    void saveCreatesParentDirectories() {
        Path file = tempDir.resolve("nested").resolve("dir").resolve("app.properties");
        AppSettingsStore store = new AppSettingsStore(file);
        store.save(new AppSettings());
        assertTrue(Files.exists(file));
    }

    @Test
    void getConfigFile() {
        Path file = tempDir.resolve("app.properties");
        AppSettingsStore store = new AppSettingsStore(file);
        assertEquals(file, store.getConfigFile());
    }

    @Test
    void loadFallsBackWhenFileIsCorrupt() throws Exception {
        Path file = tempDir.resolve("app.properties");
        Files.write(file, new byte[]{0x00, 0x01, 0x02}); // 无效内容仍可被 Properties.load 忽略
        AppSettingsStore store = new AppSettingsStore(file);
        AppSettings settings = store.load();
        // 无论文件内容如何，load 都不应抛异常
        assertFalse(settings.isAutoStart());
    }
}
