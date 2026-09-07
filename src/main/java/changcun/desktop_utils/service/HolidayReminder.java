package changcun.desktop_utils.service;

import changcun.desktop_utils.i18n.Messages;
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
    private HolidayPanel panel;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "holiday-reminder");
        t.setDaemon(false);
        return t;
    });

    public HolidayReminder(HolidayStore store, HolidayPanel panel) {
        this.store = store;
        this.panel = panel;
    }

    /** 语言切换导致 HolidayPanel 被重建后，用新实例替换引用。 */
    public void rebind(HolidayPanel newPanel) {
        if (newPanel != null) {
            this.panel = newPanel;
        }
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
                        prompt(year, Messages.tr("reminder.jan1", year)));
            }
        } else if (today.getMonth() == Month.DECEMBER && today.getDayOfMonth() == 31) {
            if (store.getDec31RemindedYear() != year) {
                store.setDec31RemindedYear(year);
                int next = year + 1;
                SwingUtilities.invokeLater(() ->
                        prompt(next, Messages.tr("reminder.dec31", next)));
            }
        }
    }

    private void prompt(int targetYear, String message) {
        String importNow = Messages.tr("reminder.importNow");
        int choice = JOptionPane.showOptionDialog(panel,
                message,
                Messages.tr("reminder.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                new Object[]{importNow, Messages.tr("reminder.later")},
                importNow);
        if (choice == JOptionPane.YES_OPTION) {
            panel.importFromExcel();
        }
    }
}
