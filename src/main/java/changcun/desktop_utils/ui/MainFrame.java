package changcun.desktop_utils.ui;

import changcun.desktop_utils.service.AutoStartManager;
import changcun.desktop_utils.service.HolidayStore;
import changcun.desktop_utils.service.ShutdownScheduler;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * 主窗口：包含“系统信息”、“定时关机”、“节假日”和“设置”四个页面。
 */
public class MainFrame extends JFrame {

    private final HolidayStore holidayStore;
    private final HolidayPanel holidayPanel;
    private final SettingsPanel settingsPanel;

    public MainFrame(ShutdownScheduler scheduler, HolidayStore holidayStore,
                     AutoStartManager autoStartManager) {
        super("桌面工具");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setIconImage(AppIcon.windowIcon());
        setSize(1000, 750);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);

        this.holidayStore = holidayStore;
        this.holidayPanel = new HolidayPanel(holidayStore);
        this.settingsPanel = new SettingsPanel(autoStartManager);

        setContentPane(buildContent(scheduler));
    }

    private JPanel buildContent(ShutdownScheduler scheduler) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiTheme.WINDOW_BG);

        root.add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("系统信息", new SystemInfoPanel());
        tabs.addTab("定时关机", new ShutdownPanel(scheduler, holidayStore));
        tabs.addTab("节假日", holidayPanel);
        tabs.addTab("设置", settingsPanel);
        root.add(tabs, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiTheme.CARD_BG);
        header.setBorder(new EmptyBorder(16, 22, 14, 22));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));

        JLabel title = UiTheme.title("桌面工具");
        JLabel subtitle = UiTheme.subtitle("系统信息 · 定时关机 · 节假日 · 设置");
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));

        text.add(title);
        text.add(subtitle);
        header.add(text, BorderLayout.WEST);
        return header;
    }

    public HolidayPanel getHolidayPanel() {
        return holidayPanel;
    }
}
