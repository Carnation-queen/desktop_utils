package changcun.desktop_utils.model;

/**
 * 一次更新检查的结果。
 */
public class UpdateCheckResult {

    public enum Status {
        /** 当前已是最新版本。 */
        UP_TO_DATE,
        /** 发现新版本。 */
        UPDATE_AVAILABLE,
        /** 检查失败（网络错误、解析失败等）。 */
        ERROR
    }

    private final Status status;
    private final String currentVersion;
    private final UpdateInfo latest;
    private final String errorMessage;

    private UpdateCheckResult(Status status, String currentVersion, UpdateInfo latest, String errorMessage) {
        this.status = status;
        this.currentVersion = currentVersion;
        this.latest = latest;
        this.errorMessage = errorMessage;
    }

    public static UpdateCheckResult upToDate(String currentVersion, UpdateInfo latest) {
        return new UpdateCheckResult(Status.UP_TO_DATE, currentVersion, latest, null);
    }

    public static UpdateCheckResult updateAvailable(String currentVersion, UpdateInfo latest) {
        return new UpdateCheckResult(Status.UPDATE_AVAILABLE, currentVersion, latest, null);
    }

    public static UpdateCheckResult error(String message) {
        return new UpdateCheckResult(Status.ERROR, null, null, message);
    }

    public Status getStatus() {
        return status;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public UpdateInfo getLatest() {
        return latest;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
