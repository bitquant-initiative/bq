package bq.ducktape;

import bq.sql.DbException;
import bq.sql.SqlCloser;
import bq.util.ta4j.ImmutableBar;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class DuckColumnIndicator implements Indicator<Num> {

  DuckBarSeries barSeries;
  String column;

  Map<Long, Num> cachedValues = null;

  DuckColumnIndicator(DuckBarSeries bs, String column) {
    this.barSeries = bs;
    this.column = BarSeriesTable.sanitizeColumn(column);
  }

  @Override
  public Num getValue(int index) {

    if (cachedValues == null) {
      fetchValues();
    }
    // 1. look up the bar in the driving BarSeries
    // 2. this will have the DuckDB rowId, which we'll use to get the data
    ImmutableBar bar = (ImmutableBar) barSeries.getBar(index);
    if (bar == null) {
      return null;
    }

    long rowId = bar.getId().get();
    Num num = cachedValues.get(rowId);
    return num;
  }

  private void fetchValues() {
    // IMPORTANT - The TA4J Indicator object model uses the bar index to get the indicator
    // value.
    // We need to load the appropriate column into memory and store the DuckDB rowid->Num
    // mappings.
    // This can then be used in a two-stage lookup operation in getValue().

    Map<Long, Num> rowIdMap = Maps.newHashMap();
    try (SqlCloser closer = SqlCloser.create()) {
      Connection c = barSeries.getTable().getDb().getConnection();
      closer.register(c);

      Statement st = c.createStatement();
      closer.register(st);
      String sql =
          String.format("select rowid, %s from %s", column, barSeries.table.getTableName());

      ResultSet rs = st.executeQuery(sql);
      closer.register(rs);
      while (rs.next()) {
        long row = rs.getLong("rowid");
        double val = rs.getDouble(2);
        if (!rs.wasNull()) {
          rowIdMap.put(row, DoubleNum.valueOf(val));
        }
      }
      this.cachedValues = rowIdMap;

    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  @Override
  public int getUnstableBars() {
    return 0;
  }

  @Override
  public BarSeries getBarSeries() {
    return barSeries;
  }

  public String toString() {
    return MoreObjects.toStringHelper("ColumnIndicator").add("column", this.column).toString();
  }
}
