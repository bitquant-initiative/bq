package bq.duckdb;

import java.time.ZoneId;

public class Operations {

  DuckDb db;

  protected Operations(DuckDb db) {
    this.db = db;
  }

  public ZoneId getSessionTimeZone() {

    String sql = "SELECT value FROM duckdb_settings() WHERE name = 'TimeZone'";

    return db.template()
        .query(
            sql,
            rs -> {
              return ZoneId.of(rs.getString("value").orElse(null));
            })
        .findFirst()
        .get();
  }

  public void setSessionTimeZone(ZoneId zone) {
    setSessionTimeZone(zone.toString());
  }

  public void setSessionTimeZone(String zone) {
    db.template().execute("set TimeZone = '" + zone + "'");
  }
}
