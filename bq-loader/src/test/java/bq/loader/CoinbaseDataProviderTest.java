package bq.loader;

import bq.loader.coinbase.CoinbaseDataProvider;
import org.junit.jupiter.api.Test;

public class CoinbaseDataProviderTest extends LoaderTest {

  @Test
  public void testIt() {
    var cb = new CoinbaseDataProvider(getDb());

    cb.symbol("X:BTC")
        .loadAll()
        .getBarData()
        .forEach(
            it -> {
              // System.out.println(it);
            });
    /*
    cb.symbol("X:BTC").getBars(LocalDate.now().minusDays(5), null).forEach(it->{
      System.out.println(it);
    });*/
  }
}
