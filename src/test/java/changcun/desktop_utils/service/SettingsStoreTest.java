package changcun.desktop_utils.service;

import changcun.desktop_utils.model.ShutdownConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void loadReturnsDefaultsWhenFileMissing() {
        SettingsStore store = new SettingsStore(tempDir.resolve("config.properties"));
        ShutdownConfig c = store.load();
        assertEquals(ShutdownConfig.Mode.NONE, c.getMode());
        assertEquals(-1L, c.getOnceEpochMillis());
        assertEquals(22, c.getDailyHour());
        assertEquals(0, c.getDailyMinute());
    }

    @Test
    void saveAndLoadRoundTrip() {
        SettingsStore store = new SettingsStore(tempDir.resolve("config.properties"));
        ShutdownConfig c = new ShutdownConfig();
        c.setMode(ShutdownConfig.Mode.ONCE);
        c.setOnceEpochMillis(1722000000000L);
        c.setDailyHour(23);
        c.setDailyMinute(45);
        c.setWorkdayHour(8);
        c.setWorkdayMinute(30);

        store.save(c);

        ShutdownConfig loaded = store.load();
        assertEquals(ShutdownConfig.Mode.ONCE, loaded.getMode());
        assertEquals(1722000000000L, loaded.getOnceEpochMillis());
        assertEquals(23, loaded.getDailyHour());
        assertEquals(45, loaded.getDailyMinute());
        assertEquals(8, loaded.getWorkdayHour());
        assertEquals(30, loaded.getWorkdayMinute());
    }

    @Test
    void loadInvalidModeFallsBackToNone() throws Exception {
        Path file = tempDir.resolve("config.properties");
        Files.writeString(file, "shutdown.mode=INVALID\nshutdown.daily.hour=7\n");
        SettingsStore store = new SettingsStore(file);
        ShutdownConfig c = store.load();
        assertEquals(ShutdownConfig.Mode.NONE, c.getMode());
        assertEquals(7, c.getDailyHour());
    }

    @Test
    void saveCreatesParentDirectories() {
        Path file = tempDir.resolve("nested").resolve("config.properties");
        SettingsStore store = new SettingsStore(file);
        store.save(new ShutdownConfig());
        ShutdownConfig loaded = store.load();
        assertEquals(ShutdownConfig.Mode.NONE, loaded.getMode());
    }

    @Test
    void getConfigFile() {
        Path file = tempDir.resolve("config.properties");
        SettingsStore store = new SettingsStore(file);
        assertEquals(file, store.getConfigFile());
    }
}
