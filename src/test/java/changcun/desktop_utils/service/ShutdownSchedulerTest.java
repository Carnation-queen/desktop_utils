package changcun.desktop_utils.service;

import changcun.desktop_utils.model.HolidayData;
import changcun.desktop_utils.model.ShutdownConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShutdownSchedulerTest {

    @TempDir
    Path tempDir;

    private SettingsStore settingsStore() {
        return new SettingsStore(tempDir.resolve("config.properties"));
    }

    private HolidayStore holidayStore() {
        return new HolidayStore(tempDir.resolve("holidays.properties"));
    }

    private static long millis(int y, int mo, int d, int h, int mi) {
        return LocalDateTime.of(y, mo, d, h, mi)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    @Test
    void nextTriggerMillisNoneReturnsMinusOne() {
        ShutdownScheduler scheduler = new ShutdownScheduler(new ShutdownConfig(),
                settingsStore(), holidayStore());
        try {
            assertEquals(-1L, scheduler.nextTriggerMillis(millis(2026, 1, 1, 10, 0)));
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void nextTriggerMillisOnceReturnsOnceEpoch() {
        ShutdownConfig cfg = new ShutdownConfig();
        cfg.setMode(ShutdownConfig.Mode.ONCE);
        long once = millis(2026, 12, 31, 23, 59);
        cfg.setOnceEpochMillis(once);

        ShutdownScheduler scheduler = new ShutdownScheduler(cfg, settingsStore(), holidayStore());
        try {
            assertEquals(once, scheduler.nextTriggerMillis(millis(2026, 1, 1, 0, 0)));
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void nextTriggerMillisDailyLaterToday() {
        ShutdownConfig cfg = new ShutdownConfig();
        cfg.setMode(ShutdownConfig.Mode.DAILY);
        cfg.setDailyHour(22);
        cfg.setDailyMinute(30);

        ShutdownScheduler scheduler = new ShutdownScheduler(cfg, settingsStore(), holidayStore());
        try {
            long now = millis(2026, 1, 1, 10, 0);
            long expected = millis(2026, 1, 1, 22, 30);
            assertEquals(expected, scheduler.nextTriggerMillis(now));
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void nextTriggerMillisDailyRollsToTomorrowWhenPassed() {
        ShutdownConfig cfg = new ShutdownConfig();
        cfg.setMode(ShutdownConfig.Mode.DAILY);
        cfg.setDailyHour(22);
        cfg.setDailyMinute(30);

        ShutdownScheduler scheduler = new ShutdownScheduler(cfg, settingsStore(), holidayStore());
        try {
            long now = millis(2026, 1, 1, 23, 0);
            long expected = millis(2026, 1, 2, 22, 30);
            assertEquals(expected, scheduler.nextTriggerMillis(now));
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void nextTriggerMillisWorkdaySkipsHoliday() {
        HolidayData holidays = new HolidayData();
        holidays.setYear(2026);
        holidays.addDate(LocalDate.of(2026, 1, 1));
        holidayStore().save(holidays);

        ShutdownConfig cfg = new ShutdownConfig();
        cfg.setMode(ShutdownConfig.Mode.WORKDAY);
        cfg.setWorkdayHour(8);
        cfg.setWorkdayMinute(0);

        ShutdownScheduler scheduler = new ShutdownScheduler(cfg, settingsStore(), holidayStore());
        try {
            long now = millis(2026, 1, 1, 9, 0);
            // 1月1日为节假日且 8:00 已过，下一个非节假日触发时间为 1月2日 8:00
            long expected = millis(2026, 1, 2, 8, 0);
            assertEquals(expected, scheduler.nextTriggerMillis(now));
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void nextTriggerMillisWorkdayWithoutHolidayData() {
        ShutdownConfig cfg = new ShutdownConfig();
        cfg.setMode(ShutdownConfig.Mode.WORKDAY);
        cfg.setWorkdayHour(8);
        cfg.setWorkdayMinute(0);

        ShutdownScheduler scheduler = new ShutdownScheduler(cfg, settingsStore(), holidayStore());
        try {
            long now = millis(2026, 1, 1, 9, 0);
            // 无节假日数据，当天 8:00 已过，返回次日 8:00
            long expected = millis(2026, 1, 2, 8, 0);
            assertEquals(expected, scheduler.nextTriggerMillis(now));
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void updateConfigPersists() {
        ShutdownScheduler scheduler = new ShutdownScheduler(new ShutdownConfig(),
                settingsStore(), holidayStore());
        try {
            ShutdownConfig newCfg = new ShutdownConfig();
            newCfg.setMode(ShutdownConfig.Mode.DAILY);
            newCfg.setDailyHour(21);
            scheduler.updateConfig(newCfg);

            assertEquals(ShutdownConfig.Mode.DAILY, scheduler.getConfig().getMode());
            assertEquals(21, scheduler.getConfig().getDailyHour());
            assertEquals(ShutdownConfig.Mode.DAILY, settingsStore().load().getMode());
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void cancelResetsToNone() {
        ShutdownConfig cfg = new ShutdownConfig();
        cfg.setMode(ShutdownConfig.Mode.DAILY);
        ShutdownScheduler scheduler = new ShutdownScheduler(cfg, settingsStore(), holidayStore());
        try {
            scheduler.cancel();
            assertEquals(ShutdownConfig.Mode.NONE, scheduler.getConfig().getMode());
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void getConfigReturnsIndependentCopy() {
        ShutdownScheduler scheduler = new ShutdownScheduler(new ShutdownConfig(),
                settingsStore(), holidayStore());
        try {
            ShutdownConfig copy = scheduler.getConfig();
            copy.setMode(ShutdownConfig.Mode.DAILY);
            assertEquals(ShutdownConfig.Mode.NONE, scheduler.getConfig().getMode());
        } finally {
            scheduler.stop();
        }
    }

    @Test
    void updateConfigNotifiesListeners() {
        ShutdownScheduler scheduler = new ShutdownScheduler(new ShutdownConfig(),
                settingsStore(), holidayStore());
        AtomicBoolean notified = new AtomicBoolean(false);
        scheduler.addListener(() -> notified.set(true));
        try {
            scheduler.updateConfig(new ShutdownConfig());
            assertTrue(notified.get());
        } finally {
            scheduler.stop();
        }
    }
}
