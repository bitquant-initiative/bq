package bq.util;

import static bq.util.Dates.asZonedDateTime;
import static bq.util.Dates.parseNumeric;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class DatesTest {

  @Test
  public void testIt() {
    ZonedDateTime dt = ZonedDateTime.now();

    System.out.println(dt.toString());
    Assertions.assertThat(Dates.asZonedDateTime(dt.toString()).get().toEpochSecond())
        .isEqualTo(dt.toEpochSecond());
  }

  @Test
  public void testX() {
    Assertions.assertThat(Dates.asZonedDateTime(null)).isEmpty();
    Assertions.assertThat(Dates.asZonedDateTime("")).isEmpty();
    Assertions.assertThat(Dates.asZonedDateTime("   ")).isEmpty();

    long refDt = 1733809243L;
    Assertions.assertThat(
            Dates.asZonedDateTime("2024-12-09T21:40:43.349104-08:00[America/Los_Angeles]")
                .get()
                .toInstant()
                .toEpochMilli())
        .isEqualTo(1733809243349L);
    Assertions.assertThat(
            Dates.asZonedDateTime("2024-12-09T21:40:43-08:00[America/Los_Angeles]")
                .get()
                .toInstant()
                .toEpochMilli())
        .isEqualTo(1733809243000L);

    Assertions.assertThat(
            Dates.asZonedDateTime("2024-12-09T21:40:43-08:00[America/Los_Angeles]")
                .get()
                .toEpochSecond())
        .isEqualTo(refDt);

    Assertions.assertThat(Dates.asZonedDateTime("2024-12-10T05:40:43Z[UTC]").get().toEpochSecond())
        .isEqualTo(refDt);
    Assertions.assertThat(Dates.asZonedDateTime("2024-12-10T05:40:43Z").get().toEpochSecond())
        .isEqualTo(refDt);

    Assertions.assertThat(Dates.asZonedDateTime("2024-12-10T05:40:43")).isEmpty();
    Assertions.assertThat(Dates.asZonedDateTime("2024-12-10T05:40:43", Zones.UTC).get().toString())
        .isEqualTo("2024-12-10T05:40:43Z[UTC]");

    Assertions.assertThat(
            Dates.asZonedDateTime("2024-12-10T05:40:43", Zones.UTC).get().toEpochSecond())
        .isEqualTo(refDt);

    // Now interpret it as a local date in NYC
    Assertions.assertThat(
            Dates.asZonedDateTime("2024-12-10T05:40:43", Zones.NYC).get().toEpochSecond())
        .isNotEqualTo(refDt);

    Assertions.assertThat(Dates.asZonedDateTime("2024-12-10T05:40:43", Zones.NYC).get().toString())
        .isEqualTo("2024-12-10T05:40:43-05:00[America/New_York]");

    Assertions.assertThat(Dates.asLocalDate("2024-12-10").get().getYear()).isEqualTo(2024);
    Assertions.assertThat(Dates.asLocalDate("2024-12-10").get().getMonthValue()).isEqualTo(12);
    Assertions.assertThat(Dates.asLocalDate("2024-12-10").get().getDayOfMonth()).isEqualTo(10);
    Assertions.assertThat(Dates.asLocalDate("20241210").get().getYear()).isEqualTo(2024);
    Assertions.assertThat(Dates.asLocalDate("20241210").get().getMonthValue()).isEqualTo(12);
    Assertions.assertThat(Dates.asLocalDate("20241210").get().getDayOfMonth()).isEqualTo(10);
    Assertions.assertThat(Dates.asLocalDate("2024/12/10").get().getYear()).isEqualTo(2024);
    Assertions.assertThat(Dates.asLocalDate("2024/12/10").get().getMonthValue()).isEqualTo(12);
    Assertions.assertThat(Dates.asLocalDate("2024/12/10").get().getDayOfMonth()).isEqualTo(10);

    Assertions.assertThat(Dates.asZonedDateTime("2024-12-10", Zones.UTC).get().toString())
        .isEqualTo("2024-12-10T00:00Z[UTC]");

    Assertions.assertThat(Dates.asZonedDateTime("2024-12-10", Zones.NYC).get().toString())
        .isEqualTo("2024-12-10T00:00-05:00[America/New_York]");

    Assertions.assertThat(Dates.asZonedDateTime("2024-12-10", Zones.UTC).get().toString())
        .isEqualTo("2024-12-10T00:00Z[UTC]");
    Assertions.assertThat(Dates.asZonedDateTime("2024-12-10", Zones.NYC).get().toString())
        .isEqualTo("2024-12-10T00:00-05:00[America/New_York]");

    Assertions.assertThat(asZonedDateTime("2024/12/10", Zones.UTC).get().toString())
        .isEqualTo("2024-12-10T00:00Z[UTC]");
  }

  @Test
  public void testInterpretEpoch() {

    Assertions.assertThat(parseNumeric("19000101").get().toString())
        .isEqualTo("1900-01-01T00:00Z[UTC]");
    Assertions.assertThat(parseNumeric("20991231").get().toString())
        .isEqualTo("2099-12-31T00:00Z[UTC]");

    Assertions.assertThat(parseNumeric("100000000").get().toString())
        .isEqualTo("1973-03-03T09:46:40Z[UTC]");

    System.out.println(ZonedDateTime.of(2100, 1, 1, 0, 0, 0, 0, Zones.UTC).toEpochSecond());

    for (int y = 1970; y < 2200; y++) {
      ZonedDateTime x = ZonedDateTime.of(y, 1, 1, 0, 0, 0, 0, Zones.UTC);
      String yyyymmdd = x.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

      Assertions.assertThat(parseNumeric(yyyymmdd).get().toEpochSecond())
          .isEqualTo(x.toEpochSecond());
      Assertions.assertThat(parseNumeric(Long.toString(x.toEpochSecond())).get().toString())
          .isEqualTo(x.toString());
      Assertions.assertThat(
              parseNumeric(Long.toString(x.toInstant().toEpochMilli())).get().toString())
          .isEqualTo(x.toString());

      Assertions.assertThat(asZonedDateTime(yyyymmdd).get().toEpochSecond())
          .isEqualTo(x.toEpochSecond());
    }
    System.out.println(Instant.ofEpochMilli(4102444800L));
  }

  @Test
  public void testXX() {
    LocalDate d = LocalDate.of(2025, 5, 31);

    Assertions.assertThat(Dates.asZonedDateTime(d.toString())).isEmpty();
    Assertions.assertThat(Dates.asZonedDateTime(d.toString(), Zones.LAX).get().toString())
        .isEqualTo(d.atStartOfDay(Zones.LAX).toString());

    Assertions.assertThat(Dates.asInstant(d.toString())).isEmpty();

    Assertions.assertThat(Dates.asLocalDate("2025/05/31").get().toString()).isEqualTo("2025-05-31");
    Assertions.assertThat(Dates.asLocalDate("20250531").get().toString()).isEqualTo("2025-05-31");
  }

  @Test
  public void testAsLocalDate() {

    long ts = 1738543678283L;
    Instant t = Instant.ofEpochMilli(ts);
    ZonedDateTime zdt = ZonedDateTime.ofInstant(t, Zones.UTC);
    LocalDate ld = LocalDate.of(2025, 2, 3);

    Assertions.assertThat(Dates.asLocalDate(zdt.toString()).get()).isEqualTo(ld.toString());
    Assertions.assertThat(Dates.asLocalDate(t.toString()).get()).isEqualTo(ld.toString());

    Assertions.assertThat(Dates.asLocalDate("" + ts).get().toString()).isEqualTo("2025-02-03");
    Assertions.assertThat(Dates.asLocalDate("" + 1738543678L).get().toString())
        .isEqualTo("2025-02-03");
  }
}
