package changcun.desktop_utils.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 当前程序的版本号工具。
 * <p>
 * 版本号优先从打包进 jar 的 {@code version.properties}（由 Maven 过滤注入
 * {@code ${project.version}}）读取，其次回退到 MANIFEST 的实现版本，最后回退到内置默认值。
 * </p>
 */
public final class AppVersion {

    /** 兜底默认版本，仅在无法读取任何版本信息时使用。 */
    public static final String DEFAULT_VERSION = "1.0.1";

    private static final String RESOURCE = "/version.properties";
    private static final String KEY = "version";

    private static volatile String cached;

    private AppVersion() {
    }

    /** 返回当前程序版本字符串（不含前缀 v）。 */
    public static String current() {
        String v = cached;
        if (v != null) {
            return v;
        }

        v = readFromResource();
        if (v == null) {
            v = readFromManifest();
        }
        if (v == null || v.isBlank()) {
            v = DEFAULT_VERSION;
        }
        cached = v;
        return v;
    }

    private static String readFromResource() {
        try (InputStream in = AppVersion.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return null;
            }
            Properties props = new Properties();
            props.load(in);
            String raw = props.getProperty(KEY);
            if (raw == null) {
                return null;
            }
            String value = raw.trim();
            // Maven 过滤失败时会原样保留 ${project.version}，此时视为无效。
            if (value.isEmpty() || value.contains("${")) {
                return null;
            }
            return value;
        } catch (IOException e) {
            return null;
        }
    }

    private static String readFromManifest() {
        Package pkg = AppVersion.class.getPackage();
        if (pkg == null) {
            return null;
        }
        String impl = pkg.getImplementationVersion();
        return impl == null || impl.isBlank() ? null : impl.trim();
    }

    /**
     * 比较两个版本号，返回负数 / 0 / 正数，分别表示 a 小于 / 等于 / 大于 b。
     * 支持常见的 {@code 1.2.3} 形式，忽略前缀 {@code v/V} 以及 {@code -xxx} 预发布后缀。
     */
    public static int compare(String a, String b) {
        int[] pa = parse(a);
        int[] pb = parse(b);
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            int x = i < pa.length ? pa[i] : 0;
            int y = i < pb.length ? pb[i] : 0;
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return 0;
    }

    private static int[] parse(String version) {
        if (version == null) {
            return new int[0];
        }
        String s = version.trim();
        if (s.startsWith("v") || s.startsWith("V")) {
            s = s.substring(1);
        }
        int dash = s.indexOf('-');
        if (dash >= 0) {
            s = s.substring(0, dash);
        }
        String[] parts = s.split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String digits = parts[i].replaceAll("\\D", "");
            out[i] = digits.isEmpty() ? 0 : Integer.parseInt(digits);
        }
        return out;
    }
}
