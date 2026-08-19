package changcun.desktop_utils.service;

import changcun.desktop_utils.model.HolidayData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 节假日数据的本地持久化与 Excel 导入。
 * 存储位置：{@code ~/.desktop_utils/holidays.properties}
 * 同时记录“1 月 1 日”和“12 月 31 日”提醒是否已提示，避免重复弹窗。
 */
public class HolidayStore {

    private static final String CONFIG_DIR = ".desktop_utils";
    private static final String CONFIG_FILE = "holidays.properties";

    private static final String KEY_YEAR = "holiday.year";
    private static final String KEY_DATES = "holiday.dates";
    private static final String KEY_REMIND_JAN1 = "reminder.jan1.year";
    private static final String KEY_REMIND_DEC31 = "reminder.dec31.year";

    private final Path configFile;

    public HolidayStore() {
        this(Paths.get(System.getProperty("user.home"), CONFIG_DIR, CONFIG_FILE));
    }

    public HolidayStore(Path configFile) {
        this.configFile = configFile;
    }

    public Path getConfigFile() {
        return configFile;
    }

    public HolidayData load() {
        HolidayData data = new HolidayData();
        if (!Files.exists(configFile)) {
            return data;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configFile)) {
            props.load(in);
        } catch (IOException e) {
            return data;
        }

        data.setYear(parseInt(props.getProperty(KEY_YEAR), LocalDate.now().getYear()));
        String datesStr = props.getProperty(KEY_DATES, "");
        for (String part : datesStr.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                data.addDate(LocalDate.parse(s));
            } catch (DateTimeParseException ignored) {
                // 忽略无法解析的条目
            }
        }
        return data;
    }

    public void save(HolidayData data) {
        Properties props = loadPropertiesForMerge();
        props.setProperty(KEY_YEAR, String.valueOf(data.getYear()));

        StringBuilder sb = new StringBuilder();
        for (LocalDate d : data.getDates()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(d.toString());
        }
        props.setProperty(KEY_DATES, sb.toString());

        try {
            Files.createDirectories(configFile.getParent());
            try (OutputStream out = Files.newOutputStream(configFile)) {
                props.store(out, "Desktop Utils - holiday data");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getJan1RemindedYear() {
        return parseInt(loadPropertiesForMerge().getProperty(KEY_REMIND_JAN1), -1);
    }

    public void setJan1RemindedYear(int year) {
        Properties props = loadPropertiesForMerge();
        props.setProperty(KEY_REMIND_JAN1, String.valueOf(year));
        saveProperties(props);
    }

    public int getDec31RemindedYear() {
        return parseInt(loadPropertiesForMerge().getProperty(KEY_REMIND_DEC31), -1);
    }

    public void setDec31RemindedYear(int year) {
        Properties props = loadPropertiesForMerge();
        props.setProperty(KEY_REMIND_DEC31, String.valueOf(year));
        saveProperties(props);
    }

    /**
     * 从 Excel 第一个工作表的首列解析日期，返回解析结果（尚未落盘）。
     * 年份取出现次数最多的年份，若无法判断则使用当前年份。
     */
    public HolidayData importFromExcel(Path excelFile) throws IOException {
        HolidayData data = new HolidayData();
        try (Workbook wb = WorkbookFactory.create(Files.newInputStream(excelFile))) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                throw new IOException("Excel 中没有工作表。");
            }
            for (Row row : sheet) {
                if (row == null) {
                    continue;
                }
                Cell cell = row.getCell(0);
                LocalDate date = parseCellAsDate(cell);
                if (date != null) {
                    data.addDate(date);
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("读取 Excel 失败：" + e.getMessage(), e);
        }

        if (data.size() == 0) {
            throw new IOException("未在 Excel 第一个工作表的首列解析到任何日期。\n"
                    + "请确保首列为日期（支持 yyyy-MM-dd、yyyy/M/d 等常见格式）。");
        }
        data.setYear(dominantYear(data.getDates()));
        return data;
    }

    private LocalDate parseCellAsDate(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                try {
                    return cell.getLocalDateTimeCellValue().toLocalDate();
                } catch (Exception ignored) {
                    // 继续尝试其它方式
                }
            }
            // Excel 日期序列号（未格式化）也尝试按日期转换
            try {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            } catch (Exception ignored) {
                // 非日期数字，忽略
            }
        }
        if (cell.getCellType() == CellType.STRING) {
            String s = cell.getStringCellValue().trim();
            return parseDateString(s);
        }
        return null;
    }

    private static LocalDate parseDateString(String s) {
        // 带年份的格式
        for (String pattern : new String[]{
                "yyyy-MM-dd", "yyyy/M/d", "yyyy.M.d", "yyyy年M月d日",
                "yyyyMMdd", "M/d/yyyy", "M-d-yyyy", "yyyy-MM-dd HH:mm:ss"}) {
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {
                // 尝试下一个格式
            }
        }
        // 无年份格式，默认使用当前年份
        int year = LocalDate.now().getYear();
        for (String pattern : new String[]{"M月d日", "M/d", "M-d"}) {
            try {
                DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .appendPattern(pattern)
                        .parseDefaulting(ChronoField.YEAR, year)
                        .toFormatter();
                return LocalDate.parse(s, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一个格式
            }
        }
        return null;
    }

    private static int dominantYear(java.util.SortedSet<LocalDate> dates) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (LocalDate d : dates) {
            counts.merge(d.getYear(), 1, Integer::sum);
        }
        int bestYear = LocalDate.now().getYear();
        int bestCount = -1;
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount) {
                bestCount = e.getValue();
                bestYear = e.getKey();
            }
        }
        return bestYear;
    }

    private Properties loadPropertiesForMerge() {
        Properties props = new Properties();
        if (Files.exists(configFile)) {
            try (InputStream in = Files.newInputStream(configFile)) {
                props.load(in);
            } catch (IOException ignored) {
                // 读取失败时使用空属性
            }
        }
        return props;
    }

    private void saveProperties(Properties props) {
        try {
            Files.createDirectories(configFile.getParent());
            try (OutputStream out = Files.newOutputStream(configFile)) {
                props.store(out, "Desktop Utils - holiday data");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
