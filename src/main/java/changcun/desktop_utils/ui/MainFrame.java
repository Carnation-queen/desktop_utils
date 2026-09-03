package changcun.desktop_utils.ui;

import changcun.desktop_utils.service.AppSettingsStore;
import changcun.desktop_utils.service.AutoStartManager;
import changcun.desktop_utils.service.HolidayStore;
import changcun.desktop_utils.service.NovelStore;
import changcun.desktop_utils.service.ShutdownScheduler;
import changcun.desktop_utils.service.UpdateChecker;
import changcun.desktop_utils.ui.novel.NovelReaderFrame;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * 主窗口：包含“系统信息”、“定时关机”、“节假日”、“设置”和“关于”五个页面。
 */
public class MainFrame extends JFrame {

    private final HolidayStore holidayStore;
    private final HolidayPanel holidayPanel;
    private final SettingsPanel settingsPanel;
    private final AboutPanel aboutPanel;
    private final NovelStore novelStore;
    private NovelReaderFrame novelReader;

    public MainFrame(ShutdownScheduler scheduler, HolidayStore holidayStore,
                     AutoStartManager autoStartManager, AppSettingsStore appSettingsStore,
                     UpdateChecker updateChecker, NovelStore novelStore) {
        super("桌面工具");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setIconImage(AppIcon.windowIcon());
        setSize(1000, 750);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);

        this.holidayStore = holidayStore;
        this.holidayPanel = new HolidayPanel(holidayStore);
        this.settingsPanel = new SettingsPanel(autoStartManager, appSettingsStore);
        this.aboutPanel = new AboutPanel(updateChecker);
        this.novelStore = novelStore;

        setContentPane(buildContent(scheduler));
        installNovelReaderShortcut();
    }

    /** 在主窗口内任意位置按下 Ctrl+Alt+Shift+F12 时唤起独立的小说阅读器窗口。 */
    private void installNovelReaderShortcut() {
        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F12,
                InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
        getRootPane().registerKeyboardAction(e -> openNovelReader(), key,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** 打开（或唤起）小说阅读器窗口。 */
    public void openNovelReader() {
        if (novelReader == null) {
            novelReader = new NovelReaderFrame(novelStore);
        }
        novelReader.open();
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
        tabs.addTab("关于", aboutPanel);
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
        JLabel subtitle = UiTheme.subtitle("系统信息 · 定时关机 · 节假日 · 设置 · 关于 · 小说阅读器(Ctrl+Alt+Shift+F12)");
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));

        text.add(title);
        text.add(subtitle);
        header.add(text, BorderLayout.WEST);
        return header;
    }

    public HolidayPanel getHolidayPanel() {
        return holidayPanel;
    }

    public AboutPanel getAboutPanel() {
        return aboutPanel;
    }
}
