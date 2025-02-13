package bq.indicator.btc;

import bq.ducktape.BarSeriesTable;
import bq.ducktape.chart.Chart;
import bq.indicator.IndicatorTest;
import org.junit.jupiter.api.Test;

public class BtcMayerMultipleIndicatorTest extends IndicatorTest {

  @Test
  public void testIt() {

    BarSeriesTable t = loadBtcTable();

    t.getDb().template().execute("delete from btc where date <'2014-12-01'");

    t.reload();

    t.addIndicator("btc_mayer_multiple() as mm");
    t.addIndicator("sma(350) as sma");

    Chart.newChart()
        .trace(
            "price",
            t2 -> {
              t2.lineWidth(.5);
              t2.addData(t, "close");
              t2.yAxis(
                  y -> {
                    y.logScale();
                  });
            })
        .trace(
            "sma",
            t2 -> {
              t2.lineWidth(.5);
              t2.addData(t, "sma");
            })
        .view();

    /*  .trace("mayer", trace -> {
      trace.lineColor("red");
      trace.lineWidth(1);
      trace.data(t, "mm");

    })*/

  }
}
