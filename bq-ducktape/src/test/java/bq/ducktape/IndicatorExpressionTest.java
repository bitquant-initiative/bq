package bq.ducktape;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class IndicatorExpressionTest {

  @Test
  public void testIt() {
    IndicatorExpression x = IndicatorExpression.parse(" sma(  12 ) as foo  ");

    Assertions.assertThat(x.getArgs()).containsExactly("12");
    Assertions.assertThat(x.getOutputName().orElse(null)).isEqualTo("foo");
  }
}
