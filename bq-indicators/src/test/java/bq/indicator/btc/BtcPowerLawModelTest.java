package bq.indicator.btc;

import static org.assertj.core.api.Assertions.byLessThan;

import bq.ducktape.BarSeriesTable;
import bq.ducktape.DuckTape;
import bq.ducktape.chart.Chart;
import bq.indicator.IndicatorTest;
import bq.indicator.btc.BtcPowerLawModel.QuantileModel;
import com.google.common.flogger.FluentLogger;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class BtcPowerLawModelTest extends IndicatorTest {

  FluentLogger logger = FluentLogger.forEnclosingClass();

  File modelOutputFile = new File("./btc-power-law-model.json");
  File btcPriceData = new File("./btc-price-data.csv");

  @Test
  public void test() {

    double a = 5.9;
    double c = -17.325;
    int d = BtcUtil.getDaysSinceGenesis(LocalDate.now());

    // System.out.println(BtcPowerLaw.calc(d,a,c));

  }

  @Test
  public void rebuildModel() throws IOException {
    DuckTape tape = DuckTape.createInMemory();

    BarSeriesTable t = tape.importTable("test", btcPriceData);

    tape.getDb().template().execute("delete from test where date < '2013-01-01'");

    BtcPowerLawModel power = BtcPowerLawModel.create();

    QuantileModel m = BtcPowerLawCalculator.generateQuantileModel(tape.getBarSeries("test"), 5.64);

    Files.asCharSink(modelOutputFile, StandardCharsets.UTF_8)
        .write(power.getModel().toJson().toPrettyString());
  }

  @Test
  public void testDefault() throws IOException {

    BtcPowerLawModel power = BtcPowerLawModel.create();

    LocalDate d = LocalDate.of(2025, 1, 28);
  }

  @Test
  @Disabled
  public void testIt() {
    DuckTape tape = DuckTape.createInMemory();

    BarSeriesTable t = tape.importTable("test", btcPriceData);

    tape.getDb().template().execute("delete from test where date < '2013-01-01'");

    t = tape.getTable("test");

    QuantileModel m = BtcPowerLawCalculator.generateQuantileModel(t.getBarSeries(), 5.65);

    BtcPowerLawModel power = BtcPowerLawModel.create(m);

    double d = power.getPrice(LocalDate.now(), 50);

    System.out.println(power.getQuantile(LocalDate.now(), 104000));

    System.out.println(d);
  }

  @Test
  public void testIt4() {

    BtcPowerLawModel x = BtcPowerLawModel.create();

    LocalDate d0 = LocalDate.of(2024, 1, 1);
    LocalDate d1 = LocalDate.of(2025, 1, 1);

    x.getPrice(d0);

    double v0 = x.getPrice(d0, 50);
    LocalDate d0a = x.getDate(v0, 50);
    System.out.println(d0 + " " + v0 + " " + d0a);

    double v1 = x.getPrice(d1, 50);
    LocalDate d1a = x.getDate(v1 + 10000, 50);
    System.out.println(d1 + " " + v1 + " " + d1a);
  }

  void checkThrown(Class<? extends Throwable> expected, Runnable r) {
    try {
      r.run();
      Assertions.failBecauseExceptionWasNotThrown(expected);
    } catch (AssertionError e) {
      throw e;
    } catch (Throwable t) {
      Assertions.assertThat(t.getClass()).isAssignableTo(expected);
    }
  }

  @Test
  public void testQuantileModelRequirement() {

    // Create a poewr law model that does not include a quantile model
    BtcPowerLawModel x = BtcPowerLawModel.create(5.61, 16.21);

    checkThrown(
        IllegalStateException.class,
        () -> {
          x.getModel();
        });

    Assertions.assertThat(x.a()).isEqualByComparingTo(5.61);
    Assertions.assertThat(x.c()).isEqualByComparingTo(16.21);

    x.getDate(500000); // ok
    x.getPrice(LocalDate.now());
    x.getDaysToPrice(50000);
    x.getDaysToPrice(50000, LocalDate.now());

    checkThrown(
        IllegalStateException.class,
        () -> {
          x.getDate(50000, 20);
        });
    checkThrown(
        IllegalStateException.class,
        () -> {
          x.quantile(40);
        });

    checkThrown(
        IllegalStateException.class,
        () -> {
          x.getModel();
        });

    checkThrown(
        IllegalStateException.class,
        () -> {
          x.getQuantile(LocalDate.now(), 100000);
        });

    checkThrown(
        IllegalStateException.class,
        () -> {
          x.getQuantile(LocalDate.now(), 100000);
        });

    checkThrown(
        IllegalStateException.class,
        () -> {
          x.getPrice(LocalDate.now(), 40);
        });

    checkThrown(
        IllegalStateException.class,
        () -> {
          x.getDaysToPrice(50000, LocalDate.now(), 30);
        });

    checkThrown(
        IllegalStateException.class,
        () -> {
          x.getDaysToPrice(50000, 30);
        });
  }

  @Test
  public void checkDaysTo() {
    BtcPowerLawModel x = BtcPowerLawModel.create().quantile(80);

    LocalDate d = x.getDate(200000);

    int count = x.getDaysToPrice(200000);

    Assertions.assertThat(LocalDate.now().plusDays(count))
        .isCloseTo(d, byLessThan(2, ChronoUnit.DAYS));
  }

  @Test
  public void testDaysToPrice() {

    BtcPowerLawModel x = BtcPowerLawModel.create();

    System.out.println(x.getDate(80000));

    System.out.println(x.getDaysToPrice(80000));
  }

  @Test
  public void testIt41() {

    LocalDate d = LocalDate.of(2025, 1, 1);
    BtcPowerLawModel x = BtcPowerLawModel.create();

    double a = x.a();
    double c = x.c();

    System.out.println(a);
    System.out.println(c);
    /*
     * Assertions.assertThat(x.getQuantile()).isEqualTo(50); x.quantile(25);
     * Assertions.assertThat(x.getQuantile()).isEqualTo(25);
     *
     *
     * Assertions.assertThat(x.getModelPrice(d)).isEqualTo(x.getModelPrice(d,25));
     *
     *
     *
     *
     * System.out.println(x.a()); System.out.println(x.c());
     *
     *
     * x.quantile(90); System.out.println(x.a()); System.out.println(x.c());
     */
    // Assertions.assertThat(x.calculateDate(5000, 0, 0))

  }

  @Test
  public void testRoundTrip() {
    BtcPowerLawModel m = BtcPowerLawModel.create();
    LocalDate d = LocalDate.now();
    for (int q = 0; q < 100; q++) {
      m.quantile(q);
      Assertions.assertThat(m.getQuantile()).isEqualTo(q);

      double price = m.getPrice(d);

      Assertions.assertThat(m.getQuantile(d, price)).isEqualTo(q);
    }
  }

  @Test
  public void testChart() {
    BarSeriesTable t = loadBtcTable();

    t.addIndicator("btc_power_law_price(10) as q10");
    t.addIndicator("btc_power_law_price(50) as q50");
    t.addIndicator("btc_power_law_price(75) as q75");
    t.addIndicator("btc_power_law_price(95) as q95");
    t.addIndicator("btc_pi_multiple() as pi");
    Chart.newChart()
        .trace(
            "btc",
            trace -> {
              trace.addData(t, "close");

              trace.yAxis(
                  y -> {
                    y.logScale();
                  });
            })
        .trace(
            "q50",
            trace -> {
              trace.addData(t, "q50");
            })
        .trace(
            "q75",
            trace -> {
              trace.addData(t, "q75");
            })
        .trace(
            "q95",
            trace -> {
              trace.addData(t, "q95");
            })
        .trace(
            "q10",
            trace -> {
              trace.addData(t, "q10");
            })
        .trace(
            "pi",
            trace -> {
              trace.addData(t, "pi");
              trace.newYAxis(
                  y -> {
                    y.linearScale();
                  });
            })
        .view();
  }
}
