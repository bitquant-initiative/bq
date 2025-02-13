package bq.util.ta4j;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.NaN;
import org.ta4j.core.num.Num;

public class Nums {

  private Nums() {}

  public static Optional<Double> asDouble(Num num) {
    if (num == null) {
      return Optional.empty();
    }
    return Optional.of(num.doubleValue());
  }

  public static Optional<Num> asDoubleNum(Num num) {
    if (num == null) {
      return Optional.empty();
    } else if (num instanceof DoubleNum) {
      return Optional.of(num);
    } else {
      return Optional.of(DoubleNum.valueOf(num.doubleValue()));
    }
  }

  public static Optional<Num> asDoubleNum(Object x) {
    return asNum(x);
  }

  public static Optional<Num> asNum(Object x) {
    if (x == null) {
      return Optional.empty();
    } else if (x instanceof DoubleNum) {
      // no conversion is reuired
      return Optional.of((Num) x);
    }

    Double d = null;
    if (x instanceof Num) {
      d = ((Num) x).doubleValue();

    } else if (x instanceof Double) {
      d = (Double) x;
    } else if (x instanceof BigDecimal) {
      BigDecimal bd = (BigDecimal) x;
      d = ((BigDecimal) x).doubleValue();

    } else if (x instanceof Number) {
      d = ((Number) x).doubleValue();

    } else if (x instanceof Boolean) {
      Boolean b = ((Boolean) x);
      if (b) {
        d = 1.0;
      } else {
        d = 0.0d;
      }
    } else if (x instanceof String) {
      try {
        String s = (String) x;
        s = s.trim();
        d = Double.parseDouble(s);

      } catch (RuntimeException e) {
        return Optional.empty();
      }
    } else if (x instanceof BigInteger) {
      BigInteger bi = (BigInteger) x;
      d = bi.doubleValue();
    }
    if (d == null) {
      return Optional.empty();
    }
    if (Double.isNaN(d)) {
      return Optional.of(NaN.NaN);
    }

    return Optional.of(DoubleNum.valueOf(d));
  }
}
