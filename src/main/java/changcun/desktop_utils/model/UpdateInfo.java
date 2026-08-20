package changcun.desktop_utils.model;

/**
 * 更新接口返回的最新版本信息。
 */
public class UpdateInfo {

    /** 最新版本号（不含前缀 v）。 */
    private String version;

    /** 更新文件的下载地址（GitHub Releases 资产，可能为空）。 */
    private String downloadUrl;

    /** 发布页面地址（无下载资产时可引导用户打开）。 */
    private String pageUrl;

    /** 更新说明 / 发布日志。 */
    private String notes;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
