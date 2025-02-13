package bq.ducktape;

import bq.duckdb.DuckDb;
import bq.duckdb.DuckTable;
import bq.util.S;
import bq.util.ta4j.Bars;
import bq.util.ta4j.ImmutableBar;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.flogger.FluentLogger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.ta4j.core.Bar;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.Num;

/**
 * BarSeriesTable is a BarSeries that is backed by a DuckDB table.
 */
public class BarSeriesTable {

  static FluentLogger logger = FluentLogger.forEnclosingClass();

  DuckTable table;
  DuckBarSeries barSeries;

  String barSeriesName;

  private BarSeriesTable() {
    super();
  }

  public static BarSeriesTable of(DuckTable duckTable, String barSeriesName) {
    BarSeriesTable table = new BarSeriesTable();
    table.table = duckTable;
    table.barSeriesName = barSeriesName;
    table.barSeries = table.selectBarSeries();

    return table;
  }

  public BarSeriesTable reload() {
    this.barSeries = selectBarSeries();
    return this;
  }

  public DuckBarSeries getBarSeries() {
    return this.barSeries;
  }

  public String getTableName() {
    return this.table.getTableName();
  }

  static Bar toDuckBar(ResultSet row) throws SQLException {

    java.sql.Date date = row.getDate("date");

    LocalDate ld = date.toLocalDate();

    long rowId = row.getLong("rowid");
    Double open = row.getObject("open") != null ? row.getDouble("open") : null;
    Double high = row.getObject("high") != null ? row.getDouble("high") : null;
    Double low = row.getObject("low") != null ? row.getDouble("low") : null;
    Double close = row.getObject("close") != null ? row.getDouble("close") : null;
    Double volume = row.getObject("volume") != null ? row.getDouble("volume") : null;

    return ImmutableBar.create(ld, open, high, low, close, volume, rowId);
  }

  private DuckBarSeries selectBarSeries() {

    // the third argument is the BarSeries name NOT the table name
    return DuckBarSeries.create(this, selectBars(), barSeriesName);
  }

  List<Bar> selectBars() {

    String sql = "select rowid,date,open,high,low,close,volume from ##table## order by date asc";
    var bars =
        getDb()
            .template()
            .query(
                c -> {
                  c.sql(sql);
                  c.bind("table", getTableName());
                },
                rs -> {
                  return toDuckBar(rs.getResultSet());
                })
            .toList();

    return bars;
  }

  class Updatex {
    long rowId;
    String column;
    Double val;

    public String toString() {
      return MoreObjects.toStringHelper("Update")
          .add("rowId", rowId)
          .add("column", column)
          .add("val", val)
          .toString();
    }
  }

  public void updateRow(long rowId, String column, Num val) {

    Double dv = val != null ? val.doubleValue() : null;
    getDb().table(getTableName()).update(rowId, column, dv);
  }

  static String sanitizeTable(String s) {
    Preconditions.checkNotNull(s != null);
    Preconditions.checkArgument(s.trim().equals(s));
    Preconditions.checkArgument(s.length() > 0);
    s.chars()
        .allMatch(
            c -> {
              return Character.isLetterOrDigit(c) || c == '_';
            });
    return s;
  }

  static String sanitizeColumn(String s) {
    Preconditions.checkNotNull(s != null);
    Preconditions.checkArgument(s.trim().equals(s));
    Preconditions.checkArgument(s.length() > 0);
    s.chars()
        .allMatch(
            c -> {
              return Character.isLetterOrDigit(c) || c == '_';
            });
    return s;
  }

  void addDoubleColumn(String name) {

    name = sanitizeColumn(name);

    String sql = "alter table %s add column if not exists %s double";

    sql = String.format(sql, getTableName(), name);

    getDb().template().execute(sql);
  }

  public Indicator<Num> getIndicator(String column) {

    return new DuckColumnIndicator(barSeries, sanitizeColumn(column));
  }

  public long addIndicator(String expression, String column) {
    IndicatorExpression exp = IndicatorExpression.parse(expression);

    if (column == null) {
      column = exp.getOutputName().orElse(exp.getFunctionName());
    }

    Indicator<Num> t =
        IndicatorBuilder.newBuilder().table(getBarSeries().table).expression(expression).build();

    return addIndicator(t, column);
  }

  public long addIndicator(String expression) {
    IndicatorExpression exp = IndicatorExpression.parse(expression);
    String column = exp.getOutputName().orElse(null);
    if (S.isBlank(column)) {
      column = exp.getFunctionName();
    }

    Indicator<Num> t =
        IndicatorBuilder.newBuilder().table(getBarSeries().table).expression(expression).build();

    return addIndicator(t, column);
  }

  private Optional<Num> getNum(Indicator t, int i) {
    try {
      Object val = t.getValue(i);
      if (val == null) {
        return Optional.empty();
      }
      if (val instanceof Num) {
        return Optional.of((Num) val);
      }
    } catch (RuntimeException ignore) {
      // getting indicator values can generate a lot of noise
    }
    return Optional.empty();
  }

  public long addIndicator(Indicator<?> indicator, String name) {

    Preconditions.checkNotNull(indicator);
    addDoubleColumn(name);
    DuckBarSeries bs = (DuckBarSeries) indicator.getBarSeries();

    AtomicLong count = new AtomicLong();
    Bars.itemList(bs)
        .forEach(
            t -> {

              //  The bar index (t.index()) and the DuckDb rowId (bar.getId())
              //  have nothing to do with each other, even though they may be the same in
              //  many trivial cases.
              Num num = getNum(indicator, t.index()).orElse(null);

              ImmutableBar bar = (ImmutableBar) t.get();

              long rowId = bar.getId().get();
              updateRow(rowId, name, num);
              count.incrementAndGet();
            });

    return count.get();
  }

  public DuckTable getTable() {
    return this.table;
  }

  public DuckDb getDb() {
    return this.getTable().getDb();
  }

  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("table", getTableName())
        .add("db", getDb())
        .toString();
  }

  public boolean tableExists() {
    return getTable().exists();
  }
}
