package bq.util.ta4j;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.List;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class ImmutableBarSeries implements BarSeries, Iterable<Bar> {

  String name;

  List<Bar> barList;

  public static BarSeries of(List<Bar> bars) {
    return of(bars, null);
  }

  public static BarSeries of(List<Bar> bars, String name) {

    if (bars == null) {
      bars = List.of();
    } else {
      bars = bars.stream().sorted(Bars.ascendingDateOrder()).toList();
    }
    ImmutableBarSeries bs = new ImmutableBarSeries(bars, name);
    bs.name = name;
    return bs;
  }

  public static BarSeries copyOf(BarSeries barSeries) {
    Preconditions.checkNotNull(barSeries);
    if (barSeries instanceof ImmutableBarSeries) {
      // we'll return a new instance to be in compliance with the method name
      ImmutableBarSeries src = (ImmutableBarSeries) barSeries;
      ImmutableBarSeries copy = new ImmutableBarSeries(src.barList, src.name);
      return copy;
    } else {
      List<Bar> bars = Lists.newArrayList();
      Bars.toList(barSeries)
          .forEach(
              b -> {
                bars.add(Bars.copyOf(b));
              });
      return ImmutableBarSeries.of(bars, barSeries.getName());
    }
  }

  protected ImmutableBarSeries(List<Bar> bars, String name) {
    if (bars == null) {
      bars = List.of();
    }
    this.barList = List.copyOf(bars);
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Num num() {

    return DoubleNum.ZERO;
  }

  @Override
  public Bar getBar(int i) {
    return barList.get(i);
  }

  @Override
  public int getBarCount() {
    return barList.size();
  }

  @Override
  public List<Bar> getBarData() {
    return List.copyOf(barList);
  }

  @Override
  public int getBeginIndex() {
    if (barList.isEmpty()) {
      return -1;
    }
    return 0;
  }

  @Override
  public int getEndIndex() {
    if (barList.isEmpty()) {
      return -1;
    }
    return barList.size() - 1;
  }

  @Override
  public int getMaximumBarCount() {

    return Integer.MAX_VALUE;
  }

  @Override
  public void setMaximumBarCount(int maximumBarCount) {
    // no-op

  }

  @Override
  public int getRemovedBarsCount() {

    return 0;
  }

  @Override
  public void addBar(Bar bar, boolean replace) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addBar(Duration timePeriod, ZonedDateTime endTime) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addBar(
      ZonedDateTime endTime,
      Num openPrice,
      Num highPrice,
      Num lowPrice,
      Num closePrice,
      Num volume,
      Num amount) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addBar(
      Duration timePeriod,
      ZonedDateTime endTime,
      Num openPrice,
      Num highPrice,
      Num lowPrice,
      Num closePrice,
      Num volume) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addBar(
      Duration timePeriod,
      ZonedDateTime endTime,
      Num openPrice,
      Num highPrice,
      Num lowPrice,
      Num closePrice,
      Num volume,
      Num amount) {
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

  @Override
  public BarSeries getSubSeries(int startIndex, int endIndex) {

    ImmutableBarSeries x = new ImmutableBarSeries(barList.subList(startIndex, endIndex), name);

    return x;
  }

  public List<Bar> bars() {
    return this.barList; // safe!
  }

  public Iterator<Bar> iterator() {
    return this.barList.iterator();
  }

  public static BarSeries empty() {
    return ImmutableBarSeries.of(List.of(), "empty");
  }

  private String formatDate(Bar b, ZonedDateTime dt) {
    if (dt == null) {
      return "";
    }
    if (b.getTimePeriod().getSeconds() == Duration.ofDays(1).getSeconds()) {
      return dt.toLocalDate().toString();
    }
    return dt.toString();
  }

  public String toString() {
    ToStringHelper h =
        MoreObjects.toStringHelper("BarSeries").add("name", getName()).add("count", barList.size());
    if (!barList.isEmpty()) {
      h = h.add("first", formatDate(getFirstBar(), getFirstBar().getBeginTime()));
      h = h.add("last", formatDate(getLastBar(), getLastBar().getBeginTime()));
    }

    return h.toString();
  }
}
