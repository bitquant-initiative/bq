package bq.ducktape.chart;

import bq.ducktape.BarSeriesTable;
import bq.ducktape.BaseTest;
import bq.ducktape.DuckTape;
import java.io.File;
import org.junit.jupiter.api.Test;

public class ChartTest extends BaseTest {

  @Test
  public void testIt() {

    DuckTape tape = DuckTape.createInMemory();

    BarSeriesTable table = tape.importTable("btc", new File("./btc.csv"));

    Chart.newChart()
        .title("foo")
        .trace(
            "test",
            c -> {
              c.addData(table.getBarSeries());
            })
        .view();
  }
}
