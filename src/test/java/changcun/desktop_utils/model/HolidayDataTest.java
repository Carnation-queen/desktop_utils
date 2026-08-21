package changcun.desktop_utils.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HolidayDataTest {

    @Test
    void defaultYearIsCurrentYear() {
        HolidayData data = new HolidayData();
        assertEquals(LocalDate.now().getYear(), data.getYear());
    }

    @Test
    void addDateIgnoresNull() {
        HolidayData data = new HolidayData();
        data.addDate(null);
        assertEquals(0, data.size());
    }

    @Test
    void addDateDeduplicates() {
        HolidayData data = new HolidayData();
        LocalDate d = LocalDate.of(2026, 5, 1);
        data.addDate(d);
        data.addDate(d);
        assertEquals(1, data.size());
    }

    @Test
    void addAllAddsEachDate() {
        HolidayData data = new HolidayData();
        data.addAll(java.util.Arrays.asList(
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 10, 1),
                null));
        assertEquals(2, data.size());
    }

    @Test
    void isHoliday() {
        HolidayData data = new HolidayData();
        LocalDate d = LocalDate.of(2026, 5, 1);
        data.addDate(d);
        assertTrue(data.isHoliday(d));
        assertFalse(data.isHoliday(LocalDate.of(2026, 5, 2)));
        assertFalse(data.isHoliday(null));
    }

    @Test
    void getDatesReturnsSortedCopy() {
        HolidayData data = new HolidayData();
        data.addDate(LocalDate.of(2026, 10, 1));
        data.addDate(LocalDate.of(2026, 5, 1));
        var dates = data.getDates();
        assertEquals(LocalDate.of(2026, 5, 1), dates.first());
        assertEquals(LocalDate.of(2026, 10, 1), dates.last());
    }

    @Test
    void getDatesIsUnmodifiable() {
        HolidayData data = new HolidayData();
        data.addDate(LocalDate.of(2026, 5, 1));
        assertThrows(UnsupportedOperationException.class,
                () -> data.getDates().add(LocalDate.of(2026, 6, 1)));
    }

    @Test
    void setYear() {
        HolidayData data = new HolidayData();
        data.setYear(2030);
        assertEquals(2030, data.getYear());
    }
}
