package changcun.desktop_utils.ui;

import changcun.desktop_utils.model.HolidayData;
import changcun.desktop_utils.service.HolidayStore;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * 第三个页面：展示已导入的节假日信息，并提供 Excel 导入入口。
 */
public class HolidayPanel extends JPanel {

    private final HolidayStore store;
    private final DefaultTableModel tableModel;
    private final JLabel infoLabel = new JLabel();

    public HolidayPanel(HolidayStore store) {
        this.store = store;

        setLayout(new BorderLayout(0, 16));
        setBackground(UiTheme.WINDOW_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);

        // 中部：表格展示
        tableModel = new DefaultTableModel(new Object[]{"序号", "日期", "星期"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(30);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD, 13f));

        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(UiTheme.CARD_BG);
        card.add(scroll, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        // 底部：状态信息
        infoLabel.setForeground(UiTheme.TEXT_SECONDARY);
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.PLAIN, 13f));
        infoLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
        add(infoLabel, BorderLayout.SOUTH);

        refresh();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(UiTheme.title("节假日管理"));
        JLabel subtitle = UiTheme.subtitle("导入年度节假日 Excel 表格，用于非节假日关机判断");
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        text.add(subtitle);
        header.add(text, BorderLayout.WEST);

        JButton importButton = UiTheme.primaryButton("导入 Excel");
        importButton.addActionListener(e -> importFromExcel());
        JButton refreshButton = UiTheme.secondaryButton("刷新");
        refreshButton.addActionListener(e -> refresh());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttons.setOpaque(false);
        buttons.add(refreshButton);
        buttons.add(importButton);
        header.add(buttons, BorderLayout.EAST);

        return header;
    }

    /** 重新加载并展示节假日数据。 */
    public void refresh() {
        HolidayData data = store.load();
        tableModel.setRowCount(0);
        int index = 1;
        for (LocalDate date : data.getDates()) {
            String week = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINA);
            tableModel.addRow(new Object[]{index++, date.toString(), week});
        }

        int currentYear = LocalDate.now().getYear();
        if (data.size() == 0) {
            infoLabel.setText("尚未导入节假日信息。");
        } else if (data.getYear() != currentYear) {
            infoLabel.setText(String.format("已加载 %d 年的 %d 个节假日（当前是 %d 年，请重新导入）。",
                    data.getYear(), data.size(), currentYear));
        } else {
            infoLabel.setText(String.format("已加载 %d 年的 %d 个节假日。", data.getYear(), data.size()));
        }
    }

    /** 弹出文件选择框并从 Excel 导入节假日。 */
    public void importFromExcel() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("选择节假日 Excel 文件");
        chooser.setFileFilter(new FileNameExtensionFilter("Excel 文件 (*.xlsx, *.xls)", "xlsx", "xls"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path file = chooser.getSelectedFile().toPath();
        try {
            HolidayData data = store.importFromExcel(file);
            store.save(data);
            refresh();
            JOptionPane.showMessageDialog(this,
                    String.format("成功导入 %d 年的 %d 个节假日。", data.getYear(), data.size()),
                    "导入成功",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "导入失败",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
