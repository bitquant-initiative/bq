package bq.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class MarketCalendarTest {

  @Test
  public void timeToUpdateCalendar() {

    // force a test failure when we no longer have a year of market holidays
    int expectedHolidayCount = 10;
    for (int y = LocalDate.now().getYear(); y <= LocalDate.now().getYear() + 1; y++) {
      int year = y;

      Assertions.assertThat(
              MarketCalendar.getHolidays().stream().filter(d -> d.getYear() == year).count())
          .withFailMessage(
              "time to update holidays for %d - https://www.nyse.com/markets/hours-calendars", year)
          .isEqualTo(expectedHolidayCount);
    }
  }

  @Test
  public void testHolidayAndTradingDay() {

    java.time.LocalDate d = java.time.LocalDate.now();
    for (int i = 0; i < 365; i++) {
      if (d.getDayOfWeek() == DayOfWeek.SATURDAY) {
        Assertions.assertThat(MarketCalendar.isTradingDay(d)).isFalse();
      } else if (d.getDayOfWeek() == DayOfWeek.SUNDAY) {
        Assertions.assertThat(MarketCalendar.isTradingDay(d)).isFalse();
      }

      if (MarketCalendar.isHoliday(d)) {
        Assertions.assertThat(MarketCalendar.isTradingDay(d)).isFalse();
      }

      d = d.plus(1, ChronoUnit.DAYS);
    }
  }
}
