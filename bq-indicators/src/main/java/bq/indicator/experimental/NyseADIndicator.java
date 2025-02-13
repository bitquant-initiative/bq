package bq.indicator.experimental;

import bq.duckdb.DuckDb;
import bq.ducktape.DuckDateRestIndicator;
import org.ta4j.core.BarSeries;

public class NyseADIndicator extends DuckDateRestIndicator {

  // String url =
  // "https://s3.us-west-2.amazonaws.com/data.bitquant.cloud/indicators/1d/NYSE.BREADTH.csv";

  static String url =
      "https://s3.us-west-2.amazonaws.com/data.bitquant.cloud/indicators/1d/NYSE.BREADTH.csv";

  public NyseADIndicator(BarSeries bs) {
    this(bs, DuckDb.getSharedInMemory());
  }

  public NyseADIndicator(BarSeries bs, DuckDb db) {
    super(bs, db, url, "date", "adv_count");
  }

  @Override
  public void beforeSelect() {

    super.beforeSelect(); // will create the table
  }
}
