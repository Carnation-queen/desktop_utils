package changcun.desktop_utils.ui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 统一的浅色主题：背景、卡片、按钮与字体，保证各页面风格一致、现代简洁。
 */
public final class UiTheme {

    public static final Color WINDOW_BG = new Color(0xF3F4F6);
    public static final Color CARD_BG = new Color(0xFFFFFF);
    public static final Color ACCENT = new Color(0x2F6BFF);
    public static final Color ACCENT_SOFT = new Color(0xEAF1FF);
    public static final Color TEXT_PRIMARY = new Color(0x1F2329);
    public static final Color TEXT_SECONDARY = new Color(0x646A73);
    public static final Color DIVIDER = new Color(0xE5E6EB);

    private UiTheme() {
    }

    /** 设置全局默认字体，优先使用系统自带的中文字体，保证中文渲染正常。 */
    public static void applyGlobalFont() {
        UIManager.put("defaultFont", pickFont());
    }

    /** 白色卡片容器。 */
    public static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_BG);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        return panel;
    }

    public static JLabel title(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_PRIMARY);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 20f));
        return label;
    }

    public static JLabel subtitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_SECONDARY);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 13f));
        return label;
    }

    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_PRIMARY);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 15f));
        return label;
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.putClientProperty("JButton.buttonType", "roundRect");
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.putClientProperty("JButton.buttonType", "roundRect");
        return button;
    }

    public static JSeparator separator() {
        JSeparator separator = new JSeparator();
        separator.setForeground(DIVIDER);
        return separator;
    }

    private static Font pickFont() {
        String[] preferred = {
                "Microsoft YaHei UI", "Microsoft YaHei", "PingFang SC",
                "Noto Sans CJK SC", "Source Han Sans SC", "SimHei"
        };
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String family : preferred) {
            if (available.contains(family)) {
                return new Font(family, Font.PLAIN, 13);
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    }
}
