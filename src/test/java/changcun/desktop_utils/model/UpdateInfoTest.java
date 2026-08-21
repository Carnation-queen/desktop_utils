package changcun.desktop_utils.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UpdateInfoTest {

    @Test
    void defaultsAreNull() {
        UpdateInfo info = new UpdateInfo();
        assertNull(info.getVersion());
        assertNull(info.getDownloadUrl());
        assertNull(info.getPageUrl());
        assertNull(info.getNotes());
    }

    @Test
    void setAndGetAllFields() {
        UpdateInfo info = new UpdateInfo();
        info.setVersion("1.2.3");
        info.setDownloadUrl("https://example.com/app.exe");
        info.setPageUrl("https://github.com/x/releases");
        info.setNotes("修复若干问题");

        assertEquals("1.2.3", info.getVersion());
        assertEquals("https://example.com/app.exe", info.getDownloadUrl());
        assertEquals("https://github.com/x/releases", info.getPageUrl());
        assertEquals("修复若干问题", info.getNotes());
    }
}
