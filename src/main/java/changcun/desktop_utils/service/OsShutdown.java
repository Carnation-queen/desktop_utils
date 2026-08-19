package changcun.desktop_utils.service;

import java.io.IOException;
import java.util.Locale;

/**
 * 跨平台执行“完全关机”命令。针对 Windows / macOS / Linux 做了适配。
 */
public final class OsShutdown {

    private OsShutdown() {
    }

    /**
     * 立即执行完全关机。失败时会依次尝试备用命令。
     */
    public static void shutdownNow() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            // /s 关机(不是重启/注销/休眠) /f 强制关闭应用 /t 0 不延迟
            run(new String[]{"shutdown", "/s", "/f", "/t", "0"});
        } else if (os.contains("mac")) {
            run(new String[]{"osascript", "-e", "tell application \"System Events\" to shut down"});
            run(new String[]{"shutdown", "-h", "now"});
        } else {
            // Linux 等类 Unix 系统
            run(new String[]{"shutdown", "-h", "now"});
            run(new String[]{"systemctl", "poweroff"});
        }
    }

    private static void run(String[] command) {
        try {
            new ProcessBuilder(command).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
