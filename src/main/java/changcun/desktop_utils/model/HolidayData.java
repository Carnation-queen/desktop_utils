package changcun.desktop_utils.model;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * 某一年份的节假日数据。
 */
public class HolidayData {

    private int year = LocalDate.now().getYear();
    private final SortedSet<LocalDate> dates = new TreeSet<>();

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public SortedSet<LocalDate> getDates() {
        return Collections.unmodifiableSortedSet(new TreeSet<>(dates));
    }

    public void addDate(LocalDate date) {
        if (date != null) {
            dates.add(date);
        }
    }

    public void addAll(Collection<LocalDate> collection) {
        for (LocalDate d : collection) {
            addDate(d);
        }
    }

    public int size() {
        return dates.size();
    }

    /** 判断某一天是否为节假日。 */
    public boolean isHoliday(LocalDate date) {
        return date != null && dates.contains(date);
    }
}
