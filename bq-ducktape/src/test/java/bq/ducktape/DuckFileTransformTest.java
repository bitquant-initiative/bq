package bq.ducktape;

import bq.duckdb.DuckFileTransform;
import org.junit.jupiter.api.Test;

public class DuckFileTransformTest {

  @Test
  public void testIt() {

    String url =
        "https://s3.us-west-2.amazonaws.com/data.bitquant.cloud/indicators/1d/NYSE.BREADTH.csv";

    DuckFileTransform.withSharedInMemory()
        .source(url)
        .transform(
            t -> {
              t.db().template().log().query(c -> c.sql("select * from " + t.table()));
              t.execute("create table another as ( select *  from " + t.getTable() + ")");

              t.deferDrop("another");
            })
        .execute();
  }
}
