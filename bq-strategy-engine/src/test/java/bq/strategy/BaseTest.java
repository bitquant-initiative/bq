package bq.strategy;

import bq.duckdb.DuckDb;
import bq.ducktape.BarSeriesTable;
import bq.ducktape.DuckTape;
import bq.ducktape.chart.Chart;
import com.google.common.flogger.FluentLogger;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public abstract class BaseTest {

  static FluentLogger logger = FluentLogger.forEnclosingClass();

  public DuckDb db;
  public DuckTape tape;
  private BarSeriesTable btcTable = null;

  static AtomicInteger testCount = new AtomicInteger();

  List<DuckDb> closeQueue = Lists.newArrayList();

  public BarSeriesTable getBtcTable() {
    if (btcTable == null) {
      btcTable = tape().importTable("btc", new File("./btc.csv"));
    }
    return btcTable;
  }

  public DuckDb getDb() {
    if (db == null) {
      db = DuckDb.createInMemory();
    }
    return db;
  }

  public DuckTape tape() {
    return getDuckTape();
  }

  public DuckDb db() {
    return getDb();
  }

  public DuckTape getDuckTape() {
    if (tape == null) {
      tape = DuckTape.create(getDb());
    }
    return tape;
  }

  public void defer(DuckDb db) {
    if (db != null) {
      this.closeQueue.add(db);
    }
  }

  @Test
  @Order(-100)
  public void preventChartFromRenderingViaSuite() {
    // This prevents charts from rendering when run as part of a suite.
    // it's very convenient to have charts pop a broswer window open
    // When testing them one-at-a-time, but incredibly annoying when run as a suite.
    Chart.disableBrowser();
  }

  @BeforeEach
  private void disableDesktop() {
    this.tape = getDuckTape();
    if (testCount.getAndIncrement() > 1) {
      Chart.disableBrowser();
    }
  }

  @AfterEach
  public final void cleanup() {
    btcTable = null;

    if (tape != null) {
      tape.close();
    }
    closeQueue.forEach(
        db -> {
          try {
            db.close();
          } catch (RuntimeException e) {
            logger.atWarning().withCause(e).log();
          }
        });
    tape = null;
  }
}
