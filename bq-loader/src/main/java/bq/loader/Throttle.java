package bq.loader;

import bq.util.Sleep;
import com.google.common.flogger.FluentLogger;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Throttle {

  static FluentLogger logger = FluentLogger.forEnclosingClass();
  int maxRpm = 5;

  ZonedDateTime resetAt = null;
  AtomicInteger count = new AtomicInteger();

  public static Throttle rpm(int rpm) {
    Throttle t = new Throttle();
    t.maxRpm = rpm;
    return t;
  }

  public void acquire() {
    if (resetAt == null) {
      resetAt = ZonedDateTime.now().plus(1, ChronoUnit.MINUTES);
    }
    if (ZonedDateTime.now().isAfter(resetAt)) {
      count.set(0);
      resetAt = ZonedDateTime.now().plus(1, ChronoUnit.MINUTES);
    }
    if (count.get() >= maxRpm) {
      long wait = resetAt.toEpochSecond() - ZonedDateTime.now().toEpochSecond() + 1;
      if (wait > 0) {
        logger.atInfo().log("pausing %d seconds for rate limit", wait);
        Sleep.sleep(wait, ChronoUnit.SECONDS);
        if (ZonedDateTime.now().isAfter(resetAt)) {
          resetAt = ZonedDateTime.now().plus(1, ChronoUnit.MINUTES);
          count.set(1);
          return;
        }
      }
    }
    count.incrementAndGet();
  }
}
