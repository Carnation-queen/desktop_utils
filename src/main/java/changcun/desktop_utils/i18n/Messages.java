package changcun.desktop_utils.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 轻量级国际化（i18n）支持，基于 JDK 自带能力，无第三方依赖。
 * <p>
 * 语言以字符串标识：{@link #LANG_SYSTEM system（跟随系统）}、{@link #LANG_ZH zh（简体中文）}、
 * {@link #LANG_EN en（English）}。文案存放在 {@code src/main/resources/i18n/}：
 * <ul>
 *   <li>{@code messages.properties} —— 中文（作为缺省回退包）；</li>
 *   <li>{@code messages_en.properties} —— 英文（缺失的键回退到中文包）。</li>
 * </ul>
 * 属性文件统一使用 UTF-8 编码。
 * </p>
 * <p>
 * 语言切换会通知所有已注册的监听器（{@link #addListener(Runnable)}），
 * UI 据此重建界面实现即时生效。
 * </p>
 */
public final class Messages {

    /** 跟随系统：按操作系统语言自动选择。 */
    public static final String LANG_SYSTEM = "system";
    /** 简体中文。 */
    public static final String LANG_ZH = "zh";
    /** 英文。 */
    public static final String LANG_EN = "en";

    private static final String BASE_RESOURCE = "/i18n/messages.properties";
    private static final String EN_RESOURCE = "/i18n/messages_en.properties";

    private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private static volatile Locale locale = resolveLocale(LANG_SYSTEM);
    private static volatile Map<String, String> strings = loadStrings(locale);

    private Messages() {
    }

    /** 程序启动时调用：按持久化的语言标识初始化界面语言。 */
    public static void init(String language) {
        setLanguage(language);
    }

    /**
     * 切换界面语言并通知监听器（由设置页在保存设置后调用）。
     * 语言未变化时不触发任何通知。
     */
    public static synchronized void setLanguage(String language) {
        Locale target = resolveLocale(language);
        if (target.equals(locale)) {
            return;
        }
        locale = target;
        strings = loadStrings(target);
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    /** 返回当前生效的 {@link Locale}（如用于星期等本地化显示）。 */
    public static Locale locale() {
        return locale;
    }

    /** 注册语言切换监听器（UI 重建等）。 */
    public static void addListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /** 移除语言切换监听器。 */
    public static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    /** 取当前语言下的文本（无参数）。 */
    public static String tr(String key) {
        return format(key, NO_ARGS);
    }

    /** 取当前语言下的文本并做占位符替换（使用 {@link MessageFormat} 的 {0} {1} 语法）。 */
    public static String tr(String key, Object... args) {
        return format(key, args);
    }

    private static final Object[] NO_ARGS = new Object[0];

    private static String format(String key, Object[] args) {
        String pattern = strings.get(key);
        if (pattern == null) {
            // 翻译缺失时原样返回键名，便于开发期排查。
            return "?" + key;
        }
        if (args == null || args.length == 0) {
            return pattern;
        }
        // 数值参数先转成普通十进制字符串，避免 MessageFormat 按数字格式化
        // 产生千位分隔符（如年份 2026 被显示成 2,026）。
        Object[] prepared = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object value = args[i];
            if (value instanceof Number) {
                prepared[i] = String.valueOf(value);
            } else {
                prepared[i] = value;
            }
        }
        return MessageFormat.format(pattern, prepared);
    }

    /** 将语言标识解析为具体 Locale；非中/英文的系统默认按英文处理。 */
    private static Locale resolveLocale(String language) {
        if (LANG_EN.equals(language)) {
            return Locale.ENGLISH;
        }
        if (LANG_ZH.equals(language)) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        Locale def = Locale.getDefault();
        String lang = def.getLanguage();
        if (lang != null && lang.toLowerCase(Locale.ROOT).startsWith("zh")) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        if (lang != null && lang.toLowerCase(Locale.ROOT).startsWith("en")) {
            return Locale.ENGLISH;
        }
        return Locale.ENGLISH;
    }

    /** 按目标语言加载文案映射；英文在中文基础上覆盖同名键（缺失自动回退中文）。 */
    private static Map<String, String> loadStrings(Locale target) {
        Map<String, String> map = new HashMap<>();
        loadInto(map, BASE_RESOURCE);
        if ("en".equals(target.getLanguage())) {
            loadInto(map, EN_RESOURCE);
        }
        return Collections.unmodifiableMap(map);
    }

    private static void loadInto(Map<String, String> map, String resource) {
        Properties props = new Properties();
        try (InputStream in = Messages.class.getResourceAsStream(resource)) {
            if (in == null) {
                return;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                props.load(reader);
            }
        } catch (IOException ignored) {
            // 资源读取失败时保留已有映射（缺省中文包），避免程序启动崩溃。
        }
        for (String key : props.stringPropertyNames()) {
            map.put(key, props.getProperty(key));
        }
    }
}

