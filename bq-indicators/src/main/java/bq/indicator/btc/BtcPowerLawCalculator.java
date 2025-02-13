package bq.indicator.btc;

import bq.indicator.btc.BtcPowerLawModel.QuantileModel;
import bq.util.BqException;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.flogger.FluentLogger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

public class BtcPowerLawCalculator {

  static FluentLogger logger = FluentLogger.forEnclosingClass();

  // THe algorithm to find c-values for a given set of quantiles is not sophisticaled.
  // It starts by iterating throught a range of c-values by adding
  // INITIAL_C_INCREMENT
  // If all quantiles are filled, the algorithm is complete. If there are missing
  // items.
  // If there are gaps, the c-step increment is multiplied by C_STEP_MULTIPLE and
  // run again.
  private static final double INITIAL_C_INCREMENT = 0.08d;

  private static final double C_STEP_MULTIPLE = 0.2d;

  static LocalDate calculateDate(double price, double a, double c) {

    long daysSinceGenesis = Math.round(Math.pow(10, (Math.log10(price) + Math.abs(c)) / a));

    return BtcUtil.GENESIS_DATE.plusDays(daysSinceGenesis);
  }

  static double calculatePrice(LocalDate d, double a, double c) {
    return calculatePrice(BtcUtil.getDaysSinceGenesis(d), a, c);
  }

  static double calculatePrice(int daysSinceGenesis, double a, double c) {

    return Math.round(Math.pow(10, (Math.log10(daysSinceGenesis) * a) - c));
  }

  static int calculateQuantile(BarSeries data, double a, double c) {

    int totalCount = 0;
    int belowModelCount = 0;
    for (Bar b : data.getBarData()) {

      LocalDate d = b.getBeginTime().toLocalDate();

      double modelValue = calculatePrice(BtcUtil.getDaysSinceGenesis(d), a, c);

      if (b.getClosePrice() != null) {
        totalCount++;
        if (b.getClosePrice().doubleValue() < modelValue) {
          belowModelCount++;
        }
      }
    }

    int q = -1;

    if (totalCount == 0) {
      q = 99;
    } else if (belowModelCount == 0) {
      return q = 0;
    } else {

      double percentBelowModel = (belowModelCount / (double) totalCount) * 100d;

      BigDecimal quantile = new BigDecimal(percentBelowModel).setScale(0, RoundingMode.HALF_EVEN);

      q = quantile.intValue();
    }
    Preconditions.checkState(q >= 0);
    // logger.atInfo().log("a=%s c=%s ==> q=%s
    // belowCount=%s",a,c,q,belowModelCount);
    return q;
  }

  /**
   * For a given data set and coefficient a, find a value c that is a quantile
   * between 1 and 99. Once we have a starting point we will fill in the other
   * values.
   *
   * @param data
   * @param a
   * @return
   */
  private static double findStartingCValue(BarSeries data, double a) {
    for (double c = 0; c < 30; c += .2) {

      double q = BtcPowerLawCalculator.calculateQuantile(data, a, c);

      if (q > 10 && q < 90) {

        return c;
      }
    }
    throw new IllegalStateException("could not find starting c value");
  }

  private static void calibrateQuantile(
      BarSeries data, double a, QuantileModel model, double step) {

    logger.atFiner().log("calibrate a=%s step=%s", a, step);

    Preconditions.checkArgument(model.quantiles.size() == 100);
    Preconditions.checkArgument(a >= 2 && a < 10, "a must be in range [2,10] was %s", a);
    Preconditions.checkArgument(step > 0, "step must be >0");
    if (step < -0.0001) {
      throw new BqException("could not calibrate quantile model for a=" + a);
    }
    Preconditions.checkArgument(step > 0.0001);
    Double cx = null;

    // If the first quantile is set, use that as a starting point
    if (model.quantiles.get(0) != null) {
      cx = model.quantiles.get(0);
    } else {
      // otherwise find a c-value that generates a quantile between 10 and 90
      // really we just don't want q=0 or q=99
      cx = BtcPowerLawCalculator.findStartingCValue(data, a);
    }

    Preconditions.checkState(!Double.isNaN(cx));

    final Double c0 = cx;
    cx = null; // c0 should be used from here forward...set cx to null to force an error if used
    double c = c0;

    int q = calculateQuantile(data, a, c);
    Preconditions.checkState(q >= 0);
    Preconditions.checkState(q < 99);

    logger.atFiner().log("starting quantile a=%s c=%s q=%s", a, c, q);

    model.quantiles.set(q, new BigDecimal(c).setScale(3, RoundingMode.HALF_UP).doubleValue());

    // first we're going to work our way UP the quantiles, and DOWN the c values
    while (c > 0 && q < 100) {
      c = c - Math.abs(step);
      q = calculateQuantile(data, a, c);

      if (q < 100) {
        if (model.quantiles.get(q) == null) {
          double qv = new BigDecimal(c).setScale(3, RoundingMode.HALF_UP).doubleValue();
          logger.atFiner().log("q[%s]=%s", q, qv);
          model.quantiles.set(q, qv);
        }
      }
    }
    // now go back to our starting point and work down the quantiles and UP the
    // c-value
    c = c0;
    while (c < 50 && q > 0) {
      c = c + Math.abs(step);
      q = BtcPowerLawCalculator.calculateQuantile(data, a, c);

      if (model.quantiles.get(q) == null) {

        double qv = new BigDecimal(c).setScale(3, RoundingMode.HALF_UP).doubleValue();
        logger.atFiner().log("q[%s]=%s", q, qv);
        model.quantiles.set(q, qv);
      }
    }

    for (int i = 0; i < model.quantiles.size(); i++) {
      if (model.quantiles.get(i) == null) {
        step = step * C_STEP_MULTIPLE;
        calibrateQuantile(data, a, model, step);
        return;
      }
    }
    for (int i = 0; i < model.quantiles.size(); i++) {
      if (model.quantiles.get(i) == null) {
        throw new IllegalStateException("could not calibtrate");
      }
    }
  }

  public static QuantileModel generateQuantileModel(BarSeries data, double a) {

    Stopwatch sw = Stopwatch.createStarted();
    QuantileModel m = new QuantileModel(a);
    Preconditions.checkState(m.quantiles.size() == 100);
    logger.atFiner().log("calibrating with %s bars", data.getBarCount());

    calibrateQuantile(data, a, m, BtcPowerLawCalculator.INITIAL_C_INCREMENT);

    logger.atInfo().log(
        "quantiles generated: duration=%sms bars=%s first=%s last=%s",
        sw.elapsed(TimeUnit.MILLISECONDS),
        data.getBarCount(),
        data.getFirstBar().getBeginTime().toLocalDate(),
        data.getLastBar().getBeginTime().toLocalDate());
    return m;
  }
}
