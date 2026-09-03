package changcun.desktop_utils.model;

/**
 * 一本已导入书库的小说（含阅读进度等元信息）。
 * <p>
 * 阅读进度以“解码后全文的字符偏移”记录，由 {@code NovelStore} 负责持久化；
 * 打开书籍时据此自动跳转到上次阅读位置。
 */
public class NovelBook {

    /** 唯一标识，同时用作书籍内容文件名 {@code books/<id>.txt}。 */
    private String id = "";

    /** 书名。 */
    private String title = "";

    /** 导入时的原始文件名（仅用于展示，不参与存储）。 */
    private String fileName = "";

    /** 解码后的总字符数。 */
    private long charCount = 0;

    /** 阅读进度（解码后全文的字符偏移）。 */
    private long progressChar = 0;

    /** 加入书库的时间（epoch 毫秒）。 */
    private long addedAt = 0;

    /** 最近一次阅读时间（epoch 毫秒）。 */
    private long updatedAt = 0;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null ? "" : id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName == null ? "" : fileName;
    }

    public long getCharCount() {
        return charCount;
    }

    public void setCharCount(long charCount) {
        this.charCount = Math.max(0, charCount);
    }

    public long getProgressChar() {
        return progressChar;
    }

    public void setProgressChar(long progressChar) {
        this.progressChar = Math.max(0, progressChar);
    }

    public long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** 阅读进度百分比（0 ~ 100），供书库列表展示。 */
    public double progressPercent() {
        if (charCount <= 0) {
            return 0;
        }
        return Math.min(100.0, progressChar * 100.0 / charCount);
    }
}
