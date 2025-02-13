package bq.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Rounding {

  public static String format(double d, int decimalPlaces) {
    String format = String.format("%%.%sf", decimalPlaces);
    return String.format(format, d);
  }

  public static double round(double d, int decimalPlaces) {

    if (Double.isNaN(d) || Double.isInfinite(d)) {
      return d;
    }
    return new BigDecimal(d).setScale(decimalPlaces, RoundingMode.HALF_UP).doubleValue();
  }
}
