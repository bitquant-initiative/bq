package bq.indicator.btc;

import bq.ducktape.BarSeriesTable;
import bq.ducktape.IndicatorRegistry;
import bq.ducktape.chart.Chart;
import bq.indicator.IndicatorTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class BtcMVRVZIndicatorTest extends IndicatorTest {

  @Test
  public void testB() {

    BarSeriesTable t = loadBtcTable();

    t.addIndicator("btc_mvrvz() as foo");
    t.addIndicator("sma(foo,100) as bar");

    Chart.newChart()
        .trace(
            "mvrv",
            trace -> {
              trace.addData(t, "foo");
              trace.yAxis(
                  y -> {
                    //   y.logScale();
                  });
            })
        .view();
  }

  @Test
  public void testA() {

    Assertions.assertThat(IndicatorRegistry.getRegistry().getAvailableIndicators().get("btc_mvrvz"))
        .isEqualTo(BtcMVRVZIndicator.class);
  }
}
