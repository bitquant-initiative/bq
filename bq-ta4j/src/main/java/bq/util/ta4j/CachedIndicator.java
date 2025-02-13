package bq.util.ta4j;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

/**
 * Simpler alternative to TA4J's odd implementation.
 */
public abstract class CachedIndicator implements Indicator<Num> {

  BarSeries barSeries;

  Cache<Integer, Optional<Num>> cache;

  public CachedIndicator(BarSeries barSeries) {
    this(
        barSeries,
        CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .maximumSize(10000)
            .build());
  }

  public CachedIndicator(BarSeries barSeries, Cache<Integer, Optional<Num>> cache) {
    this.barSeries = barSeries;
    Preconditions.checkNotNull(cache, "cache");
    this.cache = cache;
  }

  public static Indicator<Num> of(Indicator<Num> uncachedIndicator) {
    return of(uncachedIndicator, defaultCache());
  }

  public static Indicator<Num> of(
      final Indicator<Num> uncachedIndicator, Cache<Integer, Optional<Num>> cache) {

    Preconditions.checkNotNull(uncachedIndicator);
    CachedIndicator ci =
        new CachedIndicator(uncachedIndicator.getBarSeries(), cache) {

          @Override
          public Num calculate(int index) {
            return uncachedIndicator.getValue(index);
          }

          @Override
          public int getUnstableBars() {
            return uncachedIndicator.getUnstableBars();
          }

          @Override
          public BarSeries getBarSeries() {

            return uncachedIndicator.getBarSeries();
          }
        };
    return ci;
  }

  @Override
  public Num getValue(int index) {
    Optional<Num> val = cache.getIfPresent(index);
    if (val != null) {
      return val.orElse(null);
    }

    Num v = calculate(index);
    cache.put(index, Optional.ofNullable(v));
    return v;
  }

  public abstract Num calculate(int index);

  @Override
  public int getUnstableBars() {
    return 0;
  }

  @Override
  public BarSeries getBarSeries() {
    return barSeries;
  }

  private static Cache<Integer, Optional<Num>> defaultCache() {
    return CacheBuilder.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .maximumSize(10000)
        .build();
  }
}
