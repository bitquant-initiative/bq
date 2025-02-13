package bq.util;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Numbers {

  public static int numberComparator(Number a, Number b) {
    if (a == null) {

      // not that Collection sorting will always put nulls first regardless of what
      // return here.
      if (b == null) {
        // both are null
        return 0;
      } else {
        return -1;
      }
    } else {
      if (b == null) {
        return 1;
      }
    }

    // comparing unlike types is a minefield of problems.
    if (a instanceof Long && b instanceof Long) {
      return Long.compare((long) a, (long) b);
    } else if (a instanceof Integer && b instanceof Integer) {
      return Long.compare((int) a, (int) b);
    } else if (a instanceof Double && b instanceof Double) {
      return Double.compare((double) a.doubleValue(), (double) b.doubleValue());
    } else if (a instanceof BigDecimal && b instanceof BigDecimal) {
      BigDecimal ad = (BigDecimal) a;
      BigDecimal bd = (BigDecimal) b;
      return ad.compareTo(bd);
    } else if (a instanceof BigInteger && b instanceof BigInteger) {
      BigInteger ad = (BigInteger) a;
      BigInteger bd = (BigInteger) b;
      return ad.compareTo(bd);
    }

    // This is some bizarre stuff. With high scale BigDecimal values, re-parsing the
    // double value is required.
    //
    // While it is tempting to turn both values into a BigDecimal and use its compare()
    // function, that is very SLOW and ends up choking on NaN and Infinity values.
    //
    // This method is both faster *and* works!

    boolean bigDecimalWorkaround = (a instanceof BigDecimal || b instanceof BigDecimal);
    if (bigDecimalWorkaround) {
      Double da = Double.valueOf(a.toString());
      Double db = Double.valueOf(b.toString());
      return Double.compare(da, db);
    } else {
      return Double.compare(a.doubleValue(), b.doubleValue());
    }
  }
}
