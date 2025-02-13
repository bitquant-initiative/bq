package bq.sql;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public class SqlUtil {

  public static Timestamp toTimestamp(ZonedDateTime d) {
    if (d == null) {
      return null;
    }
    return toTimestamp(d.toInstant());
  }

  public static Timestamp toTimestamp(Instant t) {
    if (t == null) {
      return null;
    }
    return Timestamp.from(t);
  }

  public static Object toSqlBindType(Object val) {
    if (val == null) {
      return val;
    }
    if (val instanceof Instant) {
      Instant t = (Instant) val;
      java.sql.Timestamp x = new java.sql.Timestamp(t.toEpochMilli());
      return x;
    }
    if (val instanceof ZonedDateTime) {
      ZonedDateTime t = (ZonedDateTime) val;
      java.sql.Timestamp x = new java.sql.Timestamp(t.toInstant().toEpochMilli());
      return x;
    }
    if (val instanceof LocalDate) {
      LocalDate d = (LocalDate) val;
      return d.toString();
    }

    return val;
  }
}
