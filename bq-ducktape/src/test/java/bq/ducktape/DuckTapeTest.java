package bq.ducktape;

import bq.duckdb.DuckDb;
import com.google.common.flogger.FluentLogger;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

public class DuckTapeTest extends BaseTest {
  FluentLogger logger = FluentLogger.forEnclosingClass();

  public static final String BTC_GITHUB_URL =
      "https://raw.githubusercontent.com/bitquant-initiative/bq/bq-ducktape/refs/heads/main/btc.csv";
  public static final String BTC_URL = "file:./btc.csv";

  @Test
  public void testRemote() {

    tape().importTable("test", BTC_URL);

    BarSeries bs = tape().getBarSeries("test");
    Bar lastBar = null;
    for (Bar bar : bs.getBarData()) {

      if (lastBar != null) {
        Assertions.assertThat(bar.getBeginTime()).isAfter(lastBar.getBeginTime());
        Assertions.assertThat(bar.getBeginTime())
            .isEqualTo(lastBar.getBeginTime().plus(1, ChronoUnit.DAYS));
      }

      lastBar = bar;
    }
  }

  @Test
  public void testToDuckSafeUrl() throws IOException {

    File f = new File("./btc.csv");

    Assertions.assertThat(DuckTape.toDuckSafeUrl("file:///tmp/test.csv"))
        .isEqualTo("/tmp/test.csv");
    Assertions.assertThat(DuckTape.toDuckSafeUrl("file:/tmp/test.csv")).isEqualTo("/tmp/test.csv");
    Assertions.assertThat(DuckTape.toDuckSafeUrl("file:btc.csv")).isEqualTo("btc.csv");

    Assertions.assertThat(DuckTape.toDuckSafeUrl("btc.csv")).isEqualTo("btc.csv");
    Assertions.assertThat(DuckTape.toDuckSafeUrl("s3://bucket/btc.csv"))
        .isEqualTo("s3://bucket/btc.csv");
  }

  @Test
  public void testXX() throws IOException {

    tape().importTable("test", "./btc.csv");

    tape().getDb().template().execute("delete from test where date <'2016-01-01'");
    tape().getDb().template().execute("delete from test where date >'2024-12-31'");

    tape().exportTable("test", "./b.csv");
    System.out.println(tape().getBarSeries("test"));
  }

  @Test
  void testIt() throws SQLException {

    tape().importTable("test", new File("./src/test/resources/data/btc.csv"));

    BarSeriesTable t = tape().getTable("test");

    for (Bar bar : t.getBarSeries().getBarData()) {
      // System.out.println(bar);
    }

    ClosePriceIndicator cpi = new ClosePriceIndicator(t.getBarSeries());

    Assertions.assertThat(cpi.getBarSeries()).isSameAs(t.getBarSeries());

    BarSeries bs = t.getBarSeries();
    for (int i = 0; i < bs.getBarCount(); i++) {
      Assertions.assertThat(bs.getBar(i).getClosePrice().doubleValue())
          .isEqualTo(cpi.getValue(i).doubleValue());
    }

    t.addIndicator(new ClosePriceIndicator(t.getBarSeries()), "close_price");

    var openIndicator = t.getIndicator("open");
    for (int i = 0; i < bs.getBarCount(); i++) {

      Num indicatorVal = openIndicator.getValue(i);
      Num barVal = bs.getBar(i).getOpenPrice();

      logger.atInfo().log("bar val=%s indicator=%s", barVal, indicatorVal);
      if (barVal == null) {
        // if the open price on the bar is null, the indicator should be as well
        Assertions.assertThat(indicatorVal).isNull();
      } else {
        Assertions.assertThat(indicatorVal).isNotNull();
      }

      Assertions.assertThat(bs.getBar(i).getClosePrice().doubleValue())
          .isEqualTo(cpi.getValue(i).doubleValue());
    }
  }

  @Test
  public void testAddIndicator() {
    tape().importTable("btc", BTC_URL);

    BarSeries bs = tape().getBarSeries("btc");

    tape().getTable("btc").addIndicator("sma(20)");

    // now validate that we can select the column we just created
    tape().getDb().template().log().query("select date,close,sma from btc");

    tape().getTable("btc").addIndicator("sma(50) as sma_50");

    // now validate that we can select the column we just created
    tape().getDb().template().log().query("select date,close,sma,sma_50 from btc");

    tape().getTable("btc").addIndicator("sma(100)", "sma_100");

    // now validate that we can select the column we just created
    tape().getDb().template().log().query("select date,close,sma,sma_50,sma_100 from btc");
  }

  @Test
  public void testInMemoryShared() throws SQLException {
    Assertions.assertThat(DuckTape.getSharedInMemory()).isSameAs(DuckTape.getSharedInMemory());
    Assertions.assertThat(DuckTape.getSharedInMemory().getConnection())
        .isSameAs(DuckTape.getSharedInMemory().getConnection());
    DuckTape.getSharedInMemory().getConnection().close();

    Assertions.assertThat(DuckTape.getSharedInMemory().getConnection().isClosed()).isFalse();
  }

  @Test
  public void addColumnWithConflictingName() {
    var table = tape().importTable("btc", BTC_URL);

    table.addIndicator("sma(50) as incorrect", "correct");

    tape().getDb().template().execute("select date,correct from btc");
  }

  @Test
  public void testCreateOHLCVTable() {
    String tableName = "test_table";
    BarSeriesTable t = tape().createOHLCVTable(tableName);

    Assertions.assertThat(t.getTableName()).isEqualTo(tableName);

    tape()
        .getDb()
        .template()
        .query(
            c -> c.sql("select * from " + tableName),
            rs -> {
              return "";
            });
    Assertions.assertThat(t);
    tape().dropTable(tableName);
    Assertions.assertThat(tape().tableExists(tableName)).isFalse();
    Assertions.assertThat(t.tableExists());
  }

  @Test
  public void testTableDropCheck() {

    String tableName = "test";
    tape().dropTable(tableName);
    tape().dropTable(tableName);
    Assertions.assertThat(tape().tableExists(tableName)).isFalse();

    tape().getDb().template().execute("create table " + tableName + " (abc int)");
    Assertions.assertThat(tape().tableExists(tableName)).isTrue();
    tape().dropTable(tableName);
    Assertions.assertThat(tape().tableExists(tableName)).isFalse();
  }

  @Test
  public void testGlobalSingletons() {
    Assertions.assertThat(DuckDb.getSharedInMemory()).isSameAs(DuckDb.getSharedInMemory());
    Assertions.assertThat(DuckTape.getSharedInMemory()).isSameAs(DuckTape.getSharedInMemory());
    Assertions.assertThat(DuckDb.getSharedInMemory())
        .isSameAs(DuckTape.getSharedInMemory().getDb());
    Assertions.assertThat(DuckTape.getSharedInMemory()).isSameAs(DuckTape.getSharedInMemory());

    DuckTape a = DuckTape.createInMemory();
    DuckTape b = DuckTape.createInMemory();

    Assertions.assertThat(a).isNotSameAs(b);
    Assertions.assertThat(a.getDb()).isNotSameAs(b.getDb());

    a.close();
    b.close();
  }
}
