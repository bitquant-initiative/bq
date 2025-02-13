package bq.ducktape;

import com.google.common.collect.Sets;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class IndicatorRegistryTest {

  @Test
  public void testIt() {

    try {
      IndicatorRegistry.getRegistry().getAllIndicators().clear();
      Assertions.failBecauseExceptionWasNotThrown(UnsupportedOperationException.class);
    } catch (UnsupportedOperationException e) {

    }

    try {
      IndicatorRegistry.getRegistry().getAvailableIndicators().clear();
      Assertions.failBecauseExceptionWasNotThrown(UnsupportedOperationException.class);
    } catch (UnsupportedOperationException e) {

    }

    Set<String> excluded =
        Sets.difference(
            IndicatorRegistry.getRegistry().getAllIndicators().keySet(),
            IndicatorRegistry.getRegistry().getAvailableIndicators().keySet());

    excluded.forEach(
        it -> {
          System.out.println(it);
        });
  }
}
