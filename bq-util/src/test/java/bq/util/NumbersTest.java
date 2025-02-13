package bq.util;

import com.google.common.collect.Lists;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class NumbersTest {

  @Test
  public void testIt() {

    List<Number> list = Lists.newArrayList();

    Random r = new Random();

    for (int i = 0; i < 100000; i++) {
      list.add(r.nextDouble(-1000.0, 6000.0));
      list.add(r.nextFloat());
      list.add(null);

      list.add(r.nextLong(5000));
      list.add(r.nextInt(5000));
      list.add(Double.POSITIVE_INFINITY);
      list.add(Double.NEGATIVE_INFINITY);
      list.add(Double.NaN);
      list.add(r.nextInt());
      list.add(r.nextLong());

      // BigDecimal will sort correctly, but only if values are in range
      list.add(new BigDecimal(r.nextDouble()).setScale(10, RoundingMode.HALF_UP));
      list.add(new BigDecimal(r.nextDouble() * 1000000d).setScale(9, RoundingMode.HALF_UP));
    }

    Collections.sort(list, Numbers::numberComparator);
    Double lastNum = null;
    for (Number n : list) {

      if (lastNum != null) {

        Assertions.assertThat(n).isNotNull();
        Double thisNum = Double.valueOf(n.toString());

        Assertions.assertThat(lastNum.compareTo(thisNum))
            .withFailMessage(
                "expecting %s <= %s but compare was %s",
                lastNum, thisNum, lastNum.compareTo(thisNum))
            .isLessThanOrEqualTo(0);

        lastNum = thisNum;
      }
      if (n == null) {
        lastNum = null;
      } else {

        lastNum = Double.parseDouble(n.toString());
      }
    }
  }

  @Test
  public void testDoubleComparatorCannotHandleNull() {

    // Double comparator cannot handle null

    try {
      Lists.newArrayList(1.0, null, 2.0).stream().sorted(Double::compare).toList();
      Assertions.failBecauseExceptionWasNotThrown(NullPointerException.class);
    } catch (NullPointerException e) {

      //  java.util.TimSort doesn't like null
    }
  }

  @Test
  public void testDoubleComparator() {

    for (int i = 0; i < 1000; i++) {
      List<Double> vals =
          Lists.newArrayList(
              1.0, Double.NEGATIVE_INFINITY, -1.0, 0d, Double.NaN, Double.POSITIVE_INFINITY);

      Collections.shuffle(vals);

      Assertions.assertThat(vals.stream().sorted().map(n -> n.toString()).toList())
          .containsExactly("-Infinity", "-1.0", "0.0", "1.0", "Infinity", "NaN");
    }
  }

  @Test
  public void testNumbersComparator() {

    // note that our sort can deal wtih null
    for (int i = 0; i < 1000; i++) {
      List<Double> vals =
          Lists.newArrayList(
              null, 1.0, Double.NEGATIVE_INFINITY, -1.0, 0d, Double.NaN, Double.POSITIVE_INFINITY);

      Collections.shuffle(vals);

      Assertions.assertThat(
              vals.stream()
                  .sorted(Numbers::numberComparator)
                  .map(n -> n != null ? n.toString() : "null")
                  .toList())
          .containsExactly("null", "-Infinity", "-1.0", "0.0", "1.0", "Infinity", "NaN");
    }
  }

  @Test
  public void testX() {

    Assertions.assertThat(Double.compare(Double.NaN, 1)).isEqualTo(1);
    Assertions.assertThat(Double.compare(Double.POSITIVE_INFINITY, 1)).isEqualTo(1);
    Assertions.assertThat(Double.compare(Double.NEGATIVE_INFINITY, 1)).isEqualTo(-1);
    Assertions.assertThat(Double.compare(Double.NEGATIVE_INFINITY, Double.MIN_VALUE)).isEqualTo(-1);
    Assertions.assertThat(Double.compare(Double.NaN, Double.POSITIVE_INFINITY)).isEqualTo(1);
    Assertions.assertThat(Double.compare(Double.POSITIVE_INFINITY, Double.NaN)).isEqualTo(-1);
  }
}
