package bq.util.ta4j;

import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class CachedIndicatorTest {

  @Test
  public void testIt() {

    AtomicInteger callCount = new AtomicInteger();
    Indicator<Num> x =
        new Indicator<Num>() {

          @Override
          public Num getValue(int index) {
            callCount.incrementAndGet();
            return DoubleNum.valueOf(index);
          }

          @Override
          public int getUnstableBars() {
            return 0;
          }

          @Override
          public BarSeries getBarSeries() {
            return null;
          }
        };

    Indicator<Num> ci = CachedIndicator.of(x);

    for (int i = 0; i < 100; i++) {
      Assertions.assertThat(ci.getValue(i).intValue()).isEqualTo(i);
    }

    Assertions.assertThat(callCount.get()).isEqualTo(100);

    // Do it again, make sure that values are the same and make sure that all responses were cached
    for (int i = 0; i < 100; i++) {
      Assertions.assertThat(ci.getValue(i).intValue()).isEqualTo(i);
    }

    Assertions.assertThat(callCount.get()).isEqualTo(100);
  }
}
