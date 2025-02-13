package bq.sql;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class TablePrinterTest {

  @Test
  public void testIt() {
    Assertions.assertThat(TablePrinter.create().center("hello", 14)).isEqualTo("    hello     ");
    Assertions.assertThat(TablePrinter.create().center("the quick brown fox jumped", 14))
        .hasSize(14);
    Assertions.assertThat(TablePrinter.create().center(null, 14)).hasSize(14);
  }
}
