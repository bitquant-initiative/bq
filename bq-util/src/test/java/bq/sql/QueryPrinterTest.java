package bq.sql;

import bq.util.BqTest;
import org.junit.jupiter.api.Test;

public class QueryPrinterTest extends BqTest {

  @Test
  public void testIt() {

    var template = newTemplate();

    template.execute("create table btc as select * from '../data/BTC.csv'");

    template
        .print()
        .query("select null as foo,rowid,'fizz' as buzz,* from btc where date <='2016-2-23'");
  }
}
