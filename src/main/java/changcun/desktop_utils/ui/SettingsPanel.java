package changcun.desktop_utils.ui;

import changcun.desktop_utils.i18n.Messages;
import changcun.desktop_utils.model.AppSettings;
import changcun.desktop_utils.service.AppSettingsStore;
import changcun.desktop_utils.service.AutoStartManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

/**
 * 设置页面：提供“开机自启动”开关，状态自动持久化。
 */
public class SettingsPanel extends JPanel {

    private static final Color SUCCESS_GREEN = new Color(0x16A34A);
    private static final Color ERROR_RED = new Color(0xDC2626);

    /** 可选语言：跟随系统 / 简体中文 / English。 */
    private static final String[] LANGUAGE_CODES = {
            Messages.LANG_SYSTEM, Messages.LANG_ZH, Messages.LANG_EN
    };

    private final AutoStartManager autoStartManager;
    private final AppSettingsStore appSettingsStore;
    private final JCheckBox autoStartCheck = new JCheckBox(Messages.tr("settings.autoStart"));
    private final JCheckBox autoUpdateCheck = new JCheckBox(Messages.tr("settings.autoUpdate"));
    private final JTextField updateUrlField = new JTextField();
    private final JButton saveUrlButton = UiTheme.secondaryButton(Messages.tr("settings.updateUrl.save"));
    private final JLabel statusLabel = new JLabel();
    private final JLabel updateStatusLabel = new JLabel();
    private JComboBox<String> languageCombo;
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
        content.add(buildLanguageCard());
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
        header.add(UiTheme.title(Messages.tr("settings.title")));
        JLabel subtitle = UiTheme.subtitle(Messages.tr("settings.subtitle"));
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        header.add(subtitle);
        return header;
    }

    private JPanel buildGeneralCard() {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 14));

        card.add(UiTheme.sectionTitle(Messages.tr("settings.general.section")), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(10, 0, 4, 0));

        body.add(autoStartCheck);

        JLabel desc = UiTheme.subtitle(Messages.tr("settings.autoStart.desc"));
        desc.setBorder(new EmptyBorder(4, 0, 0, 0));
        body.add(desc);

        statusLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 13f));
        body.add(statusLabel);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /** 语言卡片：下拉选择界面语言，切换后立即生效。 */
    private JPanel buildLanguageCard() {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 14));

        card.add(UiTheme.sectionTitle(Messages.tr("settings.language.section")), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(10, 0, 4, 0));

        JLabel label = UiTheme.subtitle(Messages.tr("settings.language.label"));
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        label.setForeground(UiTheme.TEXT_PRIMARY);
        body.add(label);

        JLabel desc = UiTheme.subtitle(Messages.tr("settings.language.desc"));
        desc.setBorder(new EmptyBorder(4, 0, 10, 0));
        body.add(desc);

        languageCombo = new JComboBox<>();
        languageCombo.setFont(languageCombo.getFont().deriveFont(Font.PLAIN, 13f));
        for (String code : LANGUAGE_CODES) {
            languageCombo.addItem(labelFor(code));
        }
        languageCombo.setSelectedIndex(indexOfCode(appSettingsStore.load().getLanguage()));
        languageCombo.setAlignmentX(0.0f);
        languageCombo.setMaximumSize(new Dimension(360, 30));
        // 先完成默认选中再绑定监听，避免重建时触发语言切换。
        languageCombo.addActionListener(e -> applyLanguageSelection());
        body.add(languageCombo);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    /** 语言下拉选择：保存设置并触发全局界面重建。 */
    private void applyLanguageSelection() {
        if (languageCombo == null) {
            return;
        }
        int index = languageCombo.getSelectedIndex();
        String code = index >= 0 && index < LANGUAGE_CODES.length
                ? LANGUAGE_CODES[index]
                : Messages.LANG_SYSTEM;
        AppSettings settings = appSettingsStore.load();
        if (code.equals(settings.getLanguage())) {
            return;
        }
        settings.setLanguage(code);
        appSettingsStore.save(settings);
        // 触发 Messages 监听器：重建主窗口及各子窗口，实现即时生效。
        Messages.setLanguage(code);
    }

    private static String labelFor(String code) {
        switch (code) {
            case Messages.LANG_ZH:
                return Messages.tr("lang.zh");
            case Messages.LANG_EN:
                return Messages.tr("lang.en");
            default:
                return Messages.tr("lang.system");
        }
    }

    private static int indexOfCode(String code) {
        for (int i = 0; i < LANGUAGE_CODES.length; i++) {
            if (LANGUAGE_CODES[i].equals(code)) {
                return i;
            }
        }
        return 0;
    }

    private JPanel buildUpdateCard() {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 14));

        card.add(UiTheme.sectionTitle(Messages.tr("settings.update.section")), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(10, 0, 4, 0));

        body.add(autoUpdateCheck);

        JLabel autoDesc = UiTheme.subtitle(Messages.tr("settings.autoUpdate.desc"));
        autoDesc.setBorder(new EmptyBorder(4, 0, 8, 0));
        body.add(autoDesc);

        JLabel urlLabel = UiTheme.subtitle(Messages.tr("settings.updateUrl.label"));
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
                        Messages.tr(enabled
                                ? "settings.autoStart.failEnable"
                                : "settings.autoStart.failDisable"),
                        Messages.tr("common.hint"),
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
            statusLabel.setText(Messages.tr("settings.autoStart.unsupported"));
            statusLabel.setForeground(UiTheme.TEXT_SECONDARY);
        } else if (autoStartManager.isEnabled()) {
            statusLabel.setText(Messages.tr("settings.autoStart.enabled"));
            statusLabel.setForeground(SUCCESS_GREEN);
        } else {
            statusLabel.setText(Messages.tr("settings.autoStart.disabled"));
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
            updateStatusLabel.setText(Messages.tr("settings.updateUrl.errEmpty"));
            updateUrlField.setText(AppSettings.DEFAULT_UPDATE_URL);
            return;
        }
        AppSettings settings = appSettingsStore.load();
        settings.setUpdateUrl(url);
        appSettingsStore.save(settings);
        updateUrlField.setText(settings.getUpdateUrl());
        updateStatusLabel.setForeground(SUCCESS_GREEN);
        updateStatusLabel.setText(Messages.tr("settings.updateUrl.saved"));
    }

    private void refreshUpdateStatus() {
        AppSettings settings = appSettingsStore.load();
        if (settings.isAutoUpdate()) {
            updateStatusLabel.setText(Messages.tr("settings.update.status.on"));
            updateStatusLabel.setForeground(SUCCESS_GREEN);
        } else {
            updateStatusLabel.setText(Messages.tr("settings.update.status.off"));
            updateStatusLabel.setForeground(UiTheme.TEXT_SECONDARY);
        }
    }
}
