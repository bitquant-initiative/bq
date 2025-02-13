package bq.util;

import java.time.ZonedDateTime;
import java.util.Optional;

public class DateTimeNumberPoint extends DateNumberPoint {

  public DateTimeNumberPoint(ZonedDateTime dt, Number n) {
    super(dt, n);
  }

  @Override
  public ZonedDateTime getZonedDateTime() {
    return super.getZonedDateTime();
  }

  public String toString() {

    return String.format("(%s, %s)", getDateTime(), val);
  }

  public Optional<DateTimeNumberPoint> notNull() {
    if (val == null) {
      return Optional.empty();
    }
    return Optional.of(this);
  }
}
