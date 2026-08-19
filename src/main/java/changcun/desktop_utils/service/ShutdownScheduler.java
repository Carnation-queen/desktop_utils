package changcun.desktop_utils.service;

import changcun.desktop_utils.model.HolidayData;
import changcun.desktop_utils.model.ShutdownConfig;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 后台调度器：每隔一秒检查一次是否到达关机时间，到达后执行完全关机。
 * 线程为非守护线程，保证程序常驻后台时 JVM 不会被回收。
 */
public class ShutdownScheduler {

    private final SettingsStore store;
    private final HolidayStore holidayStore;
    private final ScheduledExecutorService executor;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private volatile ShutdownConfig config;
    private volatile boolean fired;

    /** 触发时间之后的容错窗口：只有落在这个窗口内才执行关机，避免“应用后立即关机”。 */
    private static final long TRIGGER_GRACE_MILLIS = 5_000L;

    public ShutdownScheduler(ShutdownConfig config, SettingsStore store, HolidayStore holidayStore) {
        this.config = config.copy();
        this.store = store;
        this.holidayStore = holidayStore;
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "shutdown-scheduler");
            t.setDaemon(false);
            return t;
        });
    }

    public synchronized ShutdownConfig getConfig() {
        return config.copy();
    }

    /** 更新配置并立即持久化。 */
    public synchronized void updateConfig(ShutdownConfig newConfig) {
        this.config = newConfig.copy();
        this.fired = false;
        store.save(this.config);
        notifyListeners();
    }

    /** 取消所有定时关机。 */
    public synchronized void cancel() {
        updateConfig(new ShutdownConfig());
    }

    /** 供 UI 注册状态变更监听（配置变化时刷新界面）。 */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void start() {
        executor.scheduleAtFixedRate(this::check, 1, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.shutdownNow();
    }

    private void check() {
        ShutdownConfig c = getConfig();
        long now = System.currentTimeMillis();

        switch (c.getMode()) {
            case ONCE:
                if (c.getOnceEpochMillis() > 0) {
                    if (isWithinTriggerWindow(now, c.getOnceEpochMillis())) {
                        OsShutdown.shutdownNow();
                        // 一次性任务完成后自动关闭，避免重启程序后重复关机。
                        disableOnce();
                    } else if (now >= c.getOnceEpochMillis() + TRIGGER_GRACE_MILLIS) {
                        // 已错过关机时间：自动取消，避免重启程序后误关机。
                        disableOnce();
                    }
                }
                break;
            case DAILY: {
                long today = todayTriggerMillis(c.getDailyHour(), c.getDailyMinute(), now);
                if (isWithinTriggerWindow(now, today)) {
                    if (!fired) {
                        fired = true;
                        OsShutdown.shutdownNow();
                    }
                } else {
                    // 尚未到时间或已错过今天的触发窗口，重置标记，保证下次正常触发。
                    fired = false;
                }
                break;
            }
            case WORKDAY: {
                LocalDate todayDate = LocalDate.now(ZoneId.systemDefault());
                long today = todayTriggerMillis(c.getWorkdayHour(), c.getWorkdayMinute(), now);
                if (isWithinTriggerWindow(now, today) && !isHoliday(todayDate)) {
                    if (!fired) {
                        fired = true;
                        OsShutdown.shutdownNow();
                    }
                } else {
                    // 未到时间、已错过窗口或今天为节假日：重置标记，保证后续正常触发。
                    fired = false;
                }
                break;
            }
            case NONE:
            default:
                fired = false;
                break;
        }
    }

    /** 关闭一次性任务并持久化，供“已触发”或“已错过”两种场景复用。 */
    private void disableOnce() {
        synchronized (this) {
            config.setMode(ShutdownConfig.Mode.NONE);
            config.setOnceEpochMillis(-1L);
            store.save(config);
        }
        notifyListeners();
    }

    /** 判断当前时间是否落在触发时间之后的容错窗口内。 */
    private static boolean isWithinTriggerWindow(long now, long triggerMillis) {
        return triggerMillis > 0
                && now >= triggerMillis
                && now < triggerMillis + TRIGGER_GRACE_MILLIS;
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
                // 忽略 UI 回调异常，避免影响调度。
            }
        }
    }

    private boolean isHoliday(LocalDate date) {
        return holidayStore.load().isHoliday(date);
    }

    /**
     * 计算下一次触发时间（epoch 毫秒）。无任务时返回 -1。
     */
    public long nextTriggerMillis(long nowMillis) {
        return computeNextTriggerMillis(config, nowMillis, holidayStore.load());
    }

    private static long computeNextTriggerMillis(ShutdownConfig config, long nowMillis, HolidayData holidays) {
        switch (config.getMode()) {
            case ONCE:
                return config.getOnceEpochMillis();
            case DAILY:
                return nextDailyTrigger(config.getDailyHour(), config.getDailyMinute(), nowMillis);
            case WORKDAY:
                return nextWorkdayTrigger(config.getWorkdayHour(), config.getWorkdayMinute(), nowMillis, holidays);
            case NONE:
            default:
                return -1L;
        }
    }

    private static long nextDailyTrigger(int hour, int minute, long nowMillis) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone);
        LocalDateTime target = now.toLocalDate().atTime(hour, minute);
        if (!target.isAfter(now)) {
            target = target.plusDays(1);
        }
        return target.atZone(zone).toInstant().toEpochMilli();
    }

    /**
     * 找下一个“非节假日”的触发时间（epoch 毫秒）。
     * 若没有节假日数据，则所有日期都视为非节假日。
     */
    private static long nextWorkdayTrigger(int hour, int minute, long nowMillis, HolidayData holidays) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone);
        LocalTime time = LocalTime.of(hour, minute);

        for (int i = 0; i < 370; i++) {
            LocalDate date = now.toLocalDate().plusDays(i);
            if (holidays.isHoliday(date)) {
                continue;
            }
            LocalDateTime candidate = date.atTime(time);
            if (candidate.isAfter(now)) {
                return candidate.atZone(zone).toInstant().toEpochMilli();
            }
        }
        return -1L;
    }

    /**
     * 计算“今天”指定时分的触发时间（epoch 毫秒），不考虑是否已过。
     */
    private static long todayTriggerMillis(int hour, int minute, long nowMillis) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone);
        LocalDateTime target = now.toLocalDate().atTime(hour, minute);
        return target.atZone(zone).toInstant().toEpochMilli();
    }
}
