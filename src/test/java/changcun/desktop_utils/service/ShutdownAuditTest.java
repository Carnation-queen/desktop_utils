package changcun.desktop_utils.service;

import changcun.desktop_utils.model.ShutdownConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 关机审计日志的纯逻辑测试：描述摘要与毫秒格式化（不涉及实际写日志文件，
 * 测试运行期由 src/test/resources/logback-test.xml 将日志静默）。
 */
class ShutdownAuditTest {

    @Test
    void describeNoneSaysDisabled() {
        String text = ShutdownAudit.describe(new ShutdownConfig());
        assertNotNull(text);
        assertTrue(text.contains("已关闭"), text);
    }

    @Test
    void describeDailyContainsTime() {
        ShutdownConfig cfg = new ShutdownConfig();
        cfg.setMode(ShutdownConfig.Mode.DAILY);
        cfg.setDailyHour(22);
        cfg.setDailyMinute(5);
        String text = ShutdownAudit.describe(cfg);
        assertTrue(text.contains("每天关机"), text);
        assertTrue(text.contains("22:05"), text);
    }

    @Test
    void describeWorkdayContainsTime() {
        ShutdownConfig cfg = new ShutdownConfig();
        cfg.setMode(ShutdownConfig.Mode.WORKDAY);
        cfg.setWorkdayHour(8);
        cfg.setWorkdayMinute(30);
        String text = ShutdownAudit.describe(cfg);
        assertTrue(text.contains("工作日"), text);
        assertTrue(text.contains("08:30"), text);
    }

    @Test
    void describeOnceContainsEpoch() {
        ShutdownConfig cfg = new ShutdownConfig();
        cfg.setMode(ShutdownConfig.Mode.ONCE);
        cfg.setOnceEpochMillis(1_700_000_000_000L);
        String text = ShutdownAudit.describe(cfg);
        assertTrue(text.contains("一次性关机"), text);
        assertTrue(text.contains("epoch=1700000000000"), text);
    }

    @Test
    void formatMillisIsMillisecondPrecision() {
        // 任意固定 epoch 毫秒，应格式化为 yyyy-MM-dd HH:mm:ss.SSS（共 23 字符）。
        String text = ShutdownAudit.formatMillis(1_700_000_000_123L);
        assertTrue(text.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"),
                "非法格式: " + text);
        assertTrue(text.endsWith(".123"), text);
        assertFalse(text.contains("T"), "不应包含 ISO 的 T 分隔符: " + text);
    }

    @Test
    void describeNullIsSafe() {
        String text = ShutdownAudit.describe(null);
        assertNotNull(text);
        assertTrue(text.contains("空"), text);
    }
}
