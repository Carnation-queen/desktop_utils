package changcun.desktop_utils.model;

/**
 * 小说目录条目。字符偏移基于“解码后的全文”，用于点击目录后精确跳转。
 */
public class NovelChapter {

    private final String title;
    private final int startOffset;

    public NovelChapter(String title, int startOffset) {
        this.title = title == null ? "" : title.trim();
        this.startOffset = Math.max(0, startOffset);
    }

    public String getTitle() {
        return title;
    }

    public int getStartOffset() {
        return startOffset;
    }

    @Override
    public String toString() {
        return title;
    }
}
