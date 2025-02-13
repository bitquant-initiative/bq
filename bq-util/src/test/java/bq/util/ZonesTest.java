package bq.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class ZonesTest {

  @Test
  public void testIt() {

    Assertions.assertThat(Zones.UTC.toString()).isEqualTo("UTC");
    Assertions.assertThat(Zones.LAX.toString()).isEqualTo("America/Los_Angeles");
    Assertions.assertThat(Zones.NYC.toString()).isEqualTo("America/New_York");
  }
}
