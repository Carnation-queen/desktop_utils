package changcun.desktop_utils.i18n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验国际化资源包：中文缺省包与英文包拥有完全一致的键集合，
 * 且语言切换能正确解析出对应文案。
 */
class MessagesTest {

    @AfterEach
    void resetLanguage() {
        Messages.setLanguage(Messages.LANG_SYSTEM);
    }

    @Test
    void zhAndEnBundlesHaveSameKeys() throws Exception {
        Set<String> zh = loadKeys("i18n/messages.properties");
        Set<String> en = loadKeys("i18n/messages_en.properties");
        assertEquals(zh, en,
                "中文与英文资源包键集合不一致。缺失(中文有/英文无): "
                        + new TreeSet<>(zh) + " vs " + new TreeSet<>(en));
    }

    @Test
    void switchToEnglishReturnsEnglishText() {
        Messages.setLanguage(Messages.LANG_EN);
        assertEquals("Desktop Utils", Messages.tr("app.name"));
        assertEquals("Settings", Messages.tr("settings.title"));
        assertEquals("Check for Updates", Messages.tr("about.check"));
    }

    @Test
    void switchToChineseReturnsChineseText() {
        Messages.setLanguage(Messages.LANG_ZH);
        assertEquals("桌面工具", Messages.tr("app.name"));
        assertEquals("设置", Messages.tr("settings.title"));
        assertEquals("检查更新", Messages.tr("about.check"));
    }

    @Test
    void formatPlaceholdersWork() {
        Messages.setLanguage(Messages.LANG_EN);
        String out = Messages.tr("holiday.status.loaded", 2026, 12);
        assertTrue(out.contains("2026"), out);
        assertTrue(out.contains("12"), out);
    }

    @Test
    void missingKeyReturnsKeyMarker() {
        Messages.setLanguage(Messages.LANG_EN);
        assertEquals("?no.such.key", Messages.tr("no.such.key"));
    }

    @Test
    void unknownLanguageFallsBackToSystemOrEnglish() {
        // 未知语言标识按“跟随系统”处理，结果不应是占位符。
        Messages.setLanguage("xx");
        assertFalse(Messages.tr("app.name").startsWith("?"));
    }

    private static Set<String> loadKeys(String resource) throws Exception {
        Set<String> keys = new TreeSet<>();
        try (java.io.InputStream in = MessagesTest.class.getClassLoader().getResourceAsStream(resource)) {
            ResourceBundle bundle = new PropertyResourceBundle(
                    new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            keys.addAll(bundle.keySet());
        }
        return keys;
    }
}
