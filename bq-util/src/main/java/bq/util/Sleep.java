package bq.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class Sleep {

  private Sleep() {}

  static TimeUnit toTimeUnit(ChronoUnit u) {
    switch (u) {
      case ChronoUnit.DAYS:
        return TimeUnit.DAYS;
      case ChronoUnit.HOURS:
        return TimeUnit.HOURS;
      case ChronoUnit.MINUTES:
        return TimeUnit.MINUTES;
      case ChronoUnit.SECONDS:
        return TimeUnit.SECONDS;
      case ChronoUnit.MICROS:
        return TimeUnit.MICROSECONDS;
      case ChronoUnit.NANOS:
        return TimeUnit.NANOSECONDS;
    }
    ;

    throw new IllegalArgumentException(u + " has no TimeUnit equivalent");
  }

  static ChronoUnit toChronoUnit(TimeUnit u) {
    switch (u) {
      case TimeUnit.DAYS:
        return ChronoUnit.DAYS;
      case TimeUnit.HOURS:
        return ChronoUnit.HOURS;
      case TimeUnit.MINUTES:
        return ChronoUnit.MINUTES;
      case TimeUnit.SECONDS:
        return ChronoUnit.SECONDS;
      case TimeUnit.MICROSECONDS:
        return ChronoUnit.MICROS;
      case TimeUnit.NANOSECONDS:
        return ChronoUnit.NANOS;
    }
    ;

    throw new IllegalArgumentException(u + " has no ChronoUnit equivalent");
  }

  public static void sleepSecs(int s) {
    sleep(Duration.ofSeconds(s));
  }

  public static void sleep(Duration d) {
    if (d == null) {
      return;
    }
    sleep(d.toMillis(), TimeUnit.MILLISECONDS);
  }

  public static void sleep(long t, ChronoUnit unit) {
    sleep(t, toTimeUnit(unit));
  }

  public static void sleep(long t) {
    sleep(t, TimeUnit.MILLISECONDS);
  }

  public static void sleep(long t, TimeUnit unit) {
    try {
      Thread.sleep(unit.toMillis(t));
    } catch (InterruptedException ignore) {
    }
  }
}
