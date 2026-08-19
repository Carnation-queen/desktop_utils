package changcun.desktop_utils.model;

/**
 * 程序级常规设置。
 */
public class AppSettings {

    /** 是否开机自启动，默认关闭。 */
    private boolean autoStart = false;

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }
}
