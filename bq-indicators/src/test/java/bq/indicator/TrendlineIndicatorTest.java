package bq.indicator;

import bq.ducktape.BarSeriesTable;
import bq.ducktape.chart.Chart;
import bq.util.ta4j.Bars;
import java.time.LocalDate;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

public class TrendlineIndicatorTest extends IndicatorTest {

  Optional<Bar> findBar(BarSeries bs, LocalDate dt) {
    return Bars.toStream(bs).filter(b -> !b.getBeginTime().toLocalDate().isBefore(dt)).findFirst();
  }

  @Test
  public void testIt() {

    BarSeriesTable wgmi = loadWGMI();

    wgmi.getDb().template().execute("delete from wgmi where date<'2022-06-10'");
    wgmi.reload();

    String t0 = "2022-12-29"; // point 0 on lower channel band
    String t1 = "2024-09-06"; // point 1 on lower channel band
    String t2 = "2023-07-13"; //

    BarSeries bs = wgmi.getBarSeries();

    ChannelIndicator lower = new ChannelIndicator(bs, "lower", t0, t1, t2);
    ChannelIndicator q0 = new ChannelIndicator(bs, "0", t0, t1, t2);

    ChannelIndicator upper = new ChannelIndicator(bs, "upper", t0, t1, t2);
    ChannelIndicator q100 = new ChannelIndicator(bs, "100", t0, t1, t2);

    ChannelIndicator middle = new ChannelIndicator(bs, "middle", t0, t1, t2);
    ChannelIndicator q50 = new ChannelIndicator(bs, "50", t0, t1, t2);

    for (int i = bs.getBeginIndex(); i <= bs.getEndIndex(); i++) {

      Assertions.assertThat(lower.getValue(i)).isEqualTo(q0.getValue(i));
      Assertions.assertThat(upper.getValue(i)).isEqualTo(q100.getValue(i));
      Assertions.assertThat(middle.getValue(i)).isEqualTo(q50.getValue(i));
    }

    wgmi.addIndicator(lower, "lower");
    wgmi.addIndicator(middle, "middle");
    wgmi.addIndicator(upper, "upper");
    wgmi.addIndicator(
        new ChannelIndicator(wgmi.getBarSeries(), "quantile", t0, t1, t2), "quantile");

    Chart.newChart()
        .trace(
            "wgmi",
            trace -> {
              trace.addData(wgmi, "close");
              trace.lineColor("blue");
              trace.lineWidth(.5);
            })
        .trace(
            "lower",
            trace -> {
              trace.addData(wgmi, "lower");
              trace.lineColor("green");
            })
        .trace(
            "upper",
            trace -> {
              trace.addData(wgmi, "upper");
              trace.lineColor("red");
            })
        .trace(
            "middle",
            trace -> {
              trace.addData(wgmi, "middle");
              trace.lineColor("gray");
            })
        .trace(
            "quantile",
            trace -> {
              trace.newYAxis(
                  y -> {
                    y.overlaying("y");
                    y.side("right");
                  });
              trace.addData(wgmi, "quantile");
              trace.lineStyle("dotted");
              trace.lineColor("gray");
              trace.lineWidth(.5);
            })
        .title("WGMI")
        .view();
  }

  @Test
  public void testX() {}
}
