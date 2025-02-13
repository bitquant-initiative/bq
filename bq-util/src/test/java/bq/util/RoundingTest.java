package bq.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class RoundingTest {

  @Test
  public void testXX() {

    Assertions.assertThat(Rounding.format(12, 0)).isEqualTo("12");
    Assertions.assertThat(Rounding.format(12, 2)).isEqualTo("12.00");

    Assertions.assertThat(Rounding.format(12.365, 2)).isEqualTo("12.37");

    Assertions.assertThat(Rounding.format(12.365, 5)).isEqualTo("12.36500");
  }

  @Test
  public void testX() {
    Assertions.assertThat(Rounding.round(1.23456d, 4)).isEqualByComparingTo(1.2346);
    Assertions.assertThat(Rounding.round(1.23456d, 0)).isEqualByComparingTo(1.);
  }

  @Test
  public void testNaN() {
    Assertions.assertThat(Rounding.round(Double.NaN, 2)).isNaN();
  }

  @Test
  public void testInfinty() {
    Assertions.assertThat(Rounding.round(Double.POSITIVE_INFINITY, 2)).isInfinite();
    Assertions.assertThat(Rounding.round(Double.NEGATIVE_INFINITY, 2)).isInfinite();
  }

  @Test
  @Disabled
  public void testHeadScratcher() {

    // Why does this fail?
    Assertions.assertThat(Rounding.round(1.2345, 3)).isEqualTo(1.235);
  }
}
