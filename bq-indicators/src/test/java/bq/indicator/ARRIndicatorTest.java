package bq.indicator;

import bq.ducktape.BarSeriesTable;
import bq.ducktape.chart.Chart;
import com.google.common.flogger.FluentLogger;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.MethodName.class)
public class ARRIndicatorTest extends IndicatorTest {

  static FluentLogger logger = FluentLogger.forEnclosingClass();

  @Test
  public void aaaFirst() {}

  void checkResult(LocalDate t0, double p0, LocalDate t1, double arr, double p1) {
    double years = Math.abs(ChronoUnit.DAYS.between(t0, t1)) / 365d;

    double r = (arr / 100) + 1d;

    double out = Math.pow(r, years) * p0;

    Assertions.assertThat(p1).isCloseTo(out, Percentage.withPercentage(.1));
  }

  @Test
  public void testX() {

    ARRIndicator ind = new ARRIndicator(null);

    LocalDate t0 = LocalDate.now().minusYears(4);
    double p0 = 100;
    LocalDate t1 = LocalDate.now();
    double p1 = 80;

    double arr = ARRIndicator.calculateARR(t0, p0, t1, p1);

    checkResult(t0, p0, t1, arr, p1);
  }

  @Test
  public void applyIndicator() {
    BarSeriesTable t = loadBtcTable();

    tape.getDb().template().execute("delete from btc where date<'2016-01-01'");

    BarSeriesTable t2 = tape.getTable("btc");

    ARRIndicator indicator = new ARRIndicator(t.getBarSeries(), 4);

    t2.addIndicator(indicator, "arr");

    t2.addIndicator("sma(arr,100)", "sma");

    Chart.newChart()
        .trace(
            "btc",
            trace -> {
              trace.addData(t2, "sma");
            })
        .title("BTC Annualized Rate of Return (ARR)")
        .view();
  }

  @Test
  public void testPositive() {

    ARRIndicator ind = new ARRIndicator(null);

    LocalDate t0 = LocalDate.now().minusYears(20);
    double p0 = 100;
    LocalDate t1 = LocalDate.now();
    double p1 = 100;

    double arr = ARRIndicator.calculateARR(t0, p0, t1, p1);

    checkResult(t0, p0, t1, arr, p1);
  }
}
