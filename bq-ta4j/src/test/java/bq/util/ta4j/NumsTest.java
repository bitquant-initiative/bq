package bq.util.ta4j;

import java.math.BigDecimal;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.NaN;

public class NumsTest {

  @Test
  public void testDoubleNaN() {
    double a = Double.NaN;
    double b = 1;

    double divbyzero = 1.0 / 0.0;

    Assertions.assertThat(divbyzero).isInfinite();

    Assertions.assertThat(a + b).isNaN();

    Assertions.assertThat(b + divbyzero).isInfinite();
    Assertions.assertThat(Double.isFinite(a)).isFalse();
    Assertions.assertThat(Double.isFinite(divbyzero)).isFalse();

    Assertions.assertThat(DoubleNum.valueOf(Double.NaN).doubleValue()).isNaN();

    Assertions.assertThat(Double.isInfinite(DoubleNum.valueOf(1.0 / 0.0d).doubleValue())).isTrue();
  }

  @Test
  public void testIt() {

    Assertions.assertThat(Nums.asDoubleNum(null).isEmpty()).isTrue();
    Assertions.assertThat(Nums.asDoubleNum(2.3d).get().doubleValue()).isEqualByComparingTo(2.3d);
    Assertions.assertThat(Nums.asDoubleNum(new BigDecimal(2.3d)).get().doubleValue())
        .isEqualByComparingTo(2.3d);

    Assertions.assertThat(Nums.asDoubleNum(123l).get().longValue()).isEqualByComparingTo(123l);
    Assertions.assertThat(Nums.asDoubleNum("123.456").get().doubleValue())
        .isEqualByComparingTo(123.456d);
    Assertions.assertThat(Nums.asDoubleNum("  123.456  ").get().doubleValue())
        .isEqualByComparingTo(123.456d);

    Assertions.assertThat(Nums.asDoubleNum(true).get().doubleValue()).isEqualByComparingTo(1.0d);
    Assertions.assertThat(Nums.asDoubleNum(false).get().doubleValue()).isEqualByComparingTo(0.0d);

    Assertions.assertThat(NaN.NaN.doubleValue()).isEqualByComparingTo(Double.NaN);

    Assertions.assertThat(Nums.asDoubleNum(Double.NaN).get()).isInstanceOf(NaN.class);
    Assertions.assertThat(Nums.asDoubleNum(Double.NaN).get().doubleValue())
        .isEqualByComparingTo(Double.NaN);
  }
}
