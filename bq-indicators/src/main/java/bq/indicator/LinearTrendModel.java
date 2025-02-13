package bq.indicator;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class LinearTrendModel {

  static ZoneId UTC = ZoneId.of("UTC");

  BarSeries barSeries;

  Double m; // slope
  Double b; // y intercept

  // bLower is the y-intercept
  Double bLower; // y-intercept of the channel lower band (if set)
  Double bUpper; // y-intercept of the channel upper band (if set)

  private LinearTrendModel() {}

  public static LinearTrendModel from(LocalDate d0, double p0, LocalDate d1, double p1) {
    var model = new LinearTrendModel();
    model.barSeries = null;
    model.init(d0, p0, d1, p1);
    return model;
  }

  public static LinearTrendModel from(BarSeries barSeries, String t0, String t1) {
    var model = new LinearTrendModel();
    model.barSeries = barSeries;
    model.init(toLocalDate(t0), toLocalDate(t1));
    return model;
  }

  static LocalDate toLocalDate(String s) {
    return LocalDate.parse(s, DateTimeFormatter.ISO_DATE);
  }

  public static LinearTrendModel from(BarSeries barSeries, ZonedDateTime t0, ZonedDateTime t1) {
    var model = new LinearTrendModel();
    model.barSeries = barSeries;

    model.init(model.findBar(t0.toLocalDate()).get(), model.findBar(t1.toLocalDate()).get());

    return model;
  }

  public static LinearTrendModel from(BarSeries barSeries, LocalDate t0, LocalDate t1) {
    var model = new LinearTrendModel();
    model.barSeries = barSeries;

    model.init(model.findBar(t0).get(), model.findBar(t1).get());

    return model;
  }

  public static LinearTrendModel from(BarSeries barSeries, Bar b0, Bar b1) {
    LinearTrendModel m = new LinearTrendModel();
    m.barSeries = barSeries;
    m.init(b0, b1);
    return m;
  }

  Optional<Bar> findBar(LocalDate d) {
    if (d == null) {
      return Optional.empty();
    }

    return stream(this.barSeries)
        .filter(b -> !b.getBeginTime().isBefore(d.atStartOfDay(UTC)))
        .findFirst();
  }

  public Function<ZonedDateTime, Num> trendValueFunction() {
    return new Function<ZonedDateTime, Num>() {

      @Override
      public Num apply(ZonedDateTime t) {
        return DoubleNum.valueOf(getModelPrice(t.toLocalDate()));
      }
    };
  }

  Function<ZonedDateTime, Num> quantileFunction() {
    return new Function<ZonedDateTime, Num>() {

      public Num apply(ZonedDateTime t) {
        return DoubleNum.valueOf(getQuantile(t.toLocalDate()));
      }
    };
  }

  Function<ZonedDateTime, Num> quantilePriceFunction(final double q) {

    return new Function<ZonedDateTime, Num>() {

      @Override
      public Num apply(ZonedDateTime t) {
        return DoubleNum.valueOf(getPriceAtQuantile(t.toLocalDate(), q));
      }
    };
  }

  double calcTrendPrice(long epochSeconds) {
    return (m * epochSeconds) + b;
  }

  long x(ZonedDateTime d) {
    return time(d);
  }

  long time(ZonedDateTime d) {
    return d.toEpochSecond();
  }

  long x(LocalDate d) {
    return time(d);
  }

  long time(LocalDate d) {
    return d.atStartOfDay(UTC).toEpochSecond();
  }

  public double getPriceAtQuantile(LocalDate d, double quantile) {

    // GIVEN: quantile = (price - lower) / (upper-lower) * 100
    // THEN : price = (quantile / 100) * (upper - lower) + lower
    double lower = getLowerChannelPrice(d);
    double upper = getUpperChannelPrice(d);

    return ((quantile / 100d) * (upper - lower)) + lower;
  }

  public double getQuantile(LocalDate d, double p) {

    // GIVEN: quantile = (price - lower) / (upper-lower) * 100
    double lower = getLowerChannelPrice(d);
    double upper = getUpperChannelPrice(d);

    double q = ((p - lower) / (upper - lower)) * 100d;

    return Math.round(q);
  }

  public double getQuantile(LocalDate d) {
    return getQuantile(d, getActualPrice(d));
  }

  // Solve for the date at which the model will be the given price.
  public LocalDate getModelDate(double price) {

    // y = mx + b
    // price = m * time +b
    // time = (price-b) / m

    double t = (price - b) / m;

    long epochSecs = Math.round(t);

    ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecs), UTC);

    return zdt.toLocalDate();
  }

  public double getUpperChannelPrice(LocalDate d) {
    Preconditions.checkState(bUpper != null, "channel not initialized");
    // Note that we cannot use quantile for upp or lower since
    // upper and lower y-intercept values are used to compute the quantiles
    return m * time(d) + bUpper;
  }

  public double getMiddleChannelPrice(LocalDate d) {

    return getPriceAtQuantile(d, 50d);
  }

  public double getLowerChannelPrice(LocalDate d) {
    Preconditions.checkState(bLower != null, "channel not initialized");
    // Note that we cannot use quantile for upp or lower since
    // upper and lower y-intercept values are used to compute the quantiles
    return m * time(d) + bLower;
  }

  public double getActualPrice(LocalDate d) {

    return getBar(d).getClosePrice().doubleValue();
  }

  public double getModelPrice(LocalDate d) {
    return m * time(d) + b;
  }

  private LinearTrendModel init(LocalDate d0, LocalDate d1) {
    return init(getBar(d0), getBar(d1));
  }

  Function<Bar, Double> barPriceFunction =
      (Bar bar) -> {
        // can be changed
        return bar.getClosePrice().doubleValue();
      };

  public LinearTrendModel setBarPriceFunction(Function<Bar, Double> f) {
    this.barPriceFunction = f;
    return this;
  }

  double getBarPrice(Bar b) {
    Preconditions.checkNotNull(barPriceFunction != null, "barPriceFunction not set");
    return barPriceFunction.apply(b);
  }

  private LinearTrendModel init(Bar b0, Bar b1) {
    return init(
        b0.getBeginTime().toLocalDate(),
        getBarPrice(b0),
        b1.getBeginTime().toLocalDate(),
        getBarPrice(b1));
  }

  private Bar getBar(LocalDate d) {

    Optional<Bar> bar = findBar(d);
    Preconditions.checkState(bar.isPresent(), "could not find bar for %s", d);
    return bar.get();
  }

  public LinearTrendModel channel(LocalDate d) {
    return channel(getBar(d));
  }

  public LinearTrendModel channel(Bar b) {
    return channel(b.getBeginTime().toLocalDate(), b.getClosePrice().doubleValue());
  }

  public LinearTrendModel channel(LocalDate d, double p) {

    Preconditions.checkState(m != null, "trend not initialized");
    Preconditions.checkState(b != null, "trend not initialized");

    Preconditions.checkState(bUpper == null, "channel already initialized");

    Preconditions.checkState(bLower == null, "channel already initialized");
    // we are looking to compute a new y intercept using a point on the opposite
    // side of the channel
    // y = mx + b
    // b = y - mx

    double bChannel = p - (m * x(d));

    if (bChannel > this.b) {
      bUpper = bChannel;
      bLower = b;
    } else {
      bUpper = b;
      bLower = bChannel;
    }

    return this;
  }

  private LinearTrendModel init(double x0, double y0, double x1, double y1) {
    double dx = x1 - x0;
    double dy = y1 - y0;

    this.m = (dy / dx);

    // y = mx + b
    // b = y - mx

    this.b = y0 - (m * x0);

    bLower = null;
    bUpper = null;

    return this;
  }

  private LinearTrendModel init(LocalDate d0, double p0, LocalDate d1, double p1) {

    return init(x(d0), p0, x(d1), p1);
  }

  static Stream<Bar> stream(BarSeries bs) {
    if (bs == null) {
      return Stream.empty();
    }
    List<Bar> tmp = Lists.newArrayList();
    for (int i = bs.getBeginIndex(); i <= bs.getEndIndex(); i++) {
      tmp.add(bs.getBar(i));
    }
    return tmp.stream();
  }

  public String toString() {

    return MoreObjects.toStringHelper(this)
        .add("m", new BigDecimal(m).toPlainString())
        .add("b", new BigDecimal(b).toPlainString())
        .toString();
  }
}
