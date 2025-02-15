package bq.ducktape;

import bq.duckdb.DuckDb;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.NaN;
import org.ta4j.core.num.Num;

public class DuckDateIndicator implements Indicator<Num> {

  BarSeries barSeries;
  DuckDb db;
  String tableName;
  String valueColumn;
  String dateColumn;

  Map<LocalDate, Optional<Num>> vals;

  public DuckDateIndicator(
      BarSeries bs, DuckDb db, String tableName, String dateColumn, String valueColumn) {
    super();

    this.barSeries = bs;
    this.db = db;
    this.tableName = tableName;
    this.valueColumn = valueColumn;
    this.dateColumn = dateColumn;

    Preconditions.checkNotNull(barSeries, "BarSeries");
  }

  @Override
  public Num getValue(int index) {

    Bar b = barSeries.getBar(index);
    if (b == null) {
      return NaN.NaN;
    }
    if (vals == null) {
      beforeSelect();
      select();
    }
    LocalDate d = b.getBeginTime().toLocalDate();

    Preconditions.checkNotNull(vals);
    Optional<Num> val = vals.get(d);
    if (val == null || val.isEmpty()) {
      return NaN.NaN;
    }
    return val.get();
  }

  public DuckDb getDb() {
    return this.db;
  }

  public void setTableName(String t) {
    this.tableName = t;
  }

  public String getDateColumn() {
    return this.dateColumn;
  }

  public String getTableName() {
    return this.tableName;
  }

  protected void beforeSelect() {}

  protected void select() {

    vals = Maps.newHashMap();
    String sql =
        String.format("select %s,%s from %s", dateColumn, valueColumn, tableName, dateColumn);

    db.template()
        .query(
            c -> c.sql(sql),
            rs -> {
              Optional<LocalDate> date = rs.getLocalDate(1);
              if (date.isPresent()) {
                Optional<Double> val = rs.getDouble(2);
                if (val.isEmpty()) {
                  vals.put(date.get(), Optional.empty());
                } else {
                  vals.put(date.get(), Optional.of(DoubleNum.valueOf(val.get())));
                }
              }
              return "";
            });
  }

  @Override
  public int getUnstableBars() {
    return 0;
  }

  @Override
  public BarSeries getBarSeries() {
    return barSeries;
  }
}
