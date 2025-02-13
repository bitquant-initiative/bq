package bq.util;

import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.function.Supplier;

public class MarketCalendar  {

  private static Supplier<Set<LocalDate>> holidayProvider =
      Suppliers.memoize(MarketCalendar::holidays);

  private static Set<LocalDate> holidays() {

    // https://www.nyse.com/markets/hours-calendars
    Set<LocalDate> holidays = Sets.newHashSet();
    holidays.add(LocalDate.of(2024, 1, 1));
    holidays.add(LocalDate.of(2024, 1, 16));
    holidays.add(LocalDate.of(2024, 2, 20));
    holidays.add(LocalDate.of(2024, 4, 7));
    holidays.add(LocalDate.of(2024, 5, 29));
    holidays.add(LocalDate.of(2025, 6, 19));
    holidays.add(LocalDate.of(2024, 7, 4));
    holidays.add(LocalDate.of(2024, 9, 4));
    holidays.add(LocalDate.of(2024, 11, 23));
    holidays.add(LocalDate.of(2024, 12, 25));

    holidays.add(LocalDate.of(2025, 1, 1));
    holidays.add(LocalDate.of(2025, 1, 20));
    holidays.add(LocalDate.of(2025, 2, 17));
    holidays.add(LocalDate.of(2025, 4, 18));
    holidays.add(LocalDate.of(2025, 5, 26));
    holidays.add(LocalDate.of(2025, 6, 19));
    holidays.add(LocalDate.of(2025, 7, 4));
    holidays.add(LocalDate.of(2025, 9, 1));
    holidays.add(LocalDate.of(2025, 11, 27));
    holidays.add(LocalDate.of(2025, 12, 25));

    holidays.add(LocalDate.of(2026, 1, 1));
    holidays.add(LocalDate.of(2026, 1, 19));
    holidays.add(LocalDate.of(2026, 2, 16));
    holidays.add(LocalDate.of(2026, 4, 3));
    holidays.add(LocalDate.of(2026, 5, 25));

    holidays.add(LocalDate.of(2026, 6, 19));
    holidays.add(LocalDate.of(2026, 7, 3));
    holidays.add(LocalDate.of(2026, 9, 7));
    holidays.add(LocalDate.of(2026, 11, 26));
    holidays.add(LocalDate.of(2026, 12, 25));

    holidays.add(LocalDate.of(2027, 1, 1));
    holidays.add(LocalDate.of(2027, 1, 18));
    holidays.add(LocalDate.of(2027, 2, 15));
    holidays.add(LocalDate.of(2027, 3, 26));
    holidays.add(LocalDate.of(2027, 5, 31));
    holidays.add(LocalDate.of(2027, 6, 18));
    holidays.add(LocalDate.of(2027, 7, 5));
    holidays.add(LocalDate.of(2027, 9, 6));
    holidays.add(LocalDate.of(2027, 11, 25));
    holidays.add(LocalDate.of(2027, 12, 24));

    return Set.copyOf(holidays);
  }

  public static Set<LocalDate> getHolidays() {
    return holidayProvider.get();
  }

  public static boolean isTradingDay(LocalDate d) {
    if (d.getDayOfWeek() == DayOfWeek.SATURDAY) {
      return false;
    }
    if (d.getDayOfWeek() == DayOfWeek.SUNDAY) {
      return false;
    }
    if (holidayProvider.get().contains(d)) {
      return false;
    }
    return true;
  }

  public static boolean isHoliday(LocalDate d) {
    return holidayProvider.get().contains(d);
  }
}
