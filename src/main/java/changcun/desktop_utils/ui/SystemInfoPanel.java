package changcun.desktop_utils.ui;

import changcun.desktop_utils.i18n.Messages;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Font;

/**
 * 第一个页面：展示当前系统名称、版本号等基本信息。
 */
public class SystemInfoPanel extends JPanel {

    public SystemInfoPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UiTheme.WINDOW_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel card = UiTheme.card();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        String[][] rows = {
                {Messages.tr("sysinfo.row.osName"), System.getProperty("os.name", Messages.tr("common.unknown"))},
                {Messages.tr("sysinfo.row.osVersion"), System.getProperty("os.version", Messages.tr("common.unknown"))},
                {Messages.tr("sysinfo.row.osArch"), System.getProperty("os.arch", Messages.tr("common.unknown"))},
                {Messages.tr("sysinfo.row.javaVersion"), System.getProperty("java.version", Messages.tr("common.unknown"))},
                {Messages.tr("sysinfo.row.javaVendor"), System.getProperty("java.vendor", Messages.tr("common.unknown"))},
                {Messages.tr("sysinfo.row.userName"), System.getProperty("user.name", Messages.tr("common.unknown"))},
        };

        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                card.add(UiTheme.separator());
            }
            card.add(buildInfoRow(rows[i][0], rows[i][1]));
        }

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(card, BorderLayout.NORTH);
        add(wrapper, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(UiTheme.title(Messages.tr("sysinfo.title")));
        JLabel subtitle = UiTheme.subtitle(Messages.tr("sysinfo.subtitle"));
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        header.add(subtitle);
        return header;
    }

    private JPanel buildInfoRow(String name, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(10, 4, 10, 4));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(UiTheme.TEXT_SECONDARY);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(UiTheme.TEXT_PRIMARY);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.PLAIN, 13f));

        row.add(nameLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);
        return row;
    }
}
