package bq.indicator.btc;

import bq.ducktape.BarSeriesTable;
import bq.ducktape.chart.Chart;
import bq.indicator.IndicatorTest;
import org.junit.jupiter.api.Test;

public class BtcPiMultipleIndicatorTest extends IndicatorTest {

  @Test
  public void testDivZero() {
    double a = 23;
    double b = 0d;

    System.out.println(a / b);
  }

  @Test
  public void testIt() {

    BarSeriesTable t = loadBtcTable();

    t.addIndicator("btc_pi_multiple() as pi");

    Chart.newChart()
        .trace(
            "pi",
            trace -> {
              trace.addData(t, "pi");
              trace.yAxis(y -> {});
            })
        .trace(
            "btc",
            trace -> {
              trace.addData(t, "close");
              trace.newYAxis(
                  y -> {
                    y.logScale();
                  });
            })
        .view();
  }
}
