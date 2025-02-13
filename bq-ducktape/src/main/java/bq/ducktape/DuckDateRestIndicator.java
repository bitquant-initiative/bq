package bq.ducktape;

import bq.duckdb.DuckDb;
import com.google.common.flogger.FluentLogger;
import com.google.common.hash.Hashing;
import org.ta4j.core.BarSeries;

public class DuckDateRestIndicator extends DuckDateIndicator {

  FluentLogger logger = FluentLogger.forEnclosingClass();

  String url;

  public DuckDateRestIndicator(BarSeries bs, DuckDb db, String url, String dateCol, String valCol) {
    super(bs, db, null, dateCol, valCol);
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public void beforeSelect() {

    String table = "tmp_" + Hashing.sha256().hashBytes(url.getBytes()).toString();
    setTableName(table);
    if (!getDb().table(getTableName()).exists()) {

      logger.atInfo().log("lodading %s into table %s", url, getTableName());
      String sql =
          String.format(
              "create or replace table %s as (select * from '%s' order by %s)",
              getTableName(), url, getDateColumn());

      getDb().template().execute(sql);
    }
  }
}
