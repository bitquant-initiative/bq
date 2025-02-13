package bq.util.ta4j;

import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

public class ImmutableBarSeriesTest {

  @Test
  public void testIt() {

    List<Bar> bars = Lists.newArrayList();

    for (int i = 0; i < 100; i++) {
      double v = i + 0.5;
      bars.add(ImmutableBar.create(LocalDate.of(2024, 10, 30).plusDays(i), v, v, v, v, v));
    }

    BaseBarSeries bs = new BaseBarSeriesBuilder().withBars(bars).build();

    BarSeries bs2 = ImmutableBarSeries.copyOf(bs);

    Assertions.assertThat(bs2.getBarCount()).isEqualByComparingTo(bs.getBarCount());
    // beginIndex, endIndex and barData are not comparable.
    // The bizarre implementation of BaseBarSeries is what we're working around!

    try {
      bs2.getBarData().removeLast();
      Assertions.failBecauseExceptionWasNotThrown(UnsupportedOperationException.class);
    } catch (UnsupportedOperationException ignore) {

    }
  }
}
