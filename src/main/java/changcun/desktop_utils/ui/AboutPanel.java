package changcun.desktop_utils.ui;

import changcun.desktop_utils.model.UpdateCheckResult;
import changcun.desktop_utils.model.UpdateInfo;
import changcun.desktop_utils.service.AppVersion;
import changcun.desktop_utils.service.UpdateChecker;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * “关于 / 版本”页面：展示当前版本，提供“检查更新”与“下载更新”功能。
 */
public class AboutPanel extends JPanel {

    private static final Color SUCCESS_GREEN = new Color(0x16A34A);
    private static final Color ERROR_RED = new Color(0xDC2626);

    private final UpdateChecker checker;

    private final JLabel versionLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JTextArea notesArea = new JTextArea();
    private final JScrollPane notesScroll = new JScrollPane(notesArea);
    private final JButton checkButton = UiTheme.secondaryButton("检查更新");
    private final JButton downloadButton = UiTheme.primaryButton("下载更新");
    private final JProgressBar progressBar = new JProgressBar(0, 100);

    private UpdateInfo latestUpdate;
    private boolean busy = false;

    public AboutPanel(UpdateChecker checker) {
        this.checker = checker;

        setLayout(new BorderLayout(0, 16));
        setBackground(UiTheme.WINDOW_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(buildInfoCard());
        content.add(Box.createVerticalStrut(16));
        content.add(buildUpdateCard());
        add(content, BorderLayout.CENTER);

        checkButton.addActionListener(e -> checkForUpdates(false));
        downloadButton.addActionListener(e -> startDownload());
        downloadButton.setEnabled(false);
        progressBar.setVisible(false);
        progressBar.setStringPainted(true);

        versionLabel.setText("当前版本 v" + AppVersion.current());
        notesArea.setText("暂无更新信息");
        statusLabel.setText("点击“检查更新”查看是否有新版本");
    }

    /** 启动时在后台静默检查一次；发现新版本时弹窗询问是否下载。 */
    public void autoCheck() {
        checkForUpdates(true);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(UiTheme.title("关于"));
        JLabel subtitle = UiTheme.subtitle("版本信息与软件更新");
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        header.add(subtitle);
        return header;
    }

    private JPanel buildInfoCard() {
        JPanel card = UiTheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel appName = UiTheme.sectionTitle("桌面工具 Desktop Utils");
        card.add(appName);

        versionLabel.setForeground(UiTheme.ACCENT);
        versionLabel.setFont(versionLabel.getFont().deriveFont(Font.BOLD, 18f));
        versionLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        card.add(versionLabel);

        JLabel desc = UiTheme.subtitle("系统信息 · 定时关机 · 节假日管理 · 设置");
        desc.setBorder(new EmptyBorder(8, 0, 0, 0));
        card.add(desc);
        return card;
    }

    private JPanel buildUpdateCard() {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(UiTheme.sectionTitle("检查更新"), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(10, 0, 0, 0));

        statusLabel.setForeground(UiTheme.TEXT_SECONDARY);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 13f));
        body.add(statusLabel);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttons.setOpaque(false);
        buttons.add(checkButton);
        buttons.add(downloadButton);
        buttons.setBorder(new EmptyBorder(8, 0, 0, 0));
        body.add(buttons);

        notesArea.setEditable(false);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBackground(UiTheme.WINDOW_BG);
        notesArea.setForeground(UiTheme.TEXT_PRIMARY);
        notesArea.setFont(notesArea.getFont().deriveFont(Font.PLAIN, 13f));
        notesScroll.setPreferredSize(new Dimension(0, 110));
        notesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        notesScroll.setBorder(javax.swing.BorderFactory.createLineBorder(UiTheme.DIVIDER));
        body.add(Box.createVerticalStrut(10));
        body.add(notesScroll);

        progressBar.setBorder(new EmptyBorder(10, 0, 0, 0));
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        body.add(progressBar);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void checkForUpdates(boolean silent) {
        if (busy) {
            return;
        }
        busy = true;
        checkButton.setEnabled(false);
        downloadButton.setEnabled(false);
        statusLabel.setForeground(UiTheme.TEXT_SECONDARY);
        statusLabel.setText("正在检查更新…");

        SwingWorker<UpdateCheckResult, Void> worker = new SwingWorker<>() {
            @Override
            protected UpdateCheckResult doInBackground() {
                return checker.checkForUpdate();
            }

            @Override
            protected void done() {
                busy = false;
                checkButton.setEnabled(true);
                try {
                    handleResult(get(), silent);
                } catch (Exception e) {
                    showError("检查更新失败：" + e.getMessage(), silent);
                }
            }
        };
        worker.execute();
    }

    private void handleResult(UpdateCheckResult result, boolean silent) {
        switch (result.getStatus()) {
            case UP_TO_DATE:
                statusLabel.setForeground(SUCCESS_GREEN);
                statusLabel.setText("已是最新版本（v" + result.getCurrentVersion() + "）");
                latestUpdate = null;
                downloadButton.setEnabled(false);
                notesArea.setText("暂无更新信息");
                if (!silent) {
                    JOptionPane.showMessageDialog(this,
                            "当前已是最新版本（v" + result.getCurrentVersion() + "）。",
                            "检查更新", JOptionPane.INFORMATION_MESSAGE);
                }
                break;
            case UPDATE_AVAILABLE:
                latestUpdate = result.getLatest();
                statusLabel.setForeground(UiTheme.ACCENT);
                statusLabel.setText("发现新版本 v" + latestUpdate.getVersion()
                        + "（当前 v" + result.getCurrentVersion() + "）");
                notesArea.setText(describeNotes(latestUpdate.getNotes()));
                downloadButton.setEnabled(true);
                if (silent) {
                    int choice = JOptionPane.showConfirmDialog(this,
                            "发现新版本 v" + latestUpdate.getVersion()
                                    + "，当前为 v" + result.getCurrentVersion() + "。\n是否立即下载更新？",
                            "发现新版本", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (choice == JOptionPane.YES_OPTION) {
                        startDownload();
                    }
                }
                break;
            case ERROR:
            default:
                showError("检查失败：" + result.getErrorMessage(), silent);
                break;
        }
    }

    private void showError(String message, boolean silent) {
        statusLabel.setForeground(ERROR_RED);
        statusLabel.setText(message);
        latestUpdate = null;
        downloadButton.setEnabled(false);
        if (!silent) {
            JOptionPane.showMessageDialog(this, message, "检查更新", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void startDownload() {
        if (latestUpdate == null || busy) {
            return;
        }
        if (latestUpdate.getDownloadUrl() == null || latestUpdate.getDownloadUrl().isBlank()) {
            openInBrowser(latestUpdate.getPageUrl());
            return;
        }

        busy = true;
        downloadButton.setEnabled(false);
        checkButton.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setVisible(true);
        statusLabel.setForeground(UiTheme.TEXT_SECONDARY);
        statusLabel.setText("正在下载 v" + latestUpdate.getVersion() + "…");

        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws IOException {
                return checker.download(latestUpdate, downloadDir(), p ->
                        setProgress((int) Math.round(p)));
            }

            @Override
            protected void done() {
                busy = false;
                progressBar.setVisible(false);
                checkButton.setEnabled(true);
                downloadButton.setEnabled(true);
                try {
                    Path path = get();
                    progressBar.setValue(100);
                    statusLabel.setForeground(SUCCESS_GREEN);
                    statusLabel.setText("下载完成：v" + latestUpdate.getVersion());
                    promptOpen(path);
                } catch (Exception e) {
                    statusLabel.setForeground(ERROR_RED);
                    statusLabel.setText("下载失败：" + messageOf(e));
                    JOptionPane.showMessageDialog(AboutPanel.this,
                            "下载更新失败：\n" + messageOf(e), "下载失败", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                Object value = evt.getNewValue();
                if (value instanceof Number) {
                    progressBar.setValue(((Number) value).intValue());
                }
            }
        });
        worker.execute();
    }

    private void promptOpen(Path path) {
        int choice = JOptionPane.showConfirmDialog(this,
                "更新已下载到：\n" + path + "\n\n是否立即打开（安装）？",
                "下载完成", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            open(path);
        }
    }

    private void open(Path path) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(path.toFile());
                return;
            }
        } catch (IOException ignored) {
            // 打开失败时尝试打开所在目录
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(path.getParent().toFile());
            }
        } catch (IOException ignored) {
            // 忽略
        }
    }

    private void openInBrowser(String url) {
        if (url == null || url.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "该版本未提供下载文件，请到发布页面手动下载。",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
        } catch (IOException ignored) {
            // 回退到弹窗提示地址
        }
        JOptionPane.showMessageDialog(this,
                "请访问发布页面下载更新：\n" + url,
                "打开下载页面", JOptionPane.INFORMATION_MESSAGE);
    }

    private static Path downloadDir() {
        Path downloads = Paths.get(System.getProperty("user.home"), "Downloads");
        if (Files.isDirectory(downloads)) {
            return downloads;
        }
        return Paths.get(System.getProperty("java.io.tmpdir"));
    }

    private static String describeNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return "暂无更新说明";
        }
        return notes.trim();
    }

    private static String messageOf(Exception e) {
        String message = e.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return e.getClass().getSimpleName();
    }
}
