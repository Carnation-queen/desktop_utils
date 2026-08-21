package changcun.desktop_utils.service;

import changcun.desktop_utils.model.HolidayData;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HolidayStoreTest {

    @TempDir
    Path tempDir;

    private Path configFile() {
        return tempDir.resolve("holidays.properties");
    }

    @Test
    void loadReturnsEmptyWhenFileMissing() {
        HolidayStore store = new HolidayStore(configFile());
        HolidayData data = store.load();
        assertEquals(0, data.size());
        assertEquals(LocalDate.now().getYear(), data.getYear());
    }

    @Test
    void saveAndLoadRoundTrip() {
        HolidayStore store = new HolidayStore(configFile());
        HolidayData data = new HolidayData();
        data.setYear(2026);
        data.addDate(LocalDate.of(2026, 5, 1));
        data.addDate(LocalDate.of(2026, 10, 1));

        store.save(data);

        HolidayData loaded = store.load();
        assertEquals(2026, loaded.getYear());
        assertEquals(2, loaded.size());
        assertTrue(loaded.isHoliday(LocalDate.of(2026, 5, 1)));
        assertTrue(loaded.isHoliday(LocalDate.of(2026, 10, 1)));
    }

    @Test
    void loadSkipsUnparsableDateEntries() throws Exception {
        Files.writeString(configFile(),
                "holiday.year=2026\nholiday.dates=2026-05-01,not-a-date,2026-10-01\n");
        HolidayStore store = new HolidayStore(configFile());
        HolidayData data = store.load();
        assertEquals(2, data.size());
        assertEquals(2026, data.getYear());
    }

    @Test
    void reminderYearRoundTrip() {
        HolidayStore store = new HolidayStore(configFile());
        assertEquals(-1, store.getJan1RemindedYear());
        assertEquals(-1, store.getDec31RemindedYear());

        store.setJan1RemindedYear(2026);
        store.setDec31RemindedYear(2026);

        assertEquals(2026, store.getJan1RemindedYear());
        assertEquals(2026, store.getDec31RemindedYear());
    }

    @Test
    void savePreservesReminderFieldsViaMerge() {
        HolidayStore store = new HolidayStore(configFile());
        store.setJan1RemindedYear(2026);
        store.setDec31RemindedYear(2025);

        HolidayData data = new HolidayData();
        data.setYear(2026);
        data.addDate(LocalDate.of(2026, 5, 1));
        store.save(data);

        // save 会合并已有属性，不应丢失提醒年份
        assertEquals(2026, store.getJan1RemindedYear());
        assertEquals(2025, store.getDec31RemindedYear());
        assertEquals(1, store.load().size());
    }

    @Test
    void importFromExcelParsesStringAndNumericDates() throws Exception {
        Path xlsx = tempDir.resolve("holidays.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            sheet.createRow(0).createCell(0).setCellValue("2026-05-01");
            sheet.createRow(1).createCell(0).setCellValue("2026/10/1");
            Cell numeric = sheet.createRow(2).createCell(0);
            numeric.setCellValue(LocalDate.of(2026, 6, 1));
            try (OutputStream os = Files.newOutputStream(xlsx)) {
                wb.write(os);
            }
        }

        HolidayStore store = new HolidayStore(configFile());
        HolidayData data = store.importFromExcel(xlsx);

        assertEquals(3, data.size());
        assertEquals(2026, data.getYear());
        assertTrue(data.isHoliday(LocalDate.of(2026, 5, 1)));
        assertTrue(data.isHoliday(LocalDate.of(2026, 10, 1)));
        assertTrue(data.isHoliday(LocalDate.of(2026, 6, 1)));
    }

    @Test
    void importFromExcelSupportsYearlessFormats() throws Exception {
        Path xlsx = tempDir.resolve("holidays2.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sheet1");
            sheet.createRow(0).createCell(0).setCellValue("5月1日");
            try (OutputStream os = Files.newOutputStream(xlsx)) {
                wb.write(os);
            }
        }

        HolidayStore store = new HolidayStore(configFile());
        HolidayData data = store.importFromExcel(xlsx);
        assertEquals(1, data.size());
        assertEquals(LocalDate.now().getYear(), data.getYear());
        assertTrue(data.isHoliday(LocalDate.of(LocalDate.now().getYear(), 5, 1)));
    }

    @Test
    void importFromExcelEmptySheetThrows() throws Exception {
        Path xlsx = tempDir.resolve("empty.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            wb.createSheet("Sheet1");
            try (OutputStream os = Files.newOutputStream(xlsx)) {
                wb.write(os);
            }
        }

        HolidayStore store = new HolidayStore(configFile());
        assertThrows(IOException.class, () -> store.importFromExcel(xlsx));
    }

    @Test
    void importFromExcelMissingFileThrows() {
        HolidayStore store = new HolidayStore(configFile());
        assertThrows(IOException.class,
                () -> store.importFromExcel(tempDir.resolve("missing.xlsx")));
    }

    @Test
    void getConfigFile() {
        HolidayStore store = new HolidayStore(configFile());
        assertEquals(configFile(), store.getConfigFile());
    }
}
