package bq.ducktape;

import bq.util.ta4j.ImmutableBarSeries;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.ta4j.core.Bar;

public class DuckBarSeries extends ImmutableBarSeries {

  private static final long serialVersionUID = 1L;

  BarSeriesTable table;

  DuckBarSeries(BarSeriesTable table, List<Bar> bars, String name) {

    super(bars, name);
    this.table = table;
  }

  static DuckBarSeries create(BarSeriesTable table, List<Bar> bars, String seriesName) {
    return new DuckBarSeries(table, bars, seriesName);
  }

  public String toString() {

    try {
      ToStringHelper h = MoreObjects.toStringHelper(this).add("name", getName());

      h.add("count", getBarCount());
      if (!this.isEmpty()) {

        String first = getFirstBar().getBeginTime().toString();
        String last = getFirstBar().getBeginTime().toString();
        if (getFirstBar()
            .getBeginTime()
            .equals(getFirstBar().getBeginTime().truncatedTo(ChronoUnit.DAYS))) {
          first = getFirstBar().getBeginTime().toLocalDate().toString();
        }
        if (getLastBar()
            .getEndTime()
            .equals(getLastBar().getEndTime().truncatedTo(ChronoUnit.DAYS))) {
          last = getLastBar().getEndTime().toLocalDate().toString();
        }
        h = h.add("first", first);
        h = h.add("last", last);
      }
      h.add("table", table.getTableName());
      return h.toString();

    } catch (Exception e) {
      return super.toString();
    }
  }

  public BarSeriesTable getTable() {

    return this.table;
  }
}
