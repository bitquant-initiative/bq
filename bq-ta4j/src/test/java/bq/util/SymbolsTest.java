package bq.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class SymbolsTest {

  @Test
  public void testIt() {

    Assertions.assertThat(Symbol.parse("S:MSTR").getTicker()).isEqualTo("MSTR");
    Assertions.assertThat(Symbol.parse("S:MSTR").getQualifier().get()).isEqualTo("S");
    Assertions.assertThat(Symbol.parse("MSTR").getTicker()).isEqualTo("MSTR");
    Assertions.assertThat(Symbol.parse("MSTR").getQualifier()).isEmpty();

    Assertions.assertThat(Symbol.parse(" S:MSTR ").getTicker()).isEqualTo("MSTR");
    Assertions.assertThat(Symbol.parse(" S:MSTR ").getQualifier().get()).isEqualTo("S");
    Assertions.assertThat(Symbol.parse(" MSTR ").getTicker()).isEqualTo("MSTR");
    Assertions.assertThat(Symbol.parse(" MSTR ").getQualifier()).isEmpty();

    checkException(":MSTR");
    checkException("S:");
    checkException("");
    checkException(null);

    Assertions.assertThat(Symbol.parse("X:BTC").getPathName()).isEqualTo("X_BTC");

    Assertions.assertThat(Symbol.parseTableName("X_BTC").getQualifier().get()).isEqualTo("X");
    Assertions.assertThat(Symbol.parseTableName("BTC").toString()).isEqualTo("BTC");
    Assertions.assertThat(Symbol.parseTableName("x_btc").toString()).isEqualTo("X:BTC");
    Assertions.assertThat(Symbol.parseTableName("Q_BTC_MVRVZ").toString()).isEqualTo("Q:BTC.MVRVZ");
    Assertions.assertThat(Symbol.parseTableName("Q_BTC_MvrvZ.csv").toString())
        .isEqualTo("Q:BTC.MVRVZ");
  }

  void checkException(String input) {
    try {
      Symbol.parse(input);
      Assertions.failBecauseExceptionWasNotThrown(BqException.class);
    } catch (IllegalArgumentException e) {
      // ok
    }
  }
}
