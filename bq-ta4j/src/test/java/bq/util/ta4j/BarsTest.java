package bq.util.ta4j;

import com.google.common.collect.Lists;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

public class BarsTest {

  @Test
  public void testX() {
    List<Bar> bars = Lists.newArrayList();

    for (int i = 0; i < 100; i++) {
      double v = i + 0.5;
      bars.add(ImmutableBar.create(LocalDate.of(2024, 10, 30).plusDays(i), v, v, v, v, v));
    }

    BaseBarSeries bs = new BaseBarSeriesBuilder().withBars(bars).build();

    List<Bar> bars2 = Bars.toStream(bs).filter(Bars.notBefore(LocalDate.of(2024, 11, 1))).toList();

    Assertions.assertThat(bars2).hasSize(98);
  }

  @Test
  public void testPredicates() {
    List<Bar> bars = Lists.newArrayList();

    for (int i = 0; i < 100; i++) {
      double v = i + 0.5;
      bars.add(
          ImmutableBar.create(LocalDate.of(2024, 10, 30).plusDays(i), v, v, v, v, v, (long) i));
    }
    BarSeries bs = ImmutableBarSeries.of(bars);

    Assertions.assertThat(Bars.findBar(bs, LocalDate.of(2024, 10, 31)).get().toString())
        .contains("2024-10-31");
  }

  boolean isWeekend(Bar b) {
    DayOfWeek dow = b.getBeginTime().toLocalDate().getDayOfWeek();
    if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
      return true;
    }
    return false;
  }

  @Test
  public void testDifference() {

    List<Bar> bars = Lists.newArrayList();
    for (int i = 0; i < 100; i++) {
      double v = i + 0.5;
      bars.add(
          ImmutableBar.create(LocalDate.of(2024, 10, 30).plusDays(i), v, v, v, v, v, (long) i));
    }

    List<Bar> weekendBars = bars.stream().filter(p -> isWeekend(p)).toList();

    Assertions.assertThat(Bars.difference(bars, weekendBars))
        .doesNotContainAnyElementsOf(weekendBars);
  }

  @Test
  public void testComparator() {
    List<Bar> bars = Lists.newArrayList();
    for (int i = 0; i < 100; i++) {
      double v = i + 0.5;
      bars.add(
          ImmutableBar.create(LocalDate.of(2024, 10, 30).minusDays(i), v, v, v, v, v, (long) i));
    }

    Collections.shuffle(bars);

    Collections.sort(bars, Bars.ascendingDateOrder());

    Bar lastBar = null;
    for (Bar b : bars) {

      if (lastBar != null) {
        Assertions.assertThat(b.getBeginTime()).isAfter(lastBar.getBeginTime());
      }

      lastBar = b;
    }
  }
}
