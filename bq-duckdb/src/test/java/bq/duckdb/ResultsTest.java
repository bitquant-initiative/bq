package bq.duckdb;

import bq.util.Zones;
import com.google.common.flogger.FluentLogger;
import java.time.LocalDate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResultsTest {

  static FluentLogger logger = FluentLogger.forEnclosingClass();

  @Test
  public void testIt() {

    DuckDb d = DuckDb.createInMemory();

    d.operations().setSessionTimeZone(Zones.UTC);
    d.template().execute("create table test (date date,ts timestamp with time zone , count int)");

    d.table("test")
        .append(
            c -> {
              c.beginRow();
              c.append("2024-01-01");
              c.append("2024-01-01 03:22:00");
              c.append(2);
              c.endRow();
            });

    logger.atInfo().log("\n%s", d.table("test").toPrettyString());

    d.template().log().query("SELECT value FROM duckdb_settings() WHERE name = 'TimeZone'");

    d.template()
        .queryResult(
            b -> {
              b.sql("select * from test");
            },
            rs -> {
              while (rs.next()) {

                Assertions.assertThat(rs.getLocalDate("date").get())
                    .isEqualTo(LocalDate.of(2024, 1, 1));
                Assertions.assertThat(rs.getInstant("date").get())
                    .isEqualTo("2024-01-01T00:00:00Z");
                Assertions.assertThat(rs.getZonedDateTime("date").get().toInstant().toString())
                    .isEqualTo("2024-01-01T00:00:00Z");
                Assertions.assertThat(rs.getString("date").get()).isEqualTo("2024-01-01");

                Assertions.assertThat(rs.getLocalDate("ts").get())
                    .isEqualTo(LocalDate.of(2024, 1, 1));
                Assertions.assertThat(rs.getInstant("ts").get().toString())
                    .isEqualTo("2024-01-01T03:22:00Z");
                Assertions.assertThat(rs.getZonedDateTime("ts").get().toInstant().toString())
                    .isEqualTo("2024-01-01T03:22:00Z");
                Assertions.assertThat(rs.getString("ts").get()).isEqualTo("2024-01-01T03:22Z");
              }
              return "";
            });
  }
}
