package bq.indicator;

import bq.duckdb.DuckDb;
import bq.ducktape.BarSeriesTable;
import bq.ducktape.DuckTape;
import bq.ducktape.chart.Chart;
import com.google.common.flogger.FluentLogger;
import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

public abstract class IndicatorTest {

  static FluentLogger logger = FluentLogger.forEnclosingClass();
  DuckTape tape;

  BarSeriesTable btcTable;
  BarSeriesTable wgmiTable;

  DuckDb db;

  static AtomicInteger testCounter = new AtomicInteger();

  public DuckTape tape() {
    if (this.tape == null) {
      this.tape = DuckTape.create(getDb());
    }
    return this.tape;
  }

  public DuckDb getDb() {
    if (db != null) {
      return db;
    }
    db = DuckDb.createInMemory();
    return db;
  }

  public BarSeriesTable loadWGMI() {
    if (wgmiTable == null) {
      wgmiTable = tape().importTable("wgmi", new File("./src/test/resources/WGMI.csv"));
    }
    return wgmiTable;
  }

  public BarSeriesTable loadBtcTable() {
    if (btcTable == null) {
      if (tape == null) {
        tape = DuckTape.create(getDb());
      }
      btcTable = tape.importTable("btc", new File("./btc-price-data.csv"));
      ;
    }
    return btcTable;
  }

  @BeforeEach
  public void setup(TestInfo info) {

    // It's nice to be able to have charts open in a browser when running one test at a time
    // But it is irritating to have a bunch of tabs open when running a suite.
    //
    // This simple heuristic will disable the browser capability if more than one test is run.

    int count = testCounter.incrementAndGet();
    if (count > 1) {
      Chart.disableBrowser();
    }
  }

  @AfterEach
  public void cleanup() {

    if (tape != null) {

      tape.close();
    }
    if (db != null) {
      db.close();
    }
  }
}
