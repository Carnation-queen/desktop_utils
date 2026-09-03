package changcun.desktop_utils.ui.novel;

import changcun.desktop_utils.model.NovelBook;
import changcun.desktop_utils.model.NovelChapter;
import changcun.desktop_utils.service.NovelStore;
import changcun.desktop_utils.ui.UiTheme;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 小说阅读视图：分页展示正文，支持上一页/下一页、目录跳转、字号调节与进度拖拽。
 * <p>
 * 阅读进度在每次翻页后延迟落盘（防抖），窗口关闭/返回书库时立即保存；
 * 下次打开同一本书会基于保存的字符偏移自动回到上次位置。
 */
public class ReaderPanel extends JPanel {

    private static final int DEFAULT_FONT_SIZE = 19;
    private static final int MIN_FONT_SIZE = 13;
    private static final int MAX_FONT_SIZE = 34;
    private static final int HISTORY_LIMIT = 400;
    private static final int SAVE_DELAY_MS = 400;

    private final NovelStore store;
    private final PageableTextPanel textPanel = new PageableTextPanel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel statusLabel = new JLabel();
    private final JSlider progressSlider = new JSlider(0, 1000);
    private final DefaultListModel<NovelChapter> tocModel = new DefaultListModel<>();
    private final JList<NovelChapter> tocList = new JList<>(tocModel);
    private final JButton prevButton = UiTheme.secondaryButton("‹ 上一页");
    private final JButton nextButton = UiTheme.secondaryButton("下一页 ›");
    private final JButton tocButton = UiTheme.secondaryButton("目录");
    private final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    private final JPanel tocWrap = new JPanel(new BorderLayout());
    private final Timer saveTimer;
    private final Deque<Integer> history = new ArrayDeque<>();

    private NovelBook book;
    private String content = "";
    private List<NovelChapter> chapters = Collections.emptyList();
    private int fontSize = DEFAULT_FONT_SIZE;
    private boolean updatingSlider = false;
    private boolean tocVisible = false;
    private Runnable backAction = () -> {
    };

    public ReaderPanel(NovelStore store) {
        super(new BorderLayout());
        this.store = store;
        this.saveTimer = new Timer(SAVE_DELAY_MS, e -> saveNow());
        saveTimer.setRepeats(false);

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
        bindKeys();
    }

    /** 打开并载入一本书（自动跳转到上次阅读进度）。 */
    public void loadBook(NovelBook target) {
        saveNow();
        book = target;
        history.clear();
        try {
            content = store.readContent(target);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "打开失败：" + ex.getMessage(),
                    "提示", JOptionPane.ERROR_MESSAGE);
            book = null;
            return;
        }
        chapters = NovelStore.detectChapters(content);

        titleLabel.setText(target.getTitle());
        textPanel.setContent(content);
        int start = (int) Math.min(Math.max(0L, target.getProgressChar()), content.length());
        textPanel.setStartOffset(start);
        textPanel.setReadingFont(readingFont(fontSize));
        populateToc();

        boolean hasChapters = !chapters.isEmpty();
        tocButton.setEnabled(hasChapters);
        if (!hasChapters && tocVisible) {
            setTocVisible(false);
        }
        updateFooter();
        focusReading();
    }

    /** 当窗口被重新显示且仍停留在阅读视图时调用，刷新状态。 */
    public void refreshView() {
        if (book != null) {
            updateFooter();
            repaint();
            focusReading();
        }
    }

    /** 立即保存进度（返回书库 / 窗口关闭时调用）。 */
    public void saveNow() {
        saveTimer.stop();
        if (book != null) {
            store.saveProgress(book.getId(), textPanel.getStartOffset());
        }
    }

    public void focusReading() {
        textPanel.requestFocusInWindow();
    }

    public void setBackAction(Runnable action) {
        this.backAction = action != null ? action : () -> {
        };
    }

    // ---------------------------------------------------------------------
    // UI 构建
    // ---------------------------------------------------------------------

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UiTheme.CARD_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(10, 14, 10, 14),
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.DIVIDER)));

        JButton back = UiTheme.secondaryButton("← 返回书库");
        back.addActionListener(e -> backAction.run());

        titleLabel.setForeground(UiTheme.TEXT_PRIMARY);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));

        JPanel west = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        west.setOpaque(false);
        west.add(back);
        west.add(titleLabel);

        JButton fontMinus = UiTheme.secondaryButton("A−");
        fontMinus.setToolTipText("减小字号");
        fontMinus.addActionListener(e -> adjustFontSize(-1));
        JButton fontPlus = UiTheme.secondaryButton("A＋");
        fontPlus.setToolTipText("增大字号");
        fontPlus.addActionListener(e -> adjustFontSize(1));
        tocButton.addActionListener(e -> toggleToc());

        JPanel east = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        east.setOpaque(false);
        east.add(tocButton);
        east.add(fontMinus);
        east.add(fontPlus);

        bar.add(west, BorderLayout.WEST);
        bar.add(east, BorderLayout.EAST);
        return bar;
    }

    private JComponent buildCenter() {
        // 目录区
        tocList.setFont(tocList.getFont().deriveFont(Font.PLAIN, 14f));
        tocList.setFixedCellHeight(28);
        tocList.setOpaque(false);
        tocList.setBackground(PageableTextPanel.PAPER_BG);
        tocList.setSelectionBackground(UiTheme.ACCENT_SOFT);
        tocList.setSelectionForeground(UiTheme.TEXT_PRIMARY);
        tocList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    NovelChapter c = tocList.getSelectedValue();
                    if (c != null) {
                        goTo(c.getStartOffset());
                    }
                }
            }
        });

        JLabel tocTitle = UiTheme.sectionTitle("章节目录");
        tocTitle.setBorder(new EmptyBorder(10, 12, 6, 12));
        JScrollPane tocScroll = new JScrollPane(tocList);
        tocScroll.setBorder(BorderFactory.createEmptyBorder());
        tocScroll.getViewport().setBackground(PageableTextPanel.PAPER_BG);
        tocWrap.setLayout(new BorderLayout());
        tocWrap.setBackground(PageableTextPanel.PAPER_BG);
        tocWrap.add(tocTitle, BorderLayout.NORTH);
        tocWrap.add(tocScroll, BorderLayout.CENTER);

        // 阅读区 + 可选目录
        splitPane.setContinuousLayout(true);
        splitPane.setOneTouchExpandable(false);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setDividerSize(0);
        splitPane.setRightComponent(textPanel);
        return splitPane;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setBackground(UiTheme.CARD_BG);
        footer.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(8, 12, 8, 12),
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.DIVIDER)));

        statusLabel.setForeground(UiTheme.TEXT_SECONDARY);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 13f));

        progressSlider.setToolTipText("拖动可快速跳转阅读进度");
        progressSlider.addChangeListener(e -> {
            if (!updatingSlider && !progressSlider.getValueIsAdjusting()) {
                jumpToPercent();
            }
        });

        prevButton.addActionListener(e -> pagePrev());
        nextButton.addActionListener(e -> pageNext());

        JPanel center = new JPanel(new BorderLayout(16, 0));
        center.setOpaque(false);
        center.add(statusLabel, BorderLayout.WEST);
        center.add(progressSlider, BorderLayout.CENTER);

        footer.add(prevButton, BorderLayout.WEST);
        footer.add(center, BorderLayout.CENTER);
        footer.add(nextButton, BorderLayout.EAST);
        return footer;
    }

    private void bindKeys() {
        bind(textPanel, KeyEvent.VK_PAGE_DOWN, this::pageNext);
        bind(textPanel, KeyEvent.VK_SPACE, this::pageNext);
        bind(textPanel, KeyEvent.VK_RIGHT, this::pageNext);
        bind(textPanel, KeyEvent.VK_PAGE_UP, this::pagePrev);
        bind(textPanel, KeyEvent.VK_LEFT, this::pagePrev);
        textPanel.addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0) {
                pagePrev();
            } else {
                pageNext();
            }
        });
    }

    private void bind(JComponent c, int keyCode, Runnable action) {
        String name = "novel." + keyCode;
        c.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keyCode, 0), name);
        c.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    // ---------------------------------------------------------------------
    // 目录
    // ---------------------------------------------------------------------

    private void populateToc() {
        tocModel.removeAllElements();
        for (NovelChapter chapter : chapters) {
            tocModel.addElement(chapter);
        }
    }

    private void toggleToc() {
        setTocVisible(!tocVisible);
    }

    private void setTocVisible(boolean visible) {
        tocVisible = visible;
        if (tocVisible) {
            if (splitPane.getLeftComponent() == null) {
                splitPane.setLeftComponent(tocWrap);
                splitPane.setDividerSize(6);
            }
            javax.swing.SwingUtilities.invokeLater(() -> {
                splitPane.setDividerLocation(250);
                splitPane.revalidate();
                splitPane.repaint();
            });
        } else {
            if (splitPane.getLeftComponent() != null) {
                splitPane.setLeftComponent(null);
                splitPane.setDividerSize(0);
            }
            splitPane.revalidate();
            splitPane.repaint();
            focusReading();
        }
    }

    // ---------------------------------------------------------------------
    // 翻页 / 跳转 / 进度
    // ---------------------------------------------------------------------

    private void pageNext() {
        pushHistory(textPanel.getStartOffset());
        if (!textPanel.goNext()) {
            history.pollLast(); // 已在最后一页，撤销入栈
            return;
        }
        afterNavigation();
    }

    private void pagePrev() {
        if (textPanel.getStartOffset() <= 0) {
            return;
        }
        Integer previous = history.pollLast();
        textPanel.jumpTo(previous == null ? 0 : previous);
        afterNavigation();
    }

    private void goTo(int offset) {
        if (content.isEmpty()) {
            return;
        }
        pushHistory(textPanel.getStartOffset());
        textPanel.jumpTo(Math.max(0, Math.min(offset, content.length())));
        afterNavigation();
    }

    private void jumpToPercent() {
        if (content.isEmpty()) {
            return;
        }
        long target = (long) progressSlider.getValue() * content.length() / 1000L;
        goTo((int) Math.min(target, content.length()));
    }

    private void afterNavigation() {
        updateFooter();
        scheduleSave();
        focusReading();
    }

    private void pushHistory(int offset) {
        history.addLast(offset);
        while (history.size() > HISTORY_LIMIT) {
            history.pollFirst();
        }
    }

    private void scheduleSave() {
        saveTimer.restart();
    }

    private void adjustFontSize(int delta) {
        fontSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, fontSize + delta));
        textPanel.setReadingFont(readingFont(fontSize));
        updateFooter();
        focusReading();
    }

    private void updateFooter() {
        textPanel.refreshLayout();
        int offset = textPanel.getStartOffset();
        int length = content.length();
        double percent = length == 0 ? 0.0 : offset * 100.0 / length;
        String chapter = currentChapterText(offset);
        statusLabel.setText(String.format("%s · 已读 %.1f%%", chapter, percent));

        prevButton.setEnabled(offset > 0);
        nextButton.setEnabled(!textPanel.isAtEnd());

        updatingSlider = true;
        progressSlider.setValue((int) Math.min(1000, Math.round(percent * 10)));
        updatingSlider = false;
    }

    private String currentChapterText(int offset) {
        if (chapters.isEmpty()) {
            return "阅读中";
        }
        for (int i = chapters.size() - 1; i >= 0; i--) {
            if (chapters.get(i).getStartOffset() <= offset) {
                return chapters.get(i).getTitle();
            }
        }
        return "开始";
    }

    // ---------------------------------------------------------------------
    // 字体
    // ---------------------------------------------------------------------

    private static Font readingFont(int size) {
        String family = pickReadingFamily();
        return new Font(family, Font.PLAIN, size);
    }

    private static String pickReadingFamily() {
        String[] preferred = {
                "Source Han Serif SC", "Noto Serif CJK SC", "SimSun", "NSimSun",
                "Songti SC", "STSong", "Microsoft YaHei", "PingFang SC",
                "KaiTi", "Microsoft JhengHei", "SimHei"
        };
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String family : preferred) {
            if (available.contains(family)) {
                return family;
            }
        }
        // 回退：使用 Swing 默认逻辑字体的中文可用家族
        return available.contains(Font.SERIF) ? Font.SERIF : Font.SANS_SERIF;
    }
}
