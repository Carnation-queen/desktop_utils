package changcun.desktop_utils.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppSettingsTest {

    @Test
    void defaultValues() {
        AppSettings settings = new AppSettings();
        assertFalse(settings.isAutoStart());
        assertTrue(settings.isAutoUpdate());
        assertEquals(AppSettings.DEFAULT_UPDATE_URL, settings.getUpdateUrl());
    }

    @Test
    void setAndGetAutoStart() {
        AppSettings settings = new AppSettings();
        settings.setAutoStart(true);
        assertTrue(settings.isAutoStart());
    }

    @Test
    void setAndGetAutoUpdate() {
        AppSettings settings = new AppSettings();
        settings.setAutoUpdate(false);
        assertFalse(settings.isAutoUpdate());
    }

    @Test
    void setUpdateUrlTrimsValue() {
        AppSettings settings = new AppSettings();
        settings.setUpdateUrl("  https://example.com/latest  ");
        assertEquals("https://example.com/latest", settings.getUpdateUrl());
    }

    @Test
    void setUpdateUrlNullFallsBackToDefault() {
        AppSettings settings = new AppSettings();
        settings.setUpdateUrl(null);
        assertEquals(AppSettings.DEFAULT_UPDATE_URL, settings.getUpdateUrl());
    }

    @Test
    void setUpdateUrlBlankFallsBackToDefault() {
        AppSettings settings = new AppSettings();
        settings.setUpdateUrl("   ");
        assertEquals(AppSettings.DEFAULT_UPDATE_URL, settings.getUpdateUrl());
    }

    @Test
    void getUpdateUrlFallsBackWhenInternalStateIsNull() throws Exception {
        AppSettings settings = new AppSettings();
        // 通过反射置空内部状态，验证 getter 的防御性兜底逻辑
        java.lang.reflect.Field field = AppSettings.class.getDeclaredField("updateUrl");
        field.setAccessible(true);
        field.set(settings, null);
        assertEquals(AppSettings.DEFAULT_UPDATE_URL, settings.getUpdateUrl());
    }
}
