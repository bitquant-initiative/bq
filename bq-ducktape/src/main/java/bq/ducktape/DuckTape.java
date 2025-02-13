package bq.ducktape;

import bq.duckdb.DuckConnection;
import bq.duckdb.DuckDb;
import bq.sql.DbException;
import bq.util.BqException;
import bq.util.ta4j.Bars;
import bq.util.ta4j.Nums;
import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.function.Supplier;
import org.duckdb.DuckDBConnection;
import org.ta4j.core.BarSeries;

public class DuckTape {

  private static Supplier<DuckTape> singletonSupplier =
      Suppliers.memoize(DuckTape::createSharedInMemory);
  private DuckDb db;

  private DuckTape() {}

  public static DuckTape getSharedInMemory() {
    return singletonSupplier.get();
  }

  private static DuckTape createSharedInMemory() {
    // should only be called through supplier
    return DuckTape.create(DuckDb.getSharedInMemory());
  }

  public static DuckTape createInMemory() {
    DuckTape d = new DuckTape();
    d.db = DuckDb.createInMemory();
    return d;
  }

  public static DuckTape create(DuckDb d) {
    Preconditions.checkNotNull(d, "Duck argument cannot be null");
    DuckTape t = new DuckTape();
    t.db = d;
    return t;
  }

  public static DuckTape create(File file) {
    Preconditions.checkNotNull(file, "file cannot be null");
    try {
      String url = String.format("jdbc:duckdb:%s", file.getAbsolutePath());
      DuckDBConnection c = (DuckDBConnection) DriverManager.getConnection(url);

      return create(c);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public static DuckTape create(Connection c) {
    Preconditions.checkNotNull(c);

    DuckDb d = DuckDb.create(c);
    return DuckTape.create(d);
  }

  public BarSeries getBarSeries(String name) {
    return getTable(name).getBarSeries();
  }

  public BarSeriesTable getTable(String table) {
    return getTable(table, null);
  }

  public BarSeriesTable getTable(String table, String symbol) {
    return BarSeriesTable.of(db.table(table), symbol);
  }

  public boolean tableExists(String table) {
    return db.table(table).exists();
  }

  public void dropTable(String table) {
    if (!tableExists(table)) {
      return;
    }
    String sql = String.format("drop table %s", BarSeriesTable.sanitizeTable(table));

    db.template().execute(sql);
  }

  public void exportTable(String table, String url) {
    String sql =
        String.format(
            "COPY (SELECT * FROM %s) TO '%s' (HEADER, DELIMITER ',')",
            BarSeriesTable.sanitizeTable(table), toDuckSafeUrl(url));
    db.template().execute(sql);
  }

  public void exportTable(String table, File outputFile) {
    try {
      exportTable(table, toDuckSafeUrl(outputFile.toURI().toURL().toString()));
    } catch (IOException e) {
      throw new BqException(e);
    }
  }

  public BarSeriesTable createOHLCVTable(String name) {
    String sql =
        """
        create table {{table}} (
        date   date NOT NULL PRIMARY KEY,
        open   double NULL,
        high   double NULL,
        low    double NULL,
        close  double NULL,
        volume double NULL
        )
        """;
    sql = sql.replace("{{table}}", name);
    getDb().template().execute(sql);
    return getTable(name);
  }

  public BarSeriesTable importTable(String table, String url) {
    String sql =
        String.format(
            "create table %s as select * from read_csv('%s') order by date asc",
            BarSeriesTable.sanitizeTable(table), toDuckSafeUrl(url));

    db.template().execute(sql);

    return BarSeriesTable.of(db.table(table), null);
  }

  public DuckDb getDb() {
    return db;
  }

  public BarSeriesTable importTable(String table, File f) {

    return importTable(table, f.getAbsolutePath());
  }

  public DuckConnection getConnection() {
    return db.getConnection();
  }

  public void close() {
    db.close();
  }

  public void appendAll(BarSeriesTable table, BarSeries barSeries) {
    getDb()
        .table(table.getTableName())
        .append(
            appender -> {
              Bars.toStream(barSeries)
                  .forEach(
                      bar -> {
                        try {
                          appender.beginRow();
                          appender.appendLocalDateTime(bar.getBeginTime().toLocalDateTime());
                          appender.append(Nums.asDouble(bar.getOpenPrice()).orElse(null));
                          appender.append(Nums.asDouble(bar.getHighPrice()).orElse(null));
                          appender.append(Nums.asDouble(bar.getLowPrice()).orElse(null));
                          appender.append(Nums.asDouble(bar.getClosePrice()).orElse(null));
                          appender.append(Nums.asDouble(bar.getVolume()).orElse(null));
                          appender.endRow();
                        } catch (SQLException e) {
                          throw new BqException(e);
                        }
                      });
            });
  }

  /**
   * DuckDB doesn't accept file:// url patterns.
   *
   * @param input
   * @return
   */
  static String toDuckSafeUrl(final String input) {
    if (input == null) {
      return input;
    } else if (input.startsWith("file://")) {
      return input.substring("file://".length());
    } else if (input.startsWith("file:")) {
      return input.substring("file:".length());
    }
    return input;
  }

  public String toString() {

    return getDb().toString().replace("DuckDb{", "DuckTape{");
  }
}
