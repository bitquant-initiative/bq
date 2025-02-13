package bq.util.ta4j;

import bq.util.Zones;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DoubleNum;

public class ImmutableBarsTest {

  @Test
  public void testCreateDouble() {

    LocalDate dt = LocalDate.of(2024, 12, 31);

    ImmutableBar b = (ImmutableBar) ImmutableBar.create(dt, 1.1, 2.1, 0.9, null, 5223.9, 900l);

    Assertions.assertThat(b.getBeginTime()).isEqualTo(dt.atStartOfDay(Zones.UTC));
    Assertions.assertThat(b.getEndTime()).isEqualTo(dt.plusDays(1).atStartOfDay(Zones.UTC));

    Assertions.assertThat(b.getOpenPrice().toString()).isEqualTo("1.1");
    Assertions.assertThat(b.getHighPrice().toString()).isEqualTo("2.1");
    Assertions.assertThat(b.getLowPrice().toString()).isEqualTo("0.9");
    Assertions.assertThat(b.getOpenPrice().toString());
    Assertions.assertThat(b.getVolume().toString()).isEqualTo("5223.9");

    Assertions.assertThat(b.getClosePrice()).isNull();
    Assertions.assertThat(b.getId().get()).isEqualTo(900l);
  }

  @Test
  public void testSortImmutable() {

    List<Bar> bars = com.google.common.collect.Lists.newArrayList();
    for (int i = 0; i < 30; i++) {
      bars.add(
          ImmutableBar.create(
              LocalDate.of(2024, 12, 31).minusDays(i),
              new Random().nextDouble(),
              2.1,
              0.9,
              null,
              5223.9,
              900l + i));
    }

    Collections.shuffle(bars);

    Bar last = null;
    for (Bar b : bars.stream().sorted().toList()) {

      if (last != null) {
        Assertions.assertThat(b.getBeginTime()).isAfter(last.getBeginTime());
      }
      last = b;
    }
  }

  @Test
  public void verifyThatTA4JBarsAreNotComparable() {
    LocalDate d = LocalDate.of(2025, 1, 1);
    List<Bar> bars = com.google.common.collect.Lists.newArrayList();
    for (int i = 0; i < 30; i++) {

      Bar b =
          BaseBar.builder(DoubleNum.ZERO, Double.class)
              .endTime(d.plusDays(i).atStartOfDay(Zones.UTC))
              .timePeriod(Duration.ofDays(1))
              .closePrice(100.0)
              .build();
      bars.add(b);
    }

    Collections.shuffle(bars);

    try {
      Collections.sort((List) bars);
      Assertions.failBecauseExceptionWasNotThrown(ClassCastException.class);
    } catch (ClassCastException expected) {
      // ok
    }

    Bar last = null;
    for (Bar b : bars.stream().sorted(Bars.ascendingDateOrder()).toList()) {

      if (last != null) {
        Assertions.assertThat(b.getBeginTime()).isAfter(last.getBeginTime());
      }
      last = b;
    }
  }
}
