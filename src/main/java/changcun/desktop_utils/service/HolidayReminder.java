package changcun.desktop_utils.service;

import changcun.desktop_utils.ui.HolidayPanel;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.time.LocalDate;
import java.time.Month;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 年度节假日导入提醒：
 * <ul>
 *   <li>每年 1 月 1 日：提醒重新导入当年节假日信息。</li>
 *   <li>每年 12 月 31 日：提醒导入来年节假日信息。</li>
 * </ul>
 * 每次提醒后会把年份写入本地，保证同一年只提醒一次。
 */
public class HolidayReminder {

    private final HolidayStore store;
    private final HolidayPanel panel;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "holiday-reminder");
        t.setDaemon(false);
        return t;
    });

    public HolidayReminder(HolidayStore store, HolidayPanel panel) {
        this.store = store;
        this.panel = panel;
    }

    public void start() {
        executor.scheduleAtFixedRate(this::check, 5, 60, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.shutdownNow();
    }

    private void check() {
        LocalDate today = LocalDate.now();
        int year = today.getYear();

        if (today.getMonth() == Month.JANUARY && today.getDayOfMonth() == 1) {
            if (store.getJan1RemindedYear() != year) {
                store.setJan1RemindedYear(year);
                SwingUtilities.invokeLater(() ->
                        prompt(year, "新的一年已开始，请重新导入 " + year + " 年的节假日信息。"));
            }
        } else if (today.getMonth() == Month.DECEMBER && today.getDayOfMonth() == 31) {
            if (store.getDec31RemindedYear() != year) {
                store.setDec31RemindedYear(year);
                int next = year + 1;
                SwingUtilities.invokeLater(() ->
                        prompt(next, "今年即将结束，请提前导入 " + next + " 年的节假日信息。"));
            }
        }
    }

    private void prompt(int targetYear, String message) {
        int choice = JOptionPane.showOptionDialog(panel,
                message,
                "节假日导入提醒",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new Object[]{"立即导入", "稍后"},
                "立即导入");
        if (choice == JOptionPane.YES_OPTION) {
            panel.importFromExcel();
        }
    }
}
