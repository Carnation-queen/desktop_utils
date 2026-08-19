package changcun.desktop_utils.service;

import changcun.desktop_utils.Main;
import changcun.desktop_utils.model.AppSettings;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 开机自启动管理。
 * <p>
 * Windows 下通过写入当前用户的注册表 Run 项实现：
 * {@code HKCU\Software\Microsoft\Windows\CurrentVersion\Run}。
 * 其它平台暂不支持，设置页会禁用该开关。
 */
public class AutoStartManager {

    private static final String RUN_KEY =
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String VALUE_NAME = "DesktopUtils";

    private final AppSettingsStore store;

    public AutoStartManager(AppSettingsStore store) {
        this.store = store;
    }

    /** 当前系统是否支持开机自启动（仅 Windows）。 */
    public boolean isSupported() {
        return isWindows();
    }

    /** 返回持久化的自启动开关状态。 */
    public boolean isEnabled() {
        return store.load().isAutoStart();
    }

    /**
     * 设置开机自启动开关。
     *
     * @param enabled true 开启，false 关闭
     * @return 是否设置成功；仅当操作系统写入成功后才持久化状态
     */
    public boolean setEnabled(boolean enabled) {
        if (!isSupported()) {
            return false;
        }
        boolean ok = enabled ? registerAutoStart() : unregisterAutoStart();
        if (ok) {
            AppSettings settings = store.load();
            settings.setAutoStart(enabled);
            store.save(settings);
        }
        return ok;
    }

    /**
     * 启动时同步：若用户已开启自启动，则刷新注册表命令，
     * 确保其指向当前程序（jar 或编译输出目录）的实际位置。
     */
    public void syncOnStartup() {
        if (isSupported() && isEnabled()) {
            registerAutoStart();
        }
    }

    private boolean registerAutoStart() {
        String command = buildLaunchCommand();
        if (command == null) {
            return false;
        }
        return runReg("add", RUN_KEY, "/v", VALUE_NAME, "/t", "REG_SZ", "/d", command, "/f");
    }

    private boolean unregisterAutoStart() {
        // 从未写入过注册表时直接视为已关闭。
        if (!runReg("query", RUN_KEY, "/v", VALUE_NAME)) {
            return true;
        }
        return runReg("delete", RUN_KEY, "/v", VALUE_NAME, "/f");
    }

    private String buildLaunchCommand() {
        try {
            String javaExe = Paths.get(System.getProperty("java.home"),
                    "bin", isWindows() ? "javaw.exe" : "java").toString();
            String java = "\"" + javaExe + "\"";

            Path location = null;
            if (Main.class.getProtectionDomain().getCodeSource() != null) {
                location = Path.of(
                        Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            }
            if (location == null) {
                return null;
            }

            // 打包后的 shaded jar：javaw -jar xxx.jar
            if (location.toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                return java + " -jar \"" + location + "\"";
            }
            // 开发环境：javaw -cp target/classes 主类名
            return java + " -cp \"" + location + "\" " + Main.class.getName();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static boolean runReg(String... args) {
        String[] command = new String[args.length + 1];
        command[0] = "reg";
        System.arraycopy(args, 0, command, 1, args.length);

        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            process.getOutputStream().close();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
