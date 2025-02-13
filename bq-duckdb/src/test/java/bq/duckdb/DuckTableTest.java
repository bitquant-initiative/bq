package bq.duckdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class DuckTableTest extends BaseTest {

  @Test
  public void testIt() {
    var db = getDb();

    db.table("cities").importCsv("./us-cities.csv");

    assertThat(db.table("cities").rowCount()).isEqualTo(100);

    db.table("cities").deleteWhere("rank_2020>10");

    assertThat(db.table("cities").rowCount()).isEqualTo(10);
  }
}
