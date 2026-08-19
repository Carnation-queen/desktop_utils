package changcun.desktop_utils.service;

import changcun.desktop_utils.model.ShutdownConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 负责把定时关机配置持久化到本地磁盘（半永久性、可修改）。
 * 存储位置：{@code ~/.desktop_utils/config.properties}
 */
public class SettingsStore {

    private static final String CONFIG_DIR = ".desktop_utils";
    private static final String CONFIG_FILE = "config.properties";

    private static final String KEY_MODE = "shutdown.mode";
    private static final String KEY_ONCE = "shutdown.once.epochMillis";
    private static final String KEY_DAILY_HOUR = "shutdown.daily.hour";
    private static final String KEY_DAILY_MINUTE = "shutdown.daily.minute";
    private static final String KEY_WORKDAY_HOUR = "shutdown.workday.hour";
    private static final String KEY_WORKDAY_MINUTE = "shutdown.workday.minute";

    private final Path configFile;

    public SettingsStore() {
        this(Paths.get(System.getProperty("user.home"), CONFIG_DIR, CONFIG_FILE));
    }

    public SettingsStore(Path configFile) {
        this.configFile = configFile;
    }

    public Path getConfigFile() {
        return configFile;
    }

    public ShutdownConfig load() {
        ShutdownConfig config = new ShutdownConfig();
        if (!Files.exists(configFile)) {
            return config;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configFile)) {
            props.load(in);
        } catch (IOException e) {
            // 读取失败时回退到默认配置，不影响程序启动。
            return config;
        }

        try {
            config.setMode(ShutdownConfig.Mode.valueOf(props.getProperty(KEY_MODE, ShutdownConfig.Mode.NONE.name())));
        } catch (IllegalArgumentException e) {
            config.setMode(ShutdownConfig.Mode.NONE);
        }
        config.setOnceEpochMillis(Long.parseLong(props.getProperty(KEY_ONCE, "-1")));
        config.setDailyHour(Integer.parseInt(props.getProperty(KEY_DAILY_HOUR, "22")));
        config.setDailyMinute(Integer.parseInt(props.getProperty(KEY_DAILY_MINUTE, "0")));
        config.setWorkdayHour(Integer.parseInt(props.getProperty(KEY_WORKDAY_HOUR, "22")));
        config.setWorkdayMinute(Integer.parseInt(props.getProperty(KEY_WORKDAY_MINUTE, "0")));
        return config;
    }

    public void save(ShutdownConfig config) {
        Properties props = new Properties();
        props.setProperty(KEY_MODE, config.getMode().name());
        props.setProperty(KEY_ONCE, String.valueOf(config.getOnceEpochMillis()));
        props.setProperty(KEY_DAILY_HOUR, String.valueOf(config.getDailyHour()));
        props.setProperty(KEY_DAILY_MINUTE, String.valueOf(config.getDailyMinute()));
        props.setProperty(KEY_WORKDAY_HOUR, String.valueOf(config.getWorkdayHour()));
        props.setProperty(KEY_WORKDAY_MINUTE, String.valueOf(config.getWorkdayMinute()));

        try {
            Files.createDirectories(configFile.getParent());
            try (OutputStream out = Files.newOutputStream(configFile)) {
                props.store(out, "Desktop Utils - shutdown schedule settings");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
