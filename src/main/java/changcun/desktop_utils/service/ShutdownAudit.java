package changcun.desktop_utils.service;

import changcun.desktop_utils.model.ShutdownConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 关机审计日志入口。
 * <p>
 * 把“程序启动 / 退出、定时关机配置变更、实际触发关机、错过任务”等关键事件，统一写入
 * {@code ~/.desktop_utils/logs/shutdown.log}（由 {@code logback.xml} 配置，时间精确到毫秒）。
 * </p>
 * <p>
 * 该日志在普通界面没有任何入口，仅当用户在主窗口内按下 <b>F12</b> 时才会在独立窗口中展示，
 * 用于事后追查定时关机到底有没有执行、何时执行、因何取消/错过。
 * </p>
 * <p>
 * 所有方法均为“尽力而为”：即使底层日志不可用也不会抛出异常，不影响主流程。
 * </p>
 */
public final class ShutdownAudit {

    /** 必须与 logback.xml 中配置的 logger name 保持一致。 */
    public static final String LOGGER_NAME = "ShutdownAudit";

    /** 展示用的毫秒精度时间格式（本地时区），如 2026-09-07 22:00:00.123。 */
    private static final DateTimeFormatter MS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final Logger LOG = LoggerFactory.getLogger(LOGGER_NAME);

    private ShutdownAudit() {
    }

    /** 把 epoch 毫秒格式化为本地时区的「yyyy-MM-dd HH:mm:ss.SSS」字符串。 */
    public static String formatMillis(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .format(MS_FORMAT);
    }

    /** 程序启动。 */
    public static void startup(String version, String os, String javaVersion) {
        info("程序启动  版本=" + version + " 系统=" + os + " Java=" + javaVersion);
    }

    /** 用户从托盘完全退出程序。 */
    public static void exit(String reason) {
        info("程序退出  原因=" + reason);
    }

    /** 审计日志在查看窗口中被手动清空（清空后在空文件中写入起始标记）。 */
    public static void cleared() {
        info("审计日志已被清空，以下为清空后的新记录");
    }

    /** 定时关机调度器开始运行（含当前生效的配置摘要）。 */
    public static void schedulerStarted(ShutdownConfig config) {
        info("调度器启动 当前配置=" + describe(config));
    }

    /** 定时关机调度器停止（程序退出前）。 */
    public static void schedulerStopped() {
        info("调度器停止");
    }

    /** 定时关机配置被更新/保存。 */
    public static void configUpdated(ShutdownConfig config) {
        info("配置更新  新配置=" + describe(config));
    }

    /** 定时关机被用户取消（记录取消前的旧配置）。 */
    public static void configCancelled(ShutdownConfig oldConfig) {
        info("配置取消  原配置=" + describe(oldConfig));
    }

    /** 实际触发关机：已向系统发出关机命令。scheduledMillis 为原定的触发时刻(epoch ms)。 */
    public static void triggered(ShutdownConfig.Mode mode, long scheduledMillis, long actualMillis) {
        info("【触发关机】模式=" + modeLabel(mode)
                + " 原定触发=" + formatMillis(scheduledMillis)
                + "(epoch=" + scheduledMillis + ")"
                + " 实际触发=" + formatMillis(actualMillis)
                + "(epoch=" + actualMillis + ")"
                + " 已发出系统关机命令");
    }

    /** 一次性任务已错过触发窗口，自动取消（避免重启后误关机）。 */
    public static void missedOnce(long scheduledMillis, long actualMillis) {
        info("【错过任务】一次性关机已过触发时间，自动取消"
                + " 原定触发=" + formatMillis(scheduledMillis)
                + "(epoch=" + scheduledMillis + ")"
                + " 检查时间=" + formatMillis(actualMillis)
                + "(epoch=" + actualMillis + ")");
    }

    /** 生成当前生效配置的中文摘要。 */
    public static String describe(ShutdownConfig config) {
        if (config == null) {
            return "空";
        }
        switch (config.getMode()) {
            case ONCE:
                return "一次性关机，设定触发=" + formatMillis(config.getOnceEpochMillis())
                        + "(epoch=" + config.getOnceEpochMillis() + ")";
            case DAILY:
                return "每天关机，设定时间=" + timeText(config.getDailyHour(), config.getDailyMinute());
            case WORKDAY:
                return "非节假日(工作日)关机，设定时间="
                        + timeText(config.getWorkdayHour(), config.getWorkdayMinute());
            case NONE:
            default:
                return "已关闭";
        }
    }

    private static String modeLabel(ShutdownConfig.Mode mode) {
        if (mode == null) {
            return "未知";
        }
        switch (mode) {
            case ONCE:
                return "一次性";
            case DAILY:
                return "每天";
            case WORKDAY:
                return "工作日(非节假日)";
            case NONE:
            default:
                return "已关闭";
        }
    }

    private static String timeText(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }

    private static void info(String message) {
        try {
            LOG.info("{}", message);
        } catch (RuntimeException ignored) {
            // 日志不可用时静默，绝不影响关机主流程。
        }
    }
}
