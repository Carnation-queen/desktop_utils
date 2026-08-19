package changcun.desktop_utils.ui;

import changcun.desktop_utils.service.AutoStartManager;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * 设置页面：提供“开机自启动”开关，状态自动持久化。
 */
public class SettingsPanel extends JPanel {

    private static final Color SUCCESS_GREEN = new Color(0x16A34A);

    private final AutoStartManager autoStartManager;
    private final JCheckBox autoStartCheck = new JCheckBox("开机自启动");
    private final JLabel statusLabel = new JLabel();
    private boolean applying = false;

    public SettingsPanel(AutoStartManager autoStartManager) {
        this.autoStartManager = autoStartManager;

        setLayout(new BorderLayout(0, 16));
        setBackground(UiTheme.WINDOW_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(buildGeneralCard());
        add(content, BorderLayout.CENTER);

        autoStartCheck.setFont(autoStartCheck.getFont().deriveFont(Font.BOLD, 15f));
        autoStartCheck.setForeground(UiTheme.TEXT_PRIMARY);
        autoStartCheck.setSelected(autoStartManager.isEnabled());
        autoStartCheck.setEnabled(autoStartManager.isSupported());
        // 在初始化选中状态之后再绑定监听，避免启动时触发一次写入。
        autoStartCheck.addActionListener(e -> applyAutoStart(autoStartCheck.isSelected()));

        refreshStatus();
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
}
