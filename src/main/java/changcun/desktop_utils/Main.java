package changcun.desktop_utils;

import changcun.desktop_utils.model.ShutdownConfig;
import changcun.desktop_utils.service.AppSettingsStore;
import changcun.desktop_utils.service.AutoStartManager;
import changcun.desktop_utils.service.HolidayReminder;
import changcun.desktop_utils.service.HolidayStore;
import changcun.desktop_utils.service.SettingsStore;
import changcun.desktop_utils.service.ShutdownScheduler;
import changcun.desktop_utils.tray.TrayManager;
import changcun.desktop_utils.ui.AppIcon;
import changcun.desktop_utils.ui.MainFrame;
import changcun.desktop_utils.ui.UiTheme;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 程序入口：Swing 桌面工具，包含系统信息展示、定时关机与节假日管理。
 * 运行后常驻后台，托盘图标右键可完全退出。
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UiTheme.applyGlobalFont();
            try {
                FlatLightLaf.setup();
            } catch (Exception ignored) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored2) {
                    // 使用默认外观
                }
            }

            SettingsStore store = new SettingsStore();
            AppSettingsStore appSettingsStore = new AppSettingsStore();
            AutoStartManager autoStartManager = new AutoStartManager(appSettingsStore);
            // 若已开启自启动，则刷新注册表命令，使其指向当前程序位置。
            autoStartManager.syncOnStartup();

            HolidayStore holidayStore = new HolidayStore();
            ShutdownConfig config = store.load();
            ShutdownScheduler scheduler = new ShutdownScheduler(config, store, holidayStore);

            MainFrame frame = new MainFrame(scheduler, holidayStore, autoStartManager);
            HolidayReminder reminder = new HolidayReminder(holidayStore, frame.getHolidayPanel());

            // Windows 任务栏应用图标（Alt-Tab 与固定到任务栏时使用）
            try {
                java.awt.Taskbar taskbar = java.awt.Taskbar.getTaskbar();
                if (taskbar.isSupported(java.awt.Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(AppIcon.windowIcon());
                }
            } catch (Exception ignored) {
                // 某些环境不支持 Taskbar，忽略即可
            }

            TrayManager tray = new TrayManager(frame, scheduler);
            tray.install();

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // 点击关闭按钮 = 最小化到托盘，保持后台常驻。
                    frame.setVisible(false);
                    tray.showMinimizeHint();
                }
            });

            scheduler.start();
            reminder.start();
            frame.setVisible(true);
        });
    }
}