package bq.indicator.experimental;

import bq.duckdb.DuckDb;
import bq.util.ta4j.Bars;
import bq.util.ta4j.ImmutableBar;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

public class NyseADIndicatorTest {

  @Test
  public void testIt() {

    DuckDb db = DuckDb.getSharedInMemory();
    LocalDate d = LocalDate.of(2025, 2, 7);
    Bar b = ImmutableBar.create(d, 0.0, 0.0, 0.0, 0.0, 0.0);

    BarSeries bs = Bars.toBarSeries(List.of(b), null);

    NyseADIndicator x = new NyseADIndicator(bs);

    System.out.println(x.getValue(0));
  }
}
