package bq.indicator.btc;

import bq.duckdb.DuckDb;
import bq.ducktape.DuckDateRestIndicator;
import com.google.common.flogger.FluentLogger;
import org.ta4j.core.BarSeries;

public class BtcMVRVZIndicator extends DuckDateRestIndicator {

  static FluentLogger logger = FluentLogger.forEnclosingClass();

  static String url =
      "https://s3.us-west-2.amazonaws.com/public.bitquant.cloud/indicators/1d/BTC.MVRVZ.csv";

  public BtcMVRVZIndicator(BarSeries series) {
    this(series, DuckDb.getSharedInMemory());
  }

  public BtcMVRVZIndicator(BarSeries series, DuckDb db) {
    super(series, db, url, "date", "value");
  }
}
