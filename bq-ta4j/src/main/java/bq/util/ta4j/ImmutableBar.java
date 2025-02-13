package bq.util.ta4j;

import bq.util.Zones;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import org.ta4j.core.Bar;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class ImmutableBar implements Bar, Comparable<Bar> {

  protected Duration timePeriod;
  protected ZonedDateTime beginTime;
  protected ZonedDateTime endTime;

  protected Long id;

  protected Num open;
  protected Num high;
  protected Num low;
  protected Num close;
  protected Num volume;

  protected ImmutableBar() {
    super();
  }

  @Override
  public Duration getTimePeriod() {
    return timePeriod;
  }

  public LocalDate getDate() {
    return beginTime.toLocalDate();
  }

  @Override
  public ZonedDateTime getBeginTime() {
    return beginTime;
  }

  @Override
  public ZonedDateTime getEndTime() {
    return endTime;
  }

  @Override
  public Num getOpenPrice() {
    return this.open;
  }

  @Override
  public Num getHighPrice() {
    return this.high;
  }

  @Override
  public Num getLowPrice() {
    return this.low;
  }

  @Override
  public Num getClosePrice() {
    return this.close;
  }

  @Override
  public Num getVolume() {
    return this.volume;
  }

  @Override
  public Num getAmount() {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getTrades() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addTrade(Num tradeVolume, Num tradePrice) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addPrice(Num price) {
    throw new UnsupportedOperationException();
  }

  public java.util.Optional<Long> getId() {
    return java.util.Optional.of(id);
  }

  public static Bar copyOf(Bar b) {
    Preconditions.checkNotNull(b);
    if (b instanceof ImmutableBar) {
      ImmutableBar src = (ImmutableBar) b;
      ImmutableBar copy = new ImmutableBar();
      copy.beginTime = src.beginTime;
      copy.endTime = src.endTime;
      copy.timePeriod = src.timePeriod;
      copy.open = src.open;
      copy.high = src.high;
      copy.low = src.low;
      copy.close = src.close;
      copy.volume = src.volume;
      return copy;
    }

    Bar src = (Bar) b;

    ImmutableBar copy = new ImmutableBar();
    copy.beginTime = src.getBeginTime();
    copy.endTime = src.getEndTime();
    copy.timePeriod = src.getTimePeriod();
    copy.beginTime = src.getBeginTime();
    copy.endTime = src.getEndTime();
    copy.timePeriod = src.getTimePeriod();
    copy.open = src.getOpenPrice();
    copy.high = src.getHighPrice();
    copy.low = src.getLowPrice();
    copy.close = src.getClosePrice();
    copy.volume = src.getVolume();
    return copy;
  }

  public static Bar create(LocalDate d, Num open, Num high, Num low, Num close, Num volume) {

    return create(
        d,
        Nums.asDoubleNum(open).orElse(null),
        Nums.asDoubleNum(high).orElse(null),
        Nums.asDoubleNum(low).orElse(null),
        Nums.asDoubleNum(close).orElse(null),
        Nums.asDoubleNum(volume).orElse(null));
  }

  public static Bar create(
      LocalDate d, Number open, Number high, Number low, Number close, Number volume) {
    return create(d, open, high, low, close, volume, null);
  }

  public static Bar create(
      LocalDate d, Number open, Number high, Number low, Number close, Number volume, Long id) {
    Preconditions.checkNotNull(d);
    ImmutableBar bar = new ImmutableBar();
    bar.beginTime = d.atStartOfDay(Zones.UTC);
    bar.endTime = d.plusDays(1).atStartOfDay(Zones.UTC);
    bar.timePeriod = Duration.ofDays(1);

    bar.open = open != null ? DoubleNum.valueOf(open) : null;
    bar.high = high != null ? DoubleNum.valueOf(high) : null;
    bar.low = low != null ? DoubleNum.valueOf(low) : null;
    bar.close = close != null ? DoubleNum.valueOf(close) : null;
    bar.volume = volume != null ? DoubleNum.valueOf(volume) : null;
    bar.id = id;
    return bar;
  }

  public int compareTo(Bar b) {

    return Bars.ascendingDateOrder().compare(this, b);
  }

  boolean isDaily() {
    return timePeriod.equals(Duration.ofDays(1));
  }

  private String formatVolume(Num n) {
    if (n == null) {
      return "";
    }
    if (n.isNaN()) {
      return "NaN";
    }
    BigDecimal d = new BigDecimal(n.doubleValue());
    d = d.setScale(0, RoundingMode.UP);
    return d.toPlainString();
  }

  private String formatPrice(Num n) {
    if (n == null) {
      return "";
    }

    double d = n.doubleValue();
    if (Double.isFinite(d)) {
      BigDecimal bd = new BigDecimal(d).setScale(1, RoundingMode.HALF_UP);
      return bd.toPlainString();
    }
    return n.toString();
  }

  public String toString() {

    ToStringHelper h = MoreObjects.toStringHelper("Bar");

    if (id != null) {
      h = h.add("id", id);
    }
    h =
        h.add("date", isDaily() ? beginTime.toLocalDate().toString() : beginTime.toString())
            .add("open", formatPrice(open))
            .add("high", formatPrice(high))
            .add("low", formatPrice(low))
            .add("close", formatPrice(close))
            .add("volume", formatVolume(volume));

    return h.toString();
  }
}
