package changcun.desktop_utils.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppVersionTest {

    @Test
    void currentReturnsNonBlankVersion() {
        String v = AppVersion.current();
        assertFalse(v.isBlank());
        assertFalse(v.contains("${"));
    }

    @Test
    void compareEqual() {
        assertEquals(0, AppVersion.compare("1.0.1", "1.0.1"));
        assertEquals(0, AppVersion.compare("v1.0.1", "1.0.1"));
        assertEquals(0, AppVersion.compare("1.0", "1.0.0"));
    }

    @Test
    void compareGreater() {
        assertTrue(AppVersion.compare("1.0.2", "1.0.1") > 0);
        assertTrue(AppVersion.compare("2.0", "1.9.9") > 0);
        assertTrue(AppVersion.compare("1.10.0", "1.2.0") > 0);
    }

    @Test
    void compareLess() {
        assertTrue(AppVersion.compare("1.0.1", "1.0.2") < 0);
        assertTrue(AppVersion.compare("1.9.9", "2.0") < 0);
        assertTrue(AppVersion.compare("1.0.1", "1.0.10") < 0);
    }

    @Test
    void compareIgnoresPrefixAndPrereleaseSuffix() {
        assertEquals(0, AppVersion.compare("V1.0.1", "1.0.1"));
        assertEquals(0, AppVersion.compare("1.0.1-beta", "1.0.1"));
        assertEquals(0, AppVersion.compare("1.0.1-rc.2", "1.0.1"));
    }

    @Test
    void compareWithNull() {
        assertTrue(AppVersion.compare(null, "1.0.0") < 0);
        assertTrue(AppVersion.compare("1.0.0", null) > 0);
        assertEquals(0, AppVersion.compare(null, null));
    }

    @Test
    void compareTreatsNonNumericPartsAsZero() {
        // 非数字部分会被去除，例如 "1.x" -> "1.0"
        assertEquals(0, AppVersion.compare("1.x", "1.0"));
    }

    @Test
    void currentIsDeterministicAcrossCalls() {
        assertEquals(AppVersion.current(), AppVersion.current());
        assertNotEquals("", AppVersion.current());
    }
}
