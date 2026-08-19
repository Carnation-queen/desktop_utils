package changcun.desktop_utils.service;

import changcun.desktop_utils.Main;
import changcun.desktop_utils.model.AppSettings;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 开机自启动管理，支持 Windows / macOS / Linux。
 * <ul>
 *   <li>Windows：写入当前用户注册表 Run 项（通过 .reg 文件导入，避免命令行引号转义问题）。</li>
 *   <li>macOS：写入 ~/Library/LaunchAgents 下的 LaunchAgent plist。</li>
 *   <li>Linux：写入 ~/.config/autostart 下的 .desktop 文件。</li>
 * </ul>
 */
public class AutoStartManager {

    private static final String WIN_RUN_KEY =
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String VALUE_NAME = "DesktopUtils";
    private static final String MAC_LABEL = "com.changcun.desktop-utils";

    private final AppSettingsStore store;

    public AutoStartManager(AppSettingsStore store) {
        this.store = store;
    }

    /** 当前系统是否支持开机自启动。 */
    public boolean isSupported() {
        return isWindows() || isMac() || isLinux();
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
     * 启动时同步：若用户已开启自启动，则重新写入命令，
     * 确保其指向当前程序（jar 或编译输出目录）的实际位置。
     */
    public void syncOnStartup() {
        if (isSupported() && isEnabled()) {
            registerAutoStart();
        }
    }

    private boolean registerAutoStart() {
        if (isWindows()) {
            return registerWindows();
        }
        if (isMac()) {
            return registerMac();
        }
        if (isLinux()) {
            return registerLinux();
        }
        return false;
    }

    private boolean unregisterAutoStart() {
        if (isWindows()) {
            return unregisterWindows();
        }
        if (isMac()) {
            return unregisterMac();
        }
        if (isLinux()) {
            return unregisterLinux();
        }
        return false;
    }

    // ---------- Windows ----------

    private boolean registerWindows() {
        String commandLine = buildWindowsCommandLine();
        if (commandLine == null) {
            return false;
        }

        String regContent = "Windows Registry Editor Version 5.00\r\n"
                + "\r\n"
                + "[HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run]\r\n"
                + "\"" + VALUE_NAME + "\"=\"" + escapeRegValue(commandLine) + "\"\r\n";

        Path regFile = null;
        try {
            regFile = Files.createTempFile("desktop-utils-autostart", ".reg");
            Files.write(regFile, toUtf16LeBom(regContent));
            return runReg("import", regFile.toString());
        } catch (IOException e) {
            return false;
        } finally {
            if (regFile != null) {
                try {
                    Files.deleteIfExists(regFile);
                } catch (IOException ignored) {
                    // 临时文件清理失败不影响结果。
                }
            }
        }
    }

    private boolean unregisterWindows() {
        // 值不存在时视为已关闭。
        if (!runReg("query", WIN_RUN_KEY, "/v", VALUE_NAME)) {
            return true;
        }
        return runReg("delete", WIN_RUN_KEY, "/v", VALUE_NAME, "/f");
    }

    private String buildWindowsCommandLine() {
        String[] parts = buildLaunchCommandParts();
        if (parts == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            String p = parts[i];
            if (p.contains(" ")) {
                sb.append('"').append(p).append('"');
            } else {
                sb.append(p);
            }
        }
        return sb.toString();
    }

    // ---------- macOS ----------

    private boolean registerMac() {
        String[] parts = buildLaunchCommandParts();
        if (parts == null) {
            return false;
        }
        Path plist = macPlistPath();
        try {
            Files.createDirectories(plist.getParent());
            Files.writeString(plist, buildPlist(parts), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean unregisterMac() {
        // 尽力卸载可能已加载的 LaunchAgent（未加载时失败可忽略）。
        runLaunchctl("unload", macPlistPath().toString());
        try {
            return Files.deleteIfExists(macPlistPath()) || !Files.exists(macPlistPath());
        } catch (IOException e) {
            return false;
        }
    }

    private Path macPlistPath() {
        return Paths.get(System.getProperty("user.home"),
                "Library", "LaunchAgents", "desktop-utils.plist");
    }

    private static String buildPlist(String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
                + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n");
        sb.append("<plist version=\"1.0\">\n");
        sb.append("<dict>\n");
        sb.append("    <key>Label</key>\n");
        sb.append("    <string>").append(MAC_LABEL).append("</string>\n");
        sb.append("    <key>ProgramArguments</key>\n");
        sb.append("    <array>\n");
        for (String arg : args) {
            sb.append("        <string>").append(escapeXml(arg)).append("</string>\n");
        }
        sb.append("    </array>\n");
        sb.append("    <key>RunAtLoad</key>\n");
        sb.append("    <true/>\n");
        sb.append("</dict>\n");
        sb.append("</plist>\n");
        return sb.toString();
    }

    // ---------- Linux ----------

    private boolean registerLinux() {
        String[] parts = buildLaunchCommandParts();
        if (parts == null) {
            return false;
        }
        Path desktopFile = linuxDesktopPath();
        try {
            Files.createDirectories(desktopFile.getParent());
            Files.writeString(desktopFile, buildDesktopEntry(parts), StandardCharsets.UTF_8);
            // 部分桌面环境要求可执行权限才会加载该文件。
            desktopFile.toFile().setExecutable(true, false);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean unregisterLinux() {
        try {
            return Files.deleteIfExists(linuxDesktopPath()) || !Files.exists(linuxDesktopPath());
        } catch (IOException e) {
            return false;
        }
    }

    private Path linuxDesktopPath() {
        return Paths.get(System.getProperty("user.home"),
                ".config", "autostart", "desktop-utils.desktop");
    }

    private static String buildDesktopEntry(String[] args) {
        StringBuilder exec = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                exec.append(' ');
            }
            exec.append(desktopExecArg(args[i]));
        }
        return "[Desktop Entry]\n"
                + "Type=Application\n"
                + "Name=Desktop Utils\n"
                + "Comment=桌面工具\n"
                + "Exec=" + exec + "\n"
                + "Terminal=false\n"
                + "X-GNOME-Autostart-enabled=true\n";
    }

    // ---------- 通用 ----------

    /** 构建启动命令参数：{java 可执行文件, -jar, jar 路径} 或 {java, -cp, 类路径, 主类}。 */
    private String[] buildLaunchCommandParts() {
        try {
            String javaExe = Paths.get(System.getProperty("java.home"),
                    "bin", isWindows() ? "javaw.exe" : "java").toString();

            Path location = null;
            if (Main.class.getProtectionDomain().getCodeSource() != null) {
                location = Path.of(
                        Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            }
            if (location == null) {
                return null;
            }

            if (location.toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                return new String[]{javaExe, "-jar", location.toString()};
            }
            return new String[]{javaExe, "-cp", location.toString(), Main.class.getName()};
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static String escapeRegValue(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\') {
                sb.append("\\\\");
            } else if (ch == '"') {
                sb.append("\\\"");
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static byte[] toUtf16LeBom(String content) {
        byte[] bom = {(byte) 0xFF, (byte) 0xFE};
        byte[] body = content.getBytes(StandardCharsets.UTF_16LE);
        byte[] out = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(body, 0, out, bom.length, body.length);
        return out;
    }

    private static String escapeXml(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }

    private static String desktopExecArg(String arg) {
        boolean needQuote = false;
        for (int i = 0; i < arg.length(); i++) {
            char ch = arg.charAt(i);
            if (Character.isWhitespace(ch)
                    || ch == '"' || ch == '\'' || ch == '\\'
                    || ch == '>' || ch == '<' || ch == '~'
                    || ch == '|' || ch == '&' || ch == ';'
                    || ch == '$' || ch == '*' || ch == '?'
                    || ch == '#' || ch == '(' || ch == ')'
                    || ch == '`') {
                needQuote = true;
                break;
            }
        }
        if (!needQuote) {
            return arg;
        }
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < arg.length(); i++) {
            char ch = arg.charAt(i);
            if (ch == '"' || ch == '\\' || ch == '`' || ch == '$') {
                sb.append('\\');
            }
            sb.append(ch);
        }
        sb.append('"');
        return sb.toString();
    }

    private static boolean runReg(String... args) {
        return runCommand("reg", args);
    }

    private static boolean runLaunchctl(String... args) {
        return runCommand("launchctl", args);
    }

    private static boolean runCommand(String executable, String... args) {
        List<String> command = new ArrayList<>();
        command.add(executable);
        for (String arg : args) {
            command.add(arg);
        }
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

    private static boolean isMac() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("mac") || os.contains("darwin");
    }

    private static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    }
}
