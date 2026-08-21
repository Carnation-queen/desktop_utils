package changcun.desktop_utils.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ShutdownConfigTest {

    @Test
    void defaultValues() {
        ShutdownConfig c = new ShutdownConfig();
        assertEquals(ShutdownConfig.Mode.NONE, c.getMode());
        assertEquals(-1L, c.getOnceEpochMillis());
        assertEquals(22, c.getDailyHour());
        assertEquals(0, c.getDailyMinute());
        assertEquals(22, c.getWorkdayHour());
        assertEquals(0, c.getWorkdayMinute());
    }

    @Test
    void setModeNullFallsBackToNone() {
        ShutdownConfig c = new ShutdownConfig();
        c.setMode(ShutdownConfig.Mode.DAILY);
        c.setMode(null);
        assertEquals(ShutdownConfig.Mode.NONE, c.getMode());
    }

    @Test
    void setAndGetFields() {
        ShutdownConfig c = new ShutdownConfig();
        c.setMode(ShutdownConfig.Mode.ONCE);
        c.setOnceEpochMillis(123456789L);
        c.setDailyHour(23);
        c.setDailyMinute(59);
        c.setWorkdayHour(8);
        c.setWorkdayMinute(30);

        assertEquals(ShutdownConfig.Mode.ONCE, c.getMode());
        assertEquals(123456789L, c.getOnceEpochMillis());
        assertEquals(23, c.getDailyHour());
        assertEquals(59, c.getDailyMinute());
        assertEquals(8, c.getWorkdayHour());
        assertEquals(30, c.getWorkdayMinute());
    }

    @Test
    void copyProducesEqualButIndependentObject() {
        ShutdownConfig c = new ShutdownConfig();
        c.setMode(ShutdownConfig.Mode.WORKDAY);
        c.setOnceEpochMillis(42L);
        c.setDailyHour(19);
        c.setDailyMinute(15);
        c.setWorkdayHour(7);
        c.setWorkdayMinute(5);

        ShutdownConfig copy = c.copy();
        assertNotSame(c, copy);
        assertEquals(c.getMode(), copy.getMode());
        assertEquals(c.getOnceEpochMillis(), copy.getOnceEpochMillis());
        assertEquals(c.getDailyHour(), copy.getDailyHour());
        assertEquals(c.getDailyMinute(), copy.getDailyMinute());
        assertEquals(c.getWorkdayHour(), copy.getWorkdayHour());
        assertEquals(c.getWorkdayMinute(), copy.getWorkdayMinute());
    }

    @Test
    void mutatingCopyDoesNotAffectOriginal() {
        ShutdownConfig c = new ShutdownConfig();
        c.setMode(ShutdownConfig.Mode.DAILY);

        ShutdownConfig copy = c.copy();
        copy.setMode(ShutdownConfig.Mode.NONE);
        copy.setDailyHour(1);

        assertEquals(ShutdownConfig.Mode.DAILY, c.getMode());
        assertEquals(22, c.getDailyHour());
    }
}
