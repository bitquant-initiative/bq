package bq.indicator;

import bq.util.Zones;
import com.google.common.flogger.FluentLogger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

/**
 * Look at the trailing N days/years and compute an annualized rate of return.
 *
 * May alos provide an alternate implementation to computes the
 * annualized rate of return based on a DCA strategy over the period.
 */
public class ARRIndicator extends AbstractIndicator<Num> {

  static FluentLogger logger = FluentLogger.forEnclosingClass();
  double years = 1;

  public ARRIndicator(BarSeries series) {
    this(series, 1);
  }

  public ARRIndicator(BarSeries series, double years) {
    super(series);
    this.years = years;
  }

  Optional<Bar> findFirstBarOnOrBefore(ZonedDateTime d) {
    Bar last = null;
    if (getBarSeries().getBarData() == null) {
      return Optional.empty();
    }
    for (Bar b : getBarSeries().getBarData()) {
      if (b.getBeginTime().isAfter(d)) {

        return Optional.ofNullable(last);
      }
      if (b.getBeginTime().toEpochSecond() == d.toEpochSecond()) {
        return Optional.of(b);
      }
      last = b;
    }
    return Optional.empty();
  }

  Optional<Bar> findFirstBarAfter(ZonedDateTime d) {

    List<Bar> bars = getBarSeries().getBarData();
    if (bars == null || bars.isEmpty()) {
      return Optional.empty();
    }
    for (Bar b : getBarSeries().getBarData()) {
      if (b.getBeginTime().isAfter(d)) {
        return Optional.of(b);
      }
    }
    return Optional.empty();
  }

  @Override
  public Num getValue(int index) {

    Bar b1 = getBarSeries().getBar(index);
    ZonedDateTime t1 = b1.getBeginTime();

    ZonedDateTime t0 = t1.minusDays((int) (years * 365));

    Optional<Bar> b0 = findFirstBarOnOrBefore(t0);
    if (b0.isEmpty()) {
      return null;
    }

    double arr = calculateARR(b0.get(), b1);

    return DoubleNum.valueOf(arr);
  }

  @Override
  public int getUnstableBars() {
    return 0;
  }

  public double calculateARR(Bar b0, Bar b1) {

    return calculateARR(
        b0.getBeginTime(),
        b0.getClosePrice().doubleValue(),
        b1.getBeginTime(),
        b1.getClosePrice().doubleValue());
  }

  public static double calculateARR(LocalDate t0, double p0, LocalDate t1, double p1) {
    return calculateARR(t0.atStartOfDay(Zones.UTC), p0, t1.atStartOfDay(Zones.UTC), p1);
  }

  public static double calculateARR(ZonedDateTime t0, double p0, ZonedDateTime t1, double p1) {

    long secondsBetween = t1.toEpochSecond() - t0.toEpochSecond();

    double years = secondsBetween / (double) (60 * 60 * 24 * 365);

    double arr = Math.pow((p1 / p0), 1 / years);

    if (arr > 0) {
      arr = (arr - 1) * 100;
    } else {

      arr = (1 - arr) * 100;
    }

    return new BigDecimal(arr).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }
}
