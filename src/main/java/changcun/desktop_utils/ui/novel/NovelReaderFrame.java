package changcun.desktop_utils.ui.novel;

import changcun.desktop_utils.model.NovelBook;
import changcun.desktop_utils.service.NovelStore;
import changcun.desktop_utils.ui.AppIcon;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.CardLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 独立的小说阅读器窗口（由主窗口 Ctrl+Alt+Shift+F12 唤起）。
 * <p>
 * 内部以 {@link CardLayout} 切换两个视图：
 * <ul>
 *   <li>“书库”：导入 / 列表 / 重命名 / 删除 / 打开（{@link LibraryPanel}）；</li>
 *   <li>“阅读”：分页正文、目录跳转、进度记忆（{@link ReaderPanel}）。</li>
 * </ul>
 * 窗口关闭等于隐藏（不退出程序）；再次呼出时会回到上次停留的视图并自动续读。
 */
public class NovelReaderFrame extends JFrame {

    private static final KeyStroke TOGGLE_KEY = KeyStroke.getKeyStroke(
            KeyEvent.VK_F12,
            InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);

    private final NovelStore store;
    private final CardLayout cards = new CardLayout();
    private final JPanel deck = new JPanel(cards);
    private final LibraryPanel libraryPanel;
    private ReaderPanel readerPanel;
    private String lastCard = "library";

    public NovelReaderFrame(NovelStore store) {
        super("小说阅读器");
        this.store = store;
        setIconImage(AppIcon.windowIcon());
        setSize(1020, 760);
        setMinimumSize(new java.awt.Dimension(780, 580));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        libraryPanel = new LibraryPanel(store, this::openBook);
        deck.add(libraryPanel, "library");
        setContentPane(deck);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveIfReading();
            }
        });

        // 阅读器窗口内再次按同一快捷键 = 隐藏（并保存进度）。
        getRootPane().registerKeyboardAction(e -> {
            saveIfReading();
            setVisible(false);
        }, TOGGLE_KEY, JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** 呼出阅读器：优先回到上次停留的视图，否则显示书库。 */
    public void open() {
        if (readerPanel != null && "reader".equals(lastCard)) {
            cards.show(deck, "reader");
            readerPanel.refreshView();
        } else {
            cards.show(deck, "library");
            libraryPanel.refresh();
        }
        setVisible(true);
        toFront();
    }

    /** 打开某本书进入阅读视图。 */
    public void openBook(NovelBook book) {
        if (readerPanel == null) {
            readerPanel = new ReaderPanel(store);
            readerPanel.setBackAction(this::backToLibrary);
            deck.add(readerPanel, "reader");
        }
        readerPanel.loadBook(book);
        lastCard = "reader";
        cards.show(deck, "reader");
        setVisible(true);
        toFront();
        readerPanel.focusReading();
    }

    /** 返回书库视图（立即保存当前阅读进度）。 */
    public void backToLibrary() {
        if (readerPanel != null) {
            readerPanel.saveNow();
        }
        lastCard = "library";
        cards.show(deck, "library");
        libraryPanel.refresh();
    }

    private void saveIfReading() {
        if (readerPanel != null) {
            readerPanel.saveNow();
        }
    }
}
