package changcun.desktop_utils.ui.novel;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页阅读文本面板。
 * <p>
 * 采用“真实排版测量”的分页方式：使用当前字号对应的 {@link FontMetrics}，
 * 对正文逐字符累积宽度、按实际可用宽度换行，再按可用高度切分整页行数。
 * 因此窗口缩放、字号变化时文字会自动重排，且不会溢出或截断。
 * <p>
 * 阅读位置以“解码后全文的字符偏移”记录，保存在 {@link #startOffset}：
 * <ul>
 *   <li>前进一页：{@link #goNext()} 将起点推进到下一页首行行首；</li>
 *   <li>后退一页：由外部维护“已访问页起点”历史栈回退；</li>
 *   <li>持久化：随时可读取 {@link #getStartOffset()} 落盘，重开后跳回同一位置。</li>
 * </ul>
 */
public class PageableTextPanel extends JComponent {

    /** 护眼纸张底色。 */
    public static final Color PAPER_BG = new Color(0xFAF5EA);
    /** 正文墨色。 */
    public static final Color PAPER_TEXT = new Color(0x332D24);

    private static final int MARGIN_X = 40;
    private static final int MARGIN_Y = 28;
    private static final int LINE_GAP = 9;

    private String content = "";
    private int startOffset = 0;

    /** 当前页待绘制的行片段：[起始偏移, 结束偏移)。 */
    private final List<int[]> lines = new ArrayList<>();
    private boolean atStart = true;
    private boolean atEnd = true;
    private int nextPageOffset = 0;

    public PageableTextPanel() {
        setOpaque(true);
        setBackground(PAPER_BG);
        setForeground(PAPER_TEXT);
        setFocusable(true);
    }

    /** 设置全文内容。 */
    public void setContent(String text) {
        content = text == null ? "" : text;
        if (startOffset > content.length()) {
            startOffset = 0;
        }
        repaint();
    }

    /** 设置阅读字号并重排。 */
    public void setReadingFont(Font font) {
        setFont(font);
        repaint();
    }

    /** 设置当前阅读起点（自动钳制到有效范围）。 */
    public void setStartOffset(int offset) {
        startOffset = clamp(offset, 0, content.length());
        repaint();
    }

    public int getStartOffset() {
        return startOffset;
    }

    /** 前进一页；若已在最后一页则返回 false。 */
    public boolean goNext() {
        relayout();
        if (atEnd || content.isEmpty()) {
            return false;
        }
        startOffset = nextPageOffset;
        repaint();
        return true;
    }

    /** 跳转到任意字符偏移（自动钳制）。 */
    public void jumpTo(int offset) {
        startOffset = clamp(offset, 0, content.length());
        repaint();
    }

    /** 依据当前尺寸/字号重新排版，供外部在读取分页边界前调用。 */
    public void refreshLayout() {
        relayout();
    }

    public boolean isAtStart() {
        return startOffset <= 0;
    }

    public boolean isAtEnd() {
        return atEnd;
    }

    public int getContentLength() {
        return content.length();
    }

    private int clamp(int value, int lo, int hi) {
        return value < lo ? lo : (value > hi ? hi : value);
    }

    /** 依据当前宽度/字号计算本页可容纳的行，并刷新分页边界。 */
    private void relayout() {
        lines.clear();
        int n = content.length();
        atStart = startOffset <= 0;
        if (n == 0) {
            atEnd = true;
            nextPageOffset = 0;
            return;
        }

        int availWidth = Math.max(80, getWidth() - 2 * MARGIN_X);
        int availHeight = Math.max(60, getHeight() - 2 * MARGIN_Y);
        FontMetrics fm = getFontMetrics(getFont());
        int lineHeight = fm.getHeight() + LINE_GAP;
        int maxLines = Math.max(1, availHeight / lineHeight);

        // 逐字符换行排版，最多生成 maxLines+1 行：多出的那一行仅用于确定下一页起点。
        List<int[]> tmp = new ArrayList<>();
        int pos = startOffset;
        int lineStart = startOffset;
        double width = 0;
        while (pos < n && tmp.size() <= maxLines) {
            int cp = content.codePointAt(pos);
            int cl = Character.charCount(cp);
            if (cp == '\r') {
                pos++;
                continue;
            }
            if (cp == '\n') {
                tmp.add(new int[]{lineStart, pos});
                pos++;
                lineStart = pos;
                width = 0;
                continue;
            }
            double cw = charWidth(fm, cp);
            if (width + cw > availWidth && pos > lineStart) {
                tmp.add(new int[]{lineStart, pos});
                lineStart = pos;
                width = 0;
                continue;
            }
            width += cw;
            pos += cl;
        }
        // 文本恰好结束且最后一段没有换行符时，补上最后一行。
        if (pos >= n && lineStart < n && tmp.size() <= maxLines) {
            tmp.add(new int[]{lineStart, n});
        }

        int draw = Math.min(maxLines, tmp.size());
        for (int i = 0; i < draw; i++) {
            lines.add(tmp.get(i));
        }
        if (tmp.size() > maxLines) {
            nextPageOffset = tmp.get(maxLines)[0];
            atEnd = false;
        } else {
            nextPageOffset = n;
            atEnd = true;
        }
    }

    private double charWidth(FontMetrics fm, int cp) {
        double cw;
        if (Character.isSupplementaryCodePoint(cp)) {
            cw = fm.charWidth('\u4E2D'); // 增补平面字符按全角宽度估算
        } else {
            cw = fm.charWidth((char) cp);
        }
        double min = fm.charWidth('\u4E2D') * 0.5;
        return Math.max(cw, min);
    }

    @Override
    protected void paintComponent(Graphics g) {
        relayout();
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (content.isEmpty()) {
                g2.setColor(PAPER_TEXT);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                String hint = "暂无内容";
                int x = (getWidth() - fm.stringWidth(hint)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(hint, x, y);
                return;
            }

            FontMetrics fm = g2.getFontMetrics(getFont());
            int lineHeight = fm.getHeight() + LINE_GAP;
            g2.setFont(getFont());
            g2.setColor(getForeground());
            int x = MARGIN_X;
            int y = MARGIN_Y + fm.getAscent();
            for (int[] seg : lines) {
                if (y > getHeight() - MARGIN_Y) {
                    break;
                }
                g2.drawString(content.substring(seg[0], seg[1]), x, y);
                y += lineHeight;
            }
        } finally {
            g2.dispose();
        }
    }
}
