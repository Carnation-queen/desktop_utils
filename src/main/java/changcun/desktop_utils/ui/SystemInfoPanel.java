package changcun.desktop_utils.ui;

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
                {"系统名称", System.getProperty("os.name", "未知")},
                {"系统版本", System.getProperty("os.version", "未知")},
                {"系统架构", System.getProperty("os.arch", "未知")},
                {"Java 版本", System.getProperty("java.version", "未知")},
                {"Java 厂商", System.getProperty("java.vendor", "未知")},
                {"当前用户", System.getProperty("user.name", "未知")},
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
        header.add(UiTheme.title("系统信息"));
        JLabel subtitle = UiTheme.subtitle("当前运行环境的基本信息");
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
