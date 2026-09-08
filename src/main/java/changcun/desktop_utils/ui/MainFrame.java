package changcun.desktop_utils.ui;

import changcun.desktop_utils.i18n.Messages;
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
import javax.swing.SwingUtilities;
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
    private final NovelStore novelStore;
    private final ShutdownScheduler scheduler;
    private final AutoStartManager autoStartManager;
    private final AppSettingsStore appSettingsStore;
    private final UpdateChecker updateChecker;

    private JTabbedPane tabs;
    private HolidayPanel holidayPanel;
    private SettingsPanel settingsPanel;
    private AboutPanel aboutPanel;
    private ShutdownPanel shutdownPanel;
    private int lastTabIndex = 0;
    private boolean rebuildQueued = false;

    private NovelReaderFrame novelReader;
    private ShutdownLogFrame shutdownLog;

    private Runnable onUiRebuilt = () -> {
    };

    public MainFrame(ShutdownScheduler scheduler, HolidayStore holidayStore,
                     AutoStartManager autoStartManager, AppSettingsStore appSettingsStore,
                     UpdateChecker updateChecker, NovelStore novelStore) {
        super(Messages.tr("app.name"));
        this.scheduler = scheduler;
        this.holidayStore = holidayStore;
        this.autoStartManager = autoStartManager;
        this.appSettingsStore = appSettingsStore;
        this.updateChecker = updateChecker;
        this.novelStore = novelStore;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setIconImage(AppIcon.windowIcon());
        setSize(1000, 750);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);

        setContentPane(buildContent());
        installNovelReaderShortcut();
        installShutdownLogShortcut();
        // 语言切换后重建整个界面，实现即时生效。
        Messages.addListener(this::onLanguageChanged);
    }

    /**
     * 设置界面重建完成后的回调（由 {@code Main} 使用，用于更新对面板的引用，
     * 例如节假日提醒所持有的 {@link HolidayPanel} 实例）。
     */
    public void setOnUiRebuilt(Runnable callback) {
        this.onUiRebuilt = callback != null ? callback : () -> {
        };
    }

    /**
     * 语言切换回调。不在触发切换的 AWT 事件（下拉框选中/弹层关闭）处理过程中同步拆除并
     * 重建整个窗口——那样会让窗口重建与下拉弹层/事件派发竞争，导致主窗口停留在旧界面或
     * 失去响应。改为把重建延后到当前事件处理完成之后，并合并连续多次切换。
     */
    private void onLanguageChanged() {
        if (rebuildQueued) {
            return;
        }
        rebuildQueued = true;
        SwingUtilities.invokeLater(this::rebuildForLanguage);
    }

    /** 延后执行的整体重建：重建所有面板与页签标题，并重建（丢弃）子窗口。 */
    private void rebuildForLanguage() {
        rebuildQueued = false;
        try {
            int index = tabs != null ? tabs.getSelectedIndex() : lastTabIndex;
            if (index < 0) {
                index = 0;
            }
            disposeNovelReader();
            disposeShutdownLog();
            if (shutdownPanel != null) {
                shutdownPanel.dispose();
            }
            lastTabIndex = index;
            setTitle(Messages.tr("app.name"));
            setContentPane(buildContent());
            revalidate();
            repaint();
            onUiRebuilt.run();
        } catch (Throwable t) {
            // 重建失败也打印出来，避免主窗口停留在旧界面且无任何提示。
            t.printStackTrace();
        }
    }

    /** 关闭小说阅读器子窗口（保存进度），下次呼出时以新语言重建。 */
    private void disposeNovelReader() {
        if (novelReader != null) {
            novelReader.saveProgress();
            novelReader.dispose();
            novelReader = null;
        }
    }

    /** 关闭关机日志子窗口，下次呼出时以新语言重建。 */
    private void disposeShutdownLog() {
        if (shutdownLog != null) {
            shutdownLog.dispose();
            shutdownLog = null;
        }
    }

    /** 在主窗口内任意位置按下 Ctrl+Alt+Shift+F12 时唤起独立的小说阅读器窗口。 */
    private void installNovelReaderShortcut() {
        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F12,
                InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
        getRootPane().registerKeyboardAction(e -> openNovelReader(), key,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /**
     * 隐藏入口：在主窗口内任意位置按下单独的 F12，唤起独立的关机审计日志窗口。
     * 该日志不提供任何公开菜单/按钮入口，仅在界面内按 F12 可查看。
     */
    private void installShutdownLogShortcut() {
        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0);
        getRootPane().registerKeyboardAction(e -> openShutdownLog(), key,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** 打开（或唤起）关机审计日志窗口。 */
    public void openShutdownLog() {
        if (shutdownLog == null) {
            shutdownLog = new ShutdownLogFrame();
        }
        shutdownLog.open();
    }

    /** 打开（或唤起）小说阅读器窗口。 */
    public void openNovelReader() {
        if (novelReader == null) {
            novelReader = new NovelReaderFrame(novelStore);
        }
        novelReader.open();
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiTheme.WINDOW_BG);

        root.add(buildHeader(), BorderLayout.NORTH);

        tabs = new JTabbedPane();
        tabs.addTab(Messages.tr("main.tab.system"), new SystemInfoPanel());
        shutdownPanel = new ShutdownPanel(scheduler, holidayStore);
        tabs.addTab(Messages.tr("main.tab.shutdown"), shutdownPanel);
        holidayPanel = new HolidayPanel(holidayStore);
        tabs.addTab(Messages.tr("main.tab.holiday"), holidayPanel);
        settingsPanel = new SettingsPanel(autoStartManager, appSettingsStore);
        tabs.addTab(Messages.tr("main.tab.settings"), settingsPanel);
        aboutPanel = new AboutPanel(updateChecker);
        tabs.addTab(Messages.tr("main.tab.about"), aboutPanel);
        tabs.setSelectedIndex(Math.min(Math.max(lastTabIndex, 0), tabs.getTabCount() - 1));
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

        JLabel title = UiTheme.title(Messages.tr("app.name"));
        JLabel subtitle = UiTheme.subtitle(Messages.tr("main.header.subtitle"));
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
