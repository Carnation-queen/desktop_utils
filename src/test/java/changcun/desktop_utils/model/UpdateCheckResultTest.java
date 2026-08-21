package changcun.desktop_utils.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UpdateCheckResultTest {

    private static UpdateInfo info(String version) {
        UpdateInfo i = new UpdateInfo();
        i.setVersion(version);
        return i;
    }

    @Test
    void upToDate() {
        UpdateInfo latest = info("1.0.1");
        UpdateCheckResult r = UpdateCheckResult.upToDate("1.0.1", latest);
        assertEquals(UpdateCheckResult.Status.UP_TO_DATE, r.getStatus());
        assertEquals("1.0.1", r.getCurrentVersion());
        assertEquals(latest, r.getLatest());
        assertNull(r.getErrorMessage());
    }

    @Test
    void updateAvailable() {
        UpdateInfo latest = info("1.0.2");
        UpdateCheckResult r = UpdateCheckResult.updateAvailable("1.0.1", latest);
        assertEquals(UpdateCheckResult.Status.UPDATE_AVAILABLE, r.getStatus());
        assertEquals("1.0.1", r.getCurrentVersion());
        assertEquals(latest, r.getLatest());
        assertNull(r.getErrorMessage());
    }

    @Test
    void error() {
        UpdateCheckResult r = UpdateCheckResult.error("网络错误");
        assertEquals(UpdateCheckResult.Status.ERROR, r.getStatus());
        assertNull(r.getCurrentVersion());
        assertNull(r.getLatest());
        assertEquals("网络错误", r.getErrorMessage());
    }
}
