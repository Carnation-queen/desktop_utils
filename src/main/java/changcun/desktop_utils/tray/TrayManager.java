package changcun.desktop_utils.tray;

import changcun.desktop_utils.service.ShutdownScheduler;
import changcun.desktop_utils.ui.AppIcon;
import changcun.desktop_utils.ui.MainFrame;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 系统托盘（Windows 任务栏通知区 / macOS Dock 菜单栏）管理。
 * 提供“显示主界面”和“退出程序”两个右键菜单项。
 */
public class TrayManager {

    private final MainFrame frame;
    private final ShutdownScheduler scheduler;
    private TrayIcon trayIcon;
    private boolean hintShown = false;

    public TrayManager(MainFrame frame, ShutdownScheduler scheduler) {
        this.frame = frame;
        this.scheduler = scheduler;
    }

    public boolean isSupported() {
        return SystemTray.isSupported();
    }

    public void install() {
        if (!SystemTray.isSupported()) {
            return;
        }

        int traySize = SystemTray.getSystemTray().getTrayIconSize().height;
        trayIcon = new TrayIcon(AppIcon.trayIcon(traySize), "桌面工具");
        trayIcon.setImageAutoSize(true);
        // 双击托盘图标打开主界面
        trayIcon.addActionListener(e -> showMainWindow());

        // 右键弹出 Swing 菜单，保证中文正常显示（避免 AWT 原生菜单缺字显示为方块）
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }
        });

        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private void showPopup(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem openItem = new JMenuItem("显示主界面");
        openItem.addActionListener(ev -> showMainWindow());

        JMenuItem exitItem = new JMenuItem("退出程序");
        exitItem.addActionListener(ev -> exitApplication());

        menu.add(openItem);
        menu.addSeparator();
        menu.add(exitItem);

        // 定位到鼠标位置上方，避免菜单超出屏幕底部
        int x = e.getXOnScreen();
        int y = e.getYOnScreen() - menu.getPreferredSize().height;
        if (y < 0) {
            y = e.getYOnScreen();
        }
        menu.setLocation(x, y);
        menu.setInvoker(menu);
        menu.setVisible(true);
    }

    public void showMainWindow() {
        frame.setVisible(true);
        frame.setState(javax.swing.JFrame.NORMAL);
        frame.toFront();
        frame.requestFocus();
    }

    public void showMinimizeHint() {
        if (trayIcon != null && !hintShown) {
            hintShown = true;
            trayIcon.displayMessage("程序已最小化到托盘",
                    "程序仍在后台运行，右键托盘图标可完全退出。",
                    TrayIcon.MessageType.INFO);
        }
    }

    /**
     * 完全退出：停止调度、移除托盘图标、销毁窗口并结束整个 JVM。
     */
    public void exitApplication() {
        scheduler.stop();
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        frame.dispose();
        System.exit(0);
    }

}
