package bq.strategy;

import bq.ducktape.BarSeriesTable;
import bq.ducktape.chart.Chart;
import java.io.File;
import java.time.DayOfWeek;
import org.junit.jupiter.api.Test;

public class BasicPortfolioTest extends BaseTest {

  @Test
  public void testIt() {

    System.out.println(tape);
  }

  @Test
  public void testX() {

    StrategyEngine engine = StrategyEngine.create(tape);

    Portfolio p = new Portfolio().cash(10000).assetSymbol("X:BTC");

    BarSeriesTable t = tape.importTable("btc", new File("./btc.csv"));

    t.getDb().template().execute(c -> c.sql("delete from btc where date<'2018-01-01'"));

    t.addIndicator("btc_power_law_quantile() as q");

    TradingStrategy s =
        new TradingStrategy() {

          int allocation = 0;

          @Override
          public void evaluate(Portfolio position) {

            if (position.getDouble("q").isPresent()) {
              double q = position.getDouble("q").orElse(0d);

              if (q > 80) {
                allocation = 50;
              } else if (q > 70) {
                //     allocation=90;
              } else if (q < 20) {
                allocation = 100;
              }
            }

            if (position.date.getDayOfWeek() == DayOfWeek.MONDAY) {
              position.rebalance(allocation);
            }
          }
        };

    engine.portfolio(p).inputTable(t).strategy(s);

    engine.execute();

    //   System.out.println(engine.inputTable.getTable().toPrettyString());

    System.out.println(p.getPortfolioValue() / p.getPortfolioInitialValue());

    Chart.newChart()
        .trace(
            "val",
            trace -> {
              trace.addDateSeries(db, sb -> sb.sql("select date,portfolio_value from btc"));
            })
        .view();
  }
}
