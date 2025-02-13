package bq.ducktape;

import bq.duckdb.DuckDb;
import bq.util.ta4j.ImmutableBar;
import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

public class DuckBarTest extends BaseTest {

  @Test
  public void testIt() {

    Bar bar = ImmutableBar.create(LocalDate.now(), 100d, 105.2, 99.24, 103.2, 3784574.3, 12l);

    System.out.println(bar);
  }

  @Test
  void testAddColumn() throws SQLException {
    var c = DriverManager.getConnection("jdbc:duckdb:./data");

    var st = c.createStatement();

    var f = new File("./src/test/resources/data/btc.csv");

    try {
      st.execute("drop table test");
    } catch (Exception e) {
      // e.printStackTrace();
    }
    st.close();
    st = c.createStatement();
    st.execute(
        String.format("create table test as select * from read_csv('%s')", f.getAbsolutePath()));

    st.close();

    DuckTape duckTape = DuckTape.create(DuckDb.create(c));

    BarSeriesTable t = duckTape.getTable("test");

    t.addIndicator(new ClosePriceIndicator(t.getBarSeries()), "close_price");

    Indicator<Num> openIndicator = t.getIndicator("open");

    for (int i = t.getBarSeries().getBeginIndex(); i < t.getBarSeries().getEndIndex(); i++) {

      Assertions.assertThat(t.getBarSeries().getBar(i).getOpenPrice())
          .isEqualTo(openIndicator.getValue(i));
    }

    DuckDb d = DuckDb.create(c);

    c.close();
  }
}
