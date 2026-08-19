package changcun.desktop_utils.model;

/**
 * 定时关机配置。四种模式：
 * <ul>
 *   <li>NONE —— 关闭定时关机</li>
 *   <li>ONCE —— 在指定的具体日期时间关机一次</li>
 *   <li>DAILY —— 每天在指定的时分关机</li>
 *   <li>WORKDAY —— 仅在非节假日（工作日）的指定时分关机</li>
 * </ul>
 */
public class ShutdownConfig {

    public enum Mode { NONE, ONCE, DAILY, WORKDAY }

    private Mode mode = Mode.NONE;
    /** ONCE 模式的触发时间（epoch 毫秒）。 */
    private long onceEpochMillis = -1L;
    /** DAILY 模式的触发小时（0-23）。 */
    private int dailyHour = 22;
    /** DAILY 模式的触发分钟（0-59）。 */
    private int dailyMinute = 0;
    /** WORKDAY（非节假日）模式的触发小时（0-23）。 */
    private int workdayHour = 22;
    /** WORKDAY（非节假日）模式的触发分钟（0-59）。 */
    private int workdayMinute = 0;

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode == null ? Mode.NONE : mode;
    }

    public long getOnceEpochMillis() {
        return onceEpochMillis;
    }

    public void setOnceEpochMillis(long onceEpochMillis) {
        this.onceEpochMillis = onceEpochMillis;
    }

    public int getDailyHour() {
        return dailyHour;
    }

    public void setDailyHour(int dailyHour) {
        this.dailyHour = dailyHour;
    }

    public int getDailyMinute() {
        return dailyMinute;
    }

    public void setDailyMinute(int dailyMinute) {
        this.dailyMinute = dailyMinute;
    }

    public int getWorkdayHour() {
        return workdayHour;
    }

    public void setWorkdayHour(int workdayHour) {
        this.workdayHour = workdayHour;
    }

    public int getWorkdayMinute() {
        return workdayMinute;
    }

    public void setWorkdayMinute(int workdayMinute) {
        this.workdayMinute = workdayMinute;
    }

    /** 返回一份独立的副本，避免外部修改内部状态。 */
    public ShutdownConfig copy() {
        ShutdownConfig c = new ShutdownConfig();
        c.mode = this.mode;
        c.onceEpochMillis = this.onceEpochMillis;
        c.dailyHour = this.dailyHour;
        c.dailyMinute = this.dailyMinute;
        c.workdayHour = this.workdayHour;
        c.workdayMinute = this.workdayMinute;
        return c;
    }
}
