package bq.ducktape;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

public class ReadmeTest {

  void readme() {
    DuckTape tape = DuckTape.createInMemory();
    BarSeriesTable table = tape.importTable("btc", "./btc.csv");
    BarSeries barSeries = table.getBarSeries();

    table.addIndicator("sma(20)", "sma_20");

    Indicator<Num> closeIndicator = table.getIndicator("close");

    DuckTape shared = DuckTape.getSharedInMemory();
    tape.getDb();
  }
}
