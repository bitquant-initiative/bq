package bq.ducktape;

import bq.duckdb.DuckDb;
import bq.ducktape.chart.Chart;
import com.google.common.flogger.FluentLogger;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseTest extends bq.util.BqTest {

  static FluentLogger logger = FluentLogger.forEnclosingClass();

  private DuckTape tape;
  private BarSeriesTable btcTable = null;

  static AtomicInteger testCount = new AtomicInteger();

  List<DuckDb> closeQueue = Lists.newArrayList();

  public BarSeriesTable getBtcTable() {
    if (btcTable == null) {
      btcTable = tape().importTable("btc", new File("./src/test/resources/data/btc.csv"));
    }
    return btcTable;
  }

  public DuckTape tape() {
    return getDuckTape();
  }

  public DuckTape getDuckTape() {
    if (tape == null) {

      tape = DuckTape.create(db());
    }
    return tape;
  }

  @BeforeEach
  private void disableDesktop() {
    tape = null;
    btcTable = null;
    if (testCount.getAndIncrement() > 1) {
      Chart.disableBrowser();
    }
  }
}
