package bq.indicator.btc;

import bq.ducktape.BarSeriesTable;
import bq.ducktape.chart.Chart;
import bq.indicator.IndicatorTest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.num.DoubleNum;

public class BtcGenesisIndicatorTest extends IndicatorTest {

  BarSeries singleBar(LocalDate d) {
    BaseBarSeriesBuilder.setDefaultNum(DoubleNum.ZERO);

    Bar b =
        BaseBar.builder(DoubleNum.ZERO, double.class)
            .endTime(d.plusDays(1).atStartOfDay(ZoneId.of("UTC")))
            .timePeriod(Duration.ofDays(1))
            .build();

    BarSeries bs = new BaseBarSeriesBuilder().withBars(List.of(b)).build();

    return bs;
  }

  BarSeries singleBar(int y, int m, int d) {

    return singleBar(LocalDate.of(y, m, d));
  }

  @Test
  public void testDaysSinceGenesis() {

    Assertions.assertThat(
            new BtcDaysSinceGenesisIndicator(singleBar(2009, 1, 2)).getValue(0).intValue())
        .isEqualTo(-1);
    Assertions.assertThat(
            new BtcDaysSinceGenesisIndicator(singleBar(2009, 1, 3)).getValue(0).intValue())
        .isEqualTo(0);
    Assertions.assertThat(
            new BtcDaysSinceGenesisIndicator(singleBar(2009, 1, 4)).getValue(0).intValue())
        .isEqualTo(1);
    Assertions.assertThat(
            new BtcDaysSinceGenesisIndicator(singleBar(2010, 1, 1)).getValue(0).intValue())
        .isEqualTo(363);
    Assertions.assertThat(
            new BtcDaysSinceGenesisIndicator(singleBar(2025, 1, 31)).getValue(0).intValue())
        .isEqualTo(5872);

    for (int i = -1000; i < 5000; i++) {
      LocalDate d = BtcUtil.getGenesisDate().plusDays(i);
      Assertions.assertThat(new BtcDaysSinceGenesisIndicator(singleBar(d)).getValue(0).intValue())
          .isEqualTo(BtcUtil.getDaysSinceGenesis(d));
    }

    Assertions.assertThat(
            new BtcDaysSinceGenesisIndicator(singleBar(2009, 1, 3)).getValue(0).intValue())
        .isEqualTo(0);
    Assertions.assertThat(
            new BtcDaysSinceGenesisIndicator(singleBar(2009, 1, 4)).getValue(0).intValue())
        .isEqualTo(1);
  }

  @Test
  public void testEpochDay() {

    for (int i = -1000; i < 5000; i++) {
      LocalDate d = BtcUtil.getGenesisDate().plusDays(i);
      Assertions.assertThat(new BtcDayOfEpochIndicator(singleBar(d)).getValue(0).intValue())
          .isEqualTo(BtcUtil.getDayOfEpoch(d));
    }
  }

  @Test
  public void testEpochIndicator() {

    for (int i = -1000; i < 5000; i++) {
      LocalDate d = BtcUtil.getGenesisDate().plusDays(i);
      Assertions.assertThat(new BtcEpochIndicator(singleBar(d)).getValue(0).intValue())
          .isEqualTo(BtcUtil.getEpoch(d));
    }
  }

  @Test
  public void testX() {
    BarSeriesTable btc = loadBtcTable();

    btc.addIndicator(new BtcDaysSinceGenesisIndicator(btc.getBarSeries()), "genesis");

    Chart.newChart()
        .trace(
            "btc",
            trace -> {
              trace.addData(btc, "genesis");
            })
        .view();
  }

  @Test
  public void testY() {
    BarSeriesTable btc = loadBtcTable();

    btc.addIndicator(new BtcPowerLawPriceIndicator(btc.getBarSeries()), "genesis");

    Chart.newChart()
        .trace(
            "btc",
            trace -> {
              trace.addData(btc, "genesis");
              trace.yAxis(
                  y -> {
                    y.logScale();
                  });
            })
        .view();
  }
}
