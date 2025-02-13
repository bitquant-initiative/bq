package bq.loader;

import bq.duckdb.DuckDb;
import com.google.common.flogger.FluentLogger;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

public class BreadthCalculatorTest {

  FluentLogger logger = FluentLogger.forEnclosingClass();

  @Test
  public void testIt2() {

    new BreadthCalculator()
        .db(DuckDb.getSharedInMemory())
        .createTable()
        .forNYSE()
        .from(LocalDate.now().minusDays(10))
        .to(LocalDate.now())
        .fetchData()
        .updateADLine();

    DuckDb.getSharedInMemory()
        .template()
        .log()
        .query("select * from breadth order by date", "breadth");
  }
}
