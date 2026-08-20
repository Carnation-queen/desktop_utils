package changcun.desktop_utils.model;

/**
 * 程序级常规设置。
 */
public class AppSettings {

    /** GitHub Releases 更新源地址（默认仓库的 latest 接口）。 */
    public static final String DEFAULT_UPDATE_URL =
            "https://api.github.com/repos/Carnation-queen/desktop_utils/releases/latest";

    /** 是否开机自启动，默认关闭。 */
    private boolean autoStart = false;

    /** 是否自动检查更新，默认开启。 */
    private boolean autoUpdate = true;

    /** 更新源地址。 */
    private String updateUrl = DEFAULT_UPDATE_URL;

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }

    public String getUpdateUrl() {
        return updateUrl == null || updateUrl.isBlank() ? DEFAULT_UPDATE_URL : updateUrl;
    }

    public void setUpdateUrl(String updateUrl) {
        this.updateUrl = updateUrl == null || updateUrl.isBlank()
                ? DEFAULT_UPDATE_URL
                : updateUrl.trim();
    }
}
