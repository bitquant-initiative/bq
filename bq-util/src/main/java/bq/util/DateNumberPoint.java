package bq.util;

import com.google.common.base.Preconditions;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;

public class DateNumberPoint implements Comparable<DateNumberPoint> {

  ZonedDateTime date;
  Number val;

  public DateNumberPoint(LocalDate d, Number n) {
    Preconditions.checkNotNull(d, "date");
    this.date = d.atStartOfDay(Zones.UTC);
    this.val = n;
  }

  protected DateNumberPoint(ZonedDateTime dateTime, Number n) {
    Preconditions.checkNotNull(dateTime, "dateTime");
    this.date = dateTime;
    this.val = n;
  }

  public LocalDate getDate() {
    return date.toLocalDate();
  }

  public Optional<? extends DateNumberPoint> notNull() {
    if (val == null) {
      return Optional.empty();
    }
    return Optional.of(this);
  }

  protected ZonedDateTime getZonedDateTime() {
    return date;
  }

  protected LocalDateTime getDateTime() {
    return date.toLocalDateTime();
  }

  public Optional<Number> getValue() {
    return Optional.ofNullable(val);
  }

  public Optional<Double> getDouble() {
    if (val == null) {
      return Optional.empty();
    }
    return Optional.of(val.doubleValue());
  }

  public Optional<Long> getLong() {
    if (val == null) {
      return Optional.empty();
    }
    return Optional.of(val.longValue());
  }

  public String toString() {
    return String.format("(%s, %s)", getDate(), val);
  }

  @Override
  public int compareTo(DateNumberPoint o) {

    if (o == null) {
      return 1;
    }
    return Numbers.numberComparator(val, o.val);
  }
}
