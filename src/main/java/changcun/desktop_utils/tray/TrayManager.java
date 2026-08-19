package changcun.desktop_utils.tray;

import changcun.desktop_utils.service.ShutdownScheduler;
import changcun.desktop_utils.ui.AppIcon;
import changcun.desktop_utils.ui.MainFrame;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
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
    private JPopupMenu activePopup;

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
        // 收起上一次弹出的菜单，避免重复弹窗
        if (activePopup != null && activePopup.isVisible()) {
            activePopup.setVisible(false);
        }

        JPopupMenu menu = new JPopupMenu();

        JMenuItem openItem = new JMenuItem("显示主界面");
        openItem.addActionListener(ev -> showMainWindow());

        JMenuItem exitItem = new JMenuItem("退出程序");
        exitItem.addActionListener(ev -> exitApplication());

        menu.add(openItem);
        menu.addSeparator();
        menu.add(exitItem);

        activePopup = menu;

        // 先计算菜单实际尺寸（必要时使用兜底尺寸，避免未布局时高度为 0）
        Dimension size = menu.getPreferredSize();
        if (size == null || size.width <= 0 || size.height <= 0) {
            size = new Dimension(160, 80);
        }

        Rectangle screen = screenBounds();
        int mouseX = e.getXOnScreen();
        int mouseY = e.getYOnScreen();

        // 托盘图标通常位于屏幕右下角：默认将菜单放到图标左上方，
        // 让菜单右下角贴近鼠标，避免整体偏向图标右侧。
        int x = mouseX - size.width;
        int y = mouseY - size.height;

        // 左侧或上方空间不足时，改放到鼠标的右/下方。
        if (x < screen.x) {
            x = mouseX;
        }
        if (y < screen.y) {
            y = mouseY;
        }
        // 保证菜单完全落在屏幕内。
        if (x + size.width > screen.x + screen.width) {
            x = screen.x + screen.width - size.width;
        }
        if (y + size.height > screen.y + screen.height) {
            y = screen.y + screen.height - size.height;
        }

        menu.setLocation(x, y);
        menu.setVisible(true);
    }

    private static Rectangle screenBounds() {
        try {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice().getDefaultConfiguration();
            return gc.getBounds();
        } catch (Exception e) {
            return new Rectangle(0, 0, 1920, 1080);
        }
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
