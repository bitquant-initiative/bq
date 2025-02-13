package bq.duckdb;

import static org.assertj.core.api.Assertions.assertThat;

import bq.test.BqTest;
import org.junit.jupiter.api.Test;

public class RecipesTest extends BqTest {

  @Test
  public void testCreateSharedInMemory() {

    var db = DuckDb.getSharedInMemory();

    var db2 = DuckDb.getSharedInMemory();

    assertThat(db).isSameAs(db2);

    assertThat(db.getConnection()).isSameAs(db.getConnection());
  }

  @Test
  public void testCities() {
    var db = DuckDb.getSharedInMemory();

    var table = db.table("cities");

    table.importCsv("./us-cities.csv");

    table.retainColumns("rank_2020", "city", "STATE");

    table.renameColumn("rank_2020", "rank");

    table.prettyPrint();

    table.drop();
  }

  @Test
  public void testSimple() {

    var db = DuckDb.getSharedInMemory();

    db.template()
        .execute(
            """
            create table person (
              name varchar(50) primary key
              )
            """);

    var table = db.table("person");

    db.template().update("insert into person (name) values ({{name}})", "Jane");
    db.template().update("insert into person (name) values ({{name}})", "Dick");

    assertThat(table.rowCount()).isEqualTo(2);

    table.prettyPrint();

    table.addColumn("age int");

    table.prettyPrint();

    db.template().update("update person set age={{age}} where name={{name}}", 30, "Jane");
    db.template().update("update person set age={{age}} where name={{name}}", 29, "Dick");

    table.prettyPrint();

    db.template()
        .update(
            c -> {
              c.sql("update person set age={{age}} where name={{name}}");
              c.bind("name", "Jane");
              c.bind("age", 31);
            });

    table.prettyPrint();

    table.deleteWhere("age<30");

    table.prettyPrint();
  }
}
