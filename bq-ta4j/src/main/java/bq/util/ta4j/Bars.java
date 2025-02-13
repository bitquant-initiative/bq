package bq.util.ta4j;

import bq.util.Item;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.Num;

public class Bars {

  private Bars() {}

  /**
   * TA4J BarSeries indexes are unusual *AND* necessary to use indicators.
   * Using the Item<Bar> construct makes it easy to iterate across a bar series and,
   * for example, get Indicator values.
   * @param barSeries
   * @return
   */
  public static Stream<Item<Bar>> itemStream(BarSeries barSeries) {
    return itemList(barSeries).stream();
  }

  /**
   * TA4J BarSeries indexes are unusual *AND* necessary to use indicators.
   * Using the Item<Bar> construct makes it easy to iterate across a bar series and,
   * for example, get Indicator values.
   * @param barSeries
   * @return
   */
  public static List<Item<Bar>> itemList(BarSeries barSeries) {

    List<Item<Bar>> items = Lists.newArrayList();
    for (int i = barSeries.getBeginIndex(); i <= barSeries.getEndIndex(); i++) {
      items.add(Item.of(barSeries.getBar(i), i));
    }
    return List.copyOf(items);
  }

  public static Stream<Bar> toStream(BarSeries barSeries) {
    return stream(barSeries);
  }

  public static Stream<Bar> stream(BarSeries barSeries) {
    return toList(barSeries).stream();
  }

  private static Optional<Bar> findBar(BarSeries bs, ZonedDateTime dt) {
    // I think it is a mistake to implement this method. There are many
    // edge cases
    throw new UnsupportedOperationException();
  }

  public static Optional<Bar> findBarOnOrAfter(BarSeries bs, ZonedDateTime dt) {
    Preconditions.checkNotNull(dt);
    return toStream(bs).filter(notBefore(dt)).findFirst();
  }

  public static Optional<Bar> findBarOnOrAfter(BarSeries bs, LocalDate d) {
    Preconditions.checkNotNull(d);
    return toStream(bs).filter(notBefore(d)).findFirst();
  }

  public static Optional<Bar> findBar(BarSeries bs, LocalDate d) {

    Preconditions.checkNotNull(d);
    return toStream(bs).filter(on(d)).findFirst();
  }

  public static List<Bar> toList(BarSeries barSeries) {

    Preconditions.checkNotNull(barSeries, "BarSeries cannot be null");
    if (barSeries instanceof ImmutableBarSeries) {
      // With ImmutableBarSeries we are certain that there is none of the crazy
      // mutable
      // implementation in barSeries, so we can just return.
      ImmutableBarSeries bs = (ImmutableBarSeries) barSeries;
      return List.copyOf(bs.barList);
    } else {
      List<Bar> copy = Lists.newArrayList();
      for (int i = barSeries.getBeginIndex(); i <= barSeries.getEndIndex(); i++) {
        copy.add(barSeries.getBar(i));
      }
      return List.copyOf(copy);
    }
  }

  public static Bar copyOf(Bar b) {
    Preconditions.checkNotNull(b);

    return ImmutableBar.copyOf(b);
  }

  public static BarSeries copyOf(BarSeries barSeries) {
    return ImmutableBarSeries.copyOf(barSeries);
  }

  public static Predicate<Bar> on(LocalDate d) {
    Predicate<Bar> p =
        new Predicate<Bar>() {

          @Override
          public boolean test(Bar t) {
            return t.getBeginTime().toLocalDate().equals(d);
          }
        };
    return p;
  }

  public static Predicate<Bar> notBefore(final LocalDate d) {
    Predicate<Bar> p =
        new Predicate<Bar>() {

          @Override
          public boolean test(Bar t) {
            return !t.getBeginTime().toLocalDate().isBefore(d);
          }
        };
    return p;
  }

  public static Predicate<Bar> notAfter(final LocalDate d) {
    Predicate<Bar> p =
        new Predicate<Bar>() {

          @Override
          public boolean test(Bar t) {
            return !t.getBeginTime().toLocalDate().isAfter(d);
          }
        };
    return p;
  }

  public static Predicate<Bar> notBefore(final ZonedDateTime d) {
    Predicate<Bar> p =
        new Predicate<Bar>() {

          @Override
          public boolean test(Bar t) {
            return !t.getBeginTime().isBefore(d);
          }
        };
    return p;
  }

  public static Predicate<Bar> notAfter(final ZonedDateTime d) {
    Predicate<Bar> p =
        new Predicate<Bar>() {

          @Override
          public boolean test(Bar t) {
            return !t.getBeginTime().isAfter(d);
          }
        };
    return p;
  }

  public static Optional<Double> closePrice(Bar b) {
    return Nums.asDouble(b.getClosePrice());
  }

  public static Optional<Double> openPrice(Bar b) {
    return Nums.asDouble(b.getOpenPrice());
  }

  public static Optional<Double> highPrice(Bar b) {
    return Nums.asDouble(b.getHighPrice());
  }

  public static Optional<Double> lowPrice(Bar b) {
    return Nums.asDouble(b.getLowPrice());
  }

  public static Optional<Double> volume(Bar b) {
    return Nums.asDouble(b.getVolume());
  }

  private static Map<Long, Bar> toDateMap(Collection<Bar> bars) {
    if (bars == null) {
      return Map.of();
    }
    Map<Long, Bar> map = Maps.newHashMap();
    for (Bar b : bars) {
      map.put(b.getBeginTime().toEpochSecond(), b);
    }
    return Map.copyOf(map);
  }

  /** Returns all the bars in seriea a that are not in series b.
   * Comparisons are performed based on beginTime.
   *
   * @param a
   * @param b
   * @return
   */
  public static Set<Bar> difference(BarSeries a, BarSeries b) {

    return difference(toList(a), toList(b));
  }

  public static Set<Bar> difference(Collection<Bar> a, Collection<Bar> b) {
    Preconditions.checkNotNull(a);
    Preconditions.checkNotNull(b);

    Map<Long, Bar> m1 = toDateMap(a);
    Map<Long, Bar> m2 = toDateMap(b);

    Set<Long> keysToRetain = Sets.difference(m1.keySet(), m2.keySet());

    Set<Bar> x =
        a.stream()
            .filter(bar -> keysToRetain.contains(bar.getBeginTime().toEpochSecond()))
            .collect(Collectors.toSet());

    return x;
  }

  public static BarSeries sort(BarSeries b) {
    List<Bar> bars = toList(b).stream().sorted(Bars.ascendingDateOrder()).toList();
    return ImmutableBarSeries.of(bars, b.getName());
  }

  public static Comparator<Bar> ascendingDateOrder() {

    Comparator<Bar> c =
        new Comparator<Bar>() {

          @Override
          public int compare(Bar b1, Bar b2) {

            int v =
                Long.compare(b1.getBeginTime().toEpochSecond(), b2.getBeginTime().toEpochSecond());
            if (v == 0) {
              return Long.compare(b1.getEndTime().toEpochSecond(), b2.getEndTime().toEpochSecond());
            }
            return v;
          }
        };
    return c;
  }

  public static BarSeries toBarSeries(List<Bar> bars, String name) {
    return ImmutableBarSeries.of(bars, name);
  }

  public static BarSeries empty() {
    return ImmutableBarSeries.empty();
  }

  public static Bar create(LocalDate d, Num open, Num high, Num low, Num close, Num volume) {
    return ImmutableBar.create(d, open, high, low, close, volume);
  }

  public static Bar create(
      LocalDate d, Number open, Number high, Number low, Number close, Number volume) {
    return ImmutableBar.create(d, open, high, low, close, volume);
  }

  public static Bar create(
      LocalDate d, Number open, Number high, Number low, Number close, Number volume, long id) {
    return ImmutableBar.create(d, open, high, low, close, volume, id);
  }
}
