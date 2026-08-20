package changcun.desktop_utils.ui;

import changcun.desktop_utils.model.AppSettings;
import changcun.desktop_utils.service.AppSettingsStore;
import changcun.desktop_utils.service.AutoStartManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * 设置页面：提供“开机自启动”开关，状态自动持久化。
 */
public class SettingsPanel extends JPanel {

    private static final Color SUCCESS_GREEN = new Color(0x16A34A);
    private static final Color ERROR_RED = new Color(0xDC2626);

    private final AutoStartManager autoStartManager;
    private final AppSettingsStore appSettingsStore;
    private final JCheckBox autoStartCheck = new JCheckBox("开机自启动");
    private final JCheckBox autoUpdateCheck = new JCheckBox("自动检查更新");
    private final JTextField updateUrlField = new JTextField();
    private final JButton saveUrlButton = UiTheme.secondaryButton("保存更新源");
    private final JLabel statusLabel = new JLabel();
    private final JLabel updateStatusLabel = new JLabel();
    private boolean applying = false;

    public SettingsPanel(AutoStartManager autoStartManager, AppSettingsStore appSettingsStore) {
        this.autoStartManager = autoStartManager;
        this.appSettingsStore = appSettingsStore;

        setLayout(new BorderLayout(0, 16));
        setBackground(UiTheme.WINDOW_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(buildGeneralCard());
        content.add(Box.createVerticalStrut(16));
        content.add(buildUpdateCard());
        add(content, BorderLayout.CENTER);

        autoStartCheck.setFont(autoStartCheck.getFont().deriveFont(Font.BOLD, 15f));
        autoStartCheck.setForeground(UiTheme.TEXT_PRIMARY);
        autoStartCheck.setSelected(autoStartManager.isEnabled());
        autoStartCheck.setEnabled(autoStartManager.isSupported());
        // 在初始化选中状态之后再绑定监听，避免启动时触发一次写入。
        autoStartCheck.addActionListener(e -> applyAutoStart(autoStartCheck.isSelected()));

        AppSettings settings = appSettingsStore.load();
        autoUpdateCheck.setFont(autoUpdateCheck.getFont().deriveFont(Font.BOLD, 15f));
        autoUpdateCheck.setForeground(UiTheme.TEXT_PRIMARY);
        autoUpdateCheck.setSelected(settings.isAutoUpdate());
        autoUpdateCheck.addActionListener(e -> applyAutoUpdate(autoUpdateCheck.isSelected()));

        updateUrlField.setText(settings.getUpdateUrl());
        saveUrlButton.addActionListener(e -> saveUpdateUrl());

        refreshStatus();
        refreshUpdateStatus();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(UiTheme.title("设置"));
        JLabel subtitle = UiTheme.subtitle("程序的常规偏好设置，修改后自动保存");
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        header.add(subtitle);
        return header;
    }

    private JPanel buildGeneralCard() {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 14));

        card.add(UiTheme.sectionTitle("常规设置"), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(10, 0, 4, 0));

        body.add(autoStartCheck);

        JLabel desc = UiTheme.subtitle("程序随系统启动后自动运行并常驻后台（默认关闭）");
        desc.setBorder(new EmptyBorder(4, 0, 0, 0));
        body.add(desc);

        statusLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 13f));
        body.add(statusLabel);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildUpdateCard() {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 14));

        card.add(UiTheme.sectionTitle("更新设置"), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(10, 0, 4, 0));

        body.add(autoUpdateCheck);

        JLabel autoDesc = UiTheme.subtitle("程序启动时自动在后台检查是否有新版本（默认开启）");
        autoDesc.setBorder(new EmptyBorder(4, 0, 8, 0));
        body.add(autoDesc);

        JLabel urlLabel = UiTheme.subtitle("更新源地址（GitHub Releases 接口）");
        body.add(urlLabel);

        JPanel urlRow = new JPanel(new BorderLayout(8, 0));
        urlRow.setOpaque(false);
        urlRow.setBorder(new EmptyBorder(4, 0, 0, 0));
        updateUrlField.setFont(updateUrlField.getFont().deriveFont(Font.PLAIN, 13f));
        urlRow.add(updateUrlField, BorderLayout.CENTER);
        urlRow.add(saveUrlButton, BorderLayout.EAST);
        body.add(urlRow);

        updateStatusLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        updateStatusLabel.setFont(updateStatusLabel.getFont().deriveFont(Font.PLAIN, 13f));
        body.add(updateStatusLabel);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void applyAutoStart(boolean enabled) {
        if (applying) {
            return;
        }
        applying = true;
        try {
            boolean ok = autoStartManager.setEnabled(enabled);
            if (!ok) {
                JOptionPane.showMessageDialog(this,
                        enabled
                                ? "开机自启动设置失败。\n请确认系统支持该功能，或尝试以管理员身份运行后重试。"
                                : "取消开机自启动失败，请稍后重试。",
                        "提示",
                        JOptionPane.WARNING_MESSAGE);
                // 回滚开关到持久化的实际状态
                autoStartCheck.setSelected(!enabled);
            }
        } finally {
            applying = false;
        }
        refreshStatus();
    }

    private void refreshStatus() {
        if (!autoStartManager.isSupported()) {
            statusLabel.setText("当前系统暂不支持开机自启动");
            statusLabel.setForeground(UiTheme.TEXT_SECONDARY);
        } else if (autoStartManager.isEnabled()) {
            statusLabel.setText("已开启：程序将随系统启动自动运行");
            statusLabel.setForeground(SUCCESS_GREEN);
        } else {
            statusLabel.setText("已关闭：程序不会随系统自动启动");
            statusLabel.setForeground(UiTheme.TEXT_SECONDARY);
        }
    }

    private void applyAutoUpdate(boolean enabled) {
        AppSettings settings = appSettingsStore.load();
        settings.setAutoUpdate(enabled);
        appSettingsStore.save(settings);
        refreshUpdateStatus();
    }

    private void saveUpdateUrl() {
        String url = updateUrlField.getText() == null ? "" : updateUrlField.getText().trim();
        if (url.isEmpty()) {
            updateStatusLabel.setForeground(ERROR_RED);
            updateStatusLabel.setText("更新源地址不能为空");
            updateUrlField.setText(AppSettings.DEFAULT_UPDATE_URL);
            return;
        }
        AppSettings settings = appSettingsStore.load();
        settings.setUpdateUrl(url);
        appSettingsStore.save(settings);
        updateUrlField.setText(settings.getUpdateUrl());
        updateStatusLabel.setForeground(SUCCESS_GREEN);
        updateStatusLabel.setText("更新源已保存");
    }

    private void refreshUpdateStatus() {
        AppSettings settings = appSettingsStore.load();
        if (settings.isAutoUpdate()) {
            updateStatusLabel.setText("已开启：启动时自动检查更新");
            updateStatusLabel.setForeground(SUCCESS_GREEN);
        } else {
            updateStatusLabel.setText("已关闭：需手动点击“检查更新”");
            updateStatusLabel.setForeground(UiTheme.TEXT_SECONDARY);
        }
    }
}
