package changcun.desktop_utils.ui.novel;

import changcun.desktop_utils.i18n.Messages;
import changcun.desktop_utils.model.NovelBook;
import changcun.desktop_utils.service.NovelStore;
import changcun.desktop_utils.ui.UiTheme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/**
 * 小说书库视图：展示已导入小说列表，支持导入 TXT、打开阅读、重命名与删除。
 */
public class LibraryPanel extends JPanel {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private final NovelStore store;
    private final Consumer<NovelBook> openAction;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel infoLabel = new JLabel();
    private List<NovelBook> books = List.of();

    public LibraryPanel(NovelStore store, Consumer<NovelBook> openAction) {
        this.store = store;
        this.openAction = openAction;

        setLayout(new BorderLayout(0, 16));
        setBackground(UiTheme.WINDOW_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(buildHeader(), BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{
                Messages.tr("novel.col.title"),
                Messages.tr("novel.col.progress"),
                Messages.tr("novel.col.lastRead")}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        configureTable(table);

        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(UiTheme.CARD_BG);
        card.add(scroll, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

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
        text.add(UiTheme.title(Messages.tr("novel.library.title")));
        JLabel subtitle = UiTheme.subtitle(Messages.tr("novel.library.subtitle"));
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        text.add(subtitle);
        header.add(text, BorderLayout.WEST);

        JButton importButton = UiTheme.primaryButton(Messages.tr("novel.import"));
        importButton.addActionListener(e -> importBooks());
        JButton openButton = UiTheme.secondaryButton(Messages.tr("novel.open"));
        openButton.addActionListener(e -> openSelected());
        JButton renameButton = UiTheme.secondaryButton(Messages.tr("novel.rename"));
        renameButton.addActionListener(e -> renameSelected());
        JButton deleteButton = UiTheme.secondaryButton(Messages.tr("novel.delete"));
        deleteButton.addActionListener(e -> deleteSelected());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(importButton);
        buttons.add(openButton);
        buttons.add(renameButton);
        buttons.add(deleteButton);
        header.add(buttons, BorderLayout.EAST);
        return header;
    }

    private void configureTable(JTable t) {
        t.setFillsViewportHeight(true);
        t.setRowHeight(32);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD, 13f));
        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelected();
                }
            }
        });
    }

    /** 从 Store 重新加载并刷新列表。 */
    public void refresh() {
        books = store.listBooks();
        tableModel.setRowCount(0);
        for (NovelBook book : books) {
            tableModel.addRow(new Object[]{book.getTitle(), progressText(book), timeText(book.getUpdatedAt())});
        }
        infoLabel.setText(books.isEmpty()
                ? Messages.tr("novel.library.empty")
                : Messages.tr("novel.library.count", books.size()));
    }

    private NovelBook selectedBook() {
        int row = table.getSelectedRow();
        return row >= 0 && row < books.size() ? books.get(row) : null;
    }

    private void importBooks() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(Messages.tr("novel.import.chooserTitle"));
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter(Messages.tr("novel.import.filter"), "txt"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        int imported = 0;
        int failed = 0;
        StringBuilder error = new StringBuilder();
        for (java.io.File file : chooser.getSelectedFiles()) {
            try {
                store.importBook(null, file.toPath());
                imported++;
            } catch (IOException ex) {
                failed++;
                error.append(file.getName()).append("：").append(ex.getMessage()).append('\n');
            }
        }
        refresh();
        if (failed > 0) {
            JOptionPane.showMessageDialog(this,
                    Messages.tr("novel.import.partialFail", error.toString().stripTrailing()),
                    Messages.tr("novel.import.resultTitle"), JOptionPane.WARNING_MESSAGE);
        } else if (imported > 0) {
            JOptionPane.showMessageDialog(this,
                    Messages.tr("novel.import.success", imported),
                    Messages.tr("novel.import.successTitle"), JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void openSelected() {
        NovelBook book = selectedBook();
        if (book != null) {
            openAction.accept(book);
        }
    }

    private void renameSelected() {
        NovelBook book = selectedBook();
        if (book == null) {
            return;
        }
        String title = JOptionPane.showInputDialog(this,
                Messages.tr("novel.rename.prompt"),
                Messages.tr("novel.rename.title"), JOptionPane.PLAIN_MESSAGE);
        if (title != null && !title.isBlank()) {
            store.renameBook(book.getId(), title);
            refresh();
        }
    }

    private void deleteSelected() {
        NovelBook book = selectedBook();
        if (book == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                Messages.tr("novel.delete.confirm", book.getTitle()),
                Messages.tr("novel.delete.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            store.deleteBook(book.getId());
            refresh();
        }
    }

    private static String progressText(NovelBook book) {
        if (book.getCharCount() <= 0) {
            return "—";
        }
        return String.format("%.1f%%", book.progressPercent());
    }

    private static String timeText(long millis) {
        return millis <= 0 ? "—" : TIME_FORMAT.format(new Date(millis));
    }
}
