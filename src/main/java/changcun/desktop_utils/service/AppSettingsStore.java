package changcun.desktop_utils.service;

import changcun.desktop_utils.model.AppSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 负责程序级常规设置（如开机自启动）的本地持久化。
 * 存储位置：{@code ~/.desktop_utils/app.properties}
 */
public class AppSettingsStore {

    private static final String CONFIG_DIR = ".desktop_utils";
    private static final String CONFIG_FILE = "app.properties";

    private static final String KEY_AUTO_START = "app.autoStart";

    private final Path configFile;

    public AppSettingsStore() {
        this(Paths.get(System.getProperty("user.home"), CONFIG_DIR, CONFIG_FILE));
    }

    public AppSettingsStore(Path configFile) {
        this.configFile = configFile;
    }

    public Path getConfigFile() {
        return configFile;
    }

    public AppSettings load() {
        AppSettings settings = new AppSettings();
        if (!Files.exists(configFile)) {
            return settings;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configFile)) {
            props.load(in);
        } catch (IOException e) {
            // 读取失败时回退到默认值，不影响程序启动。
            return settings;
        }

        settings.setAutoStart(Boolean.parseBoolean(props.getProperty(KEY_AUTO_START, "false")));
        return settings;
    }

    public void save(AppSettings settings) {
        Properties props = new Properties();
        props.setProperty(KEY_AUTO_START, String.valueOf(settings.isAutoStart()));

        try {
            Files.createDirectories(configFile.getParent());
            try (OutputStream out = Files.newOutputStream(configFile)) {
                props.store(out, "Desktop Utils - app settings");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
