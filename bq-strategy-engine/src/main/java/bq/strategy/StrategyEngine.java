package bq.strategy;

import bq.duckdb.DuckDb;
import bq.ducktape.BarSeriesTable;
import bq.ducktape.DuckTape;
import bq.sql.SqlTemplate;
import bq.sql.mapper.Mappers;
import bq.util.Dates;
import bq.util.ta4j.ImmutableBar;
import com.google.common.base.Preconditions;
import com.google.common.flogger.FluentLogger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.ta4j.core.Bar;

public class StrategyEngine {

  static FluentLogger logger = FluentLogger.forEnclosingClass();

  DuckTape tape;
  BarSeriesTable inputTable;
  Portfolio portfolio;
  TradingStrategy strategy;

  public static StrategyEngine create(DuckTape tape) {

    StrategyEngine engine = new StrategyEngine();
    engine.tape = tape;
    return engine;
  }

  public DuckDb getDb() {
    return tape.getDb();
  }

  public void addColumn(String columnSpec) {
    String sql =
        String.format(
            "alter table %s add column if not exists %s", inputTable.getTableName(), columnSpec);
    logger.atFiner().log("%s", sql);
    getDb().template().execute(sql);
  }

  public void alterInputTable() {
    addColumn("cash double");
    addColumn("portfolio_value double");
    addColumn("portfolio_initial_value double");
    addColumn("asset_symbol varchar(20)");
    addColumn("asset_qty double");
    addColumn("asset_price double");
    addColumn("asset_cost_basis double");
    addColumn("txn_qty double");
    addColumn("txn_price double");
  }

  public void writeRecord() {
    String sql =
        """
        update {{table}} set
        cash={{cash}},
        portfolio_value={{portfolio_value}},
        portfolio_initial_value={{portfolio_initial_value}},
        asset_symbol = {{asset_symbol}},
        asset_qty = {{asset_qty}},
        asset_price = {{asset_price}},
        asset_cost_basis = {{asset_cost_basis}},
        txn_qty = {{txn_qty}},
        txn_price = {{txn_price}}

        where date = {{date}}
        """;

    sql = sql.replace("{{table}}", inputTable.getTableName());

    SqlTemplate t = inputTable.getDb().template();

    String fsql = sql;

    t.update(
        c -> {
          c.sql(fsql);
          c.bind("date", portfolio.getDate());
          c.bind(
              "cash",
              new BigDecimal(portfolio.getCash()).setScale(2, RoundingMode.HALF_UP).doubleValue());
          c.bind(
              "portfolio_value",
              new BigDecimal(portfolio.getPortfolioValue())
                  .setScale(2, RoundingMode.HALF_UP)
                  .doubleValue());
          c.bind(
              "portfolio_initial_value",
              new BigDecimal(portfolio.getPortfolioInitialValue())
                  .setScale(2, RoundingMode.HALF_UP)
                  .doubleValue());
          c.bind("asset_symbol", portfolio.getAssetSymbol());
          c.bind(
              "asset_qty",
              new BigDecimal(portfolio.getAssetQty())
                  .setScale(8, RoundingMode.HALF_UP)
                  .doubleValue());
          c.bind("asset_price", portfolio.getAssetPrice());
          c.bind("asset_cost_basis", portfolio.getAssetCostBasis());
          c.bind("txn_qty", portfolio.txnQty);
          c.bind("txn_price", portfolio.txnPrice);
        });
  }

  public StrategyEngine inputTable(BarSeriesTable inputTable) {
    this.inputTable = inputTable;
    return this;
  }

  public StrategyEngine portfolio(Portfolio p) {
    this.portfolio = p;
    this.portfolio.engine = this;
    return this;
  }

  public StrategyEngine strategy(TradingStrategy strategy) {
    this.strategy = strategy;
    return this;
  }

  public void execute() {
    Preconditions.checkState(inputTable != null, "inputTable must be set");
    Preconditions.checkState(this.strategy != null, "strategy must be set");
    AtomicInteger count = new AtomicInteger();

    alterInputTable();

    String sql =
        String.format("select rowid,* from %s order by date asc", inputTable.getTableName());
    getDb()
        .template()
        .query(
            c -> {
              c.sql(sql);
            },
            Mappers.jsonObjectMapper())
        .forEach(
            n -> {
              LocalDate d = Dates.asLocalDate(n.path("date").asText()).get();
              Bar bar =
                  ImmutableBar.create(
                      d,
                      n.path("open").asDouble(0),
                      n.path("high").asDouble(0),
                      n.path("low").asDouble(0),
                      n.path("close").asDouble(0),
                      n.path("volume").asDouble(0),
                      n.path("rowid").asLong());
              portfolio.txnPrice = 0;
              portfolio.txnQty = 0;
              portfolio.next(bar, n);
              if (count.get() > 0) {
                strategy.evaluate(portfolio);
              }
              writeRecord();
              count.incrementAndGet();
            });
  }
}
