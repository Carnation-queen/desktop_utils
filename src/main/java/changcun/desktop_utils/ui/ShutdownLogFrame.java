package changcun.desktop_utils.ui;

import changcun.desktop_utils.i18n.Messages;
import changcun.desktop_utils.service.ShutdownAudit;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 独立的“关机审计日志”查看窗口。
 * <p>
 * 该窗口没有任何公开入口：仅当用户在主窗口内按下 <b>F12</b> 时由 {@link MainFrame} 唤起，
 * 用于查看 {@code ~/.desktop_utils/logs/shutdown.log} 中记录的历史关机/配置变更审计信息。
 * 窗口关闭等于隐藏（不退出程序）；日志窗口处于焦点时再次按 F12 或按 ESC 即隐藏。
 * </p>
 */
public class ShutdownLogFrame extends JFrame {

    private static final KeyStroke TOGGLE_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0);

    private static final Path LOG_FILE = Paths.get(
            System.getProperty("user.home"), ".desktop_utils", "logs", "shutdown.log");

    private final JLabel clockLabel = new JLabel();
    private final JTextArea logArea = new JTextArea();
    private final Timer clockTimer;
    private JLabel countLabel;

    public ShutdownLogFrame() {
        super(Messages.tr("log.title"));
        setIconImage(AppIcon.windowIcon());
        setSize(920, 620);
        setMinimumSize(new Dimension(640, 420));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        buildUi();
        clockTimer = new Timer(100, e -> refreshClock());
        clockTimer.start();

        // 日志窗口自身获得焦点时，再次按 F12 或 ESC 隐藏（形成与主窗口的“开/关”切换）。
        getRootPane().registerKeyboardAction(e -> hideWindow(), TOGGLE_KEY,
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> hideWindow(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** 呼出日志窗口：置顶显示并重新读取日志文件。 */
    public void open() {
        refresh();
        setVisible(true);
        toFront();
        requestFocus();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(UiTheme.WINDOW_BG);
        root.setBorder(new EmptyBorder(16, 16, 16, 16));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiTheme.CARD_BG);
        header.setBorder(new EmptyBorder(14, 18, 14, 18));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new javax.swing.BoxLayout(text, javax.swing.BoxLayout.Y_AXIS));
        text.add(UiTheme.title(Messages.tr("log.title")));
        JLabel sub = UiTheme.subtitle(Messages.tr("log.subtitle"));
        sub.setBorder(new EmptyBorder(4, 0, 0, 0));
        text.add(sub);
        header.add(text, BorderLayout.WEST);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new javax.swing.BoxLayout(right, javax.swing.BoxLayout.Y_AXIS));

        clockLabel.setForeground(UiTheme.TEXT_SECONDARY);
        clockLabel.setFont(clockLabel.getFont().deriveFont(Font.PLAIN, 13f));
        clockLabel.setHorizontalAlignment(JLabel.RIGHT);
        clockLabel.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
        right.add(clockLabel);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        buttons.setOpaque(false);
        JButton refresh = UiTheme.secondaryButton(Messages.tr("common.refresh"));
        refresh.addActionListener(e -> refresh());
        JButton clear = UiTheme.secondaryButton(Messages.tr("log.clear"));
        clear.addActionListener(e -> clearLog());
        buttons.add(refresh);
        buttons.add(clear);
        buttons.setAlignmentX(JComponent.RIGHT_ALIGNMENT);
        right.add(buttons);

        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel buildBody() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UiTheme.CARD_BG);
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        countLabel = new JLabel(" ");
        countLabel.setForeground(UiTheme.TEXT_SECONDARY);
        countLabel.setFont(countLabel.getFont().deriveFont(Font.PLAIN, 12f));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 2, 6, 2));
        top.add(countLabel, BorderLayout.WEST);
        JLabel path = new JLabel(Messages.tr("log.filePrefix") + LOG_FILE.toAbsolutePath());
        path.setForeground(UiTheme.TEXT_SECONDARY);
        path.setFont(path.getFont().deriveFont(Font.PLAIN, 12f));
        top.add(path, BorderLayout.EAST);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(false);
        logArea.setBackground(UiTheme.CARD_BG);
        logArea.setForeground(UiTheme.TEXT_PRIMARY);

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        card.add(top, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    /** 重新读取日志文件内容（新日志在底部），并刷新条目统计。 */
    private void refresh() {
        try {
            String content;
            if (Files.exists(LOG_FILE)) {
                content = String.join("\n", Files.readAllLines(LOG_FILE, StandardCharsets.UTF_8));
            } else {
                content = "";
            }
            logArea.setText(content);
            logArea.setCaretPosition(Math.max(0, logArea.getDocument().getLength() - 1));

            long size = Files.exists(LOG_FILE) ? Files.size(LOG_FILE) : 0L;
            long lines = content.isEmpty() ? 0 : content.split("\n", -1).length;
            countLabel.setText(Messages.tr("log.count", lines, humanSize(size)));
        } catch (Exception e) {
            logArea.setText(Messages.tr("log.readFail", e));
            countLabel.setText(" ");
        }
    }

    /** 顶部毫秒时钟（用于对照“实际触发时刻”）。 */
    private void refreshClock() {
        clockLabel.setText(Messages.tr("log.clockPrefix")
                + ShutdownAudit.formatMillis(System.currentTimeMillis()));
    }

    /**
     * 清空日志：通过 Logback 停掉文件 appender、删除日志文件后重启 appender，
     * 避免在 appender 仍持有文件句柄时直接截断导致的错位写入。
     */
    private void clearLog() {
        int choice = JOptionPane.showConfirmDialog(this,
                Messages.tr("log.clear.confirm"),
                Messages.tr("log.clear"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            // 先把 appender 收集到列表，避免多次遍历同一迭代器。
            List<Appender<ILoggingEvent>> appenders = new ArrayList<>();
            Iterator<Appender<ILoggingEvent>> it = auditAppenders();
            while (it.hasNext()) {
                appenders.add(it.next());
            }
            for (Appender<ILoggingEvent> a : appenders) {
                if (a instanceof RollingFileAppender) {
                    a.stop();
                }
            }
            Files.deleteIfExists(LOG_FILE);
            for (Appender<ILoggingEvent> a : appenders) {
                if (a instanceof RollingFileAppender) {
                    a.start();
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    Messages.tr("log.clear.fail", e),
                    Messages.tr("log.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        refresh();
        // 以空状态写入一行起始标记，方便确认 appender 已恢复正常。
        ShutdownAudit.cleared();
        refresh();
    }

    private Iterator<Appender<ILoggingEvent>> auditAppenders() {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger audit = ctx.getLogger(ShutdownAudit.LOGGER_NAME);
        return audit.iteratorForAppenders();
    }

    private void hideWindow() {
        setVisible(false);
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        return String.format("%.1f KB", bytes / 1024.0);
    }
}
