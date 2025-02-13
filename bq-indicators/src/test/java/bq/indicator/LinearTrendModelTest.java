package bq.indicator;

import java.time.LocalDate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class LinearTrendModelTest {

  @Test
  public void testIt() {

    LocalDate t0 = LocalDate.of(2024, 6, 28);
    LinearTrendModel model =
        LinearTrendModel.from(LocalDate.of(2024, 6, 28), 100, LocalDate.of(2025, 2, 03), 200);

    Assertions.assertThat(model.getModelPrice(LocalDate.of(2024, 6, 28)))
        .isEqualByComparingTo(100.0);
    Assertions.assertThat(model.getModelPrice(LocalDate.of(2025, 2, 3)))
        .isEqualByComparingTo(200.0);
    Assertions.assertThat(model.getModelPrice(LocalDate.of(2024, 10, 16)))
        .isEqualByComparingTo(150.0);

    // NOTE: there is no guarantee that dates will round correctly back to the
    // original values. But these do.
    Assertions.assertThat(model.getModelDate(100)).isEqualTo("2024-06-28");
    Assertions.assertThat(model.getModelDate(150)).isEqualTo("2024-10-16");
    Assertions.assertThat(model.getModelDate(200)).isEqualTo("2025-02-03");

    try {
      model.getUpperChannelPrice(LocalDate.now());
      Assertions.failBecauseExceptionWasNotThrown(IllegalStateException.class);
    } catch (IllegalStateException e) {
      // ignore

    }

    try {
      model.getLowerChannelPrice(LocalDate.now());
      Assertions.failBecauseExceptionWasNotThrown(IllegalStateException.class);
    } catch (IllegalStateException e) {
      // ignore
    }

    try {
      model.getMiddleChannelPrice(LocalDate.now());
      Assertions.failBecauseExceptionWasNotThrown(IllegalStateException.class);
    } catch (IllegalStateException e) {
      // ignore
    }

    try {
      model.getQuantile(LocalDate.now());
      Assertions.failBecauseExceptionWasNotThrown(IllegalStateException.class);
    } catch (IllegalStateException e) {
      // ignore
    }

    try {
      model.getActualPrice(LocalDate.now());
      Assertions.failBecauseExceptionWasNotThrown(IllegalStateException.class);
    } catch (IllegalStateException e) {
      // ignore
    }

    try {
      model.channel(LocalDate.of(2024, 6, 28));
      Assertions.failBecauseExceptionWasNotThrown(IllegalStateException.class);
    } catch (IllegalStateException e) {
    }

    model.channel(LocalDate.of(2024, 6, 28), 120);

    Assertions.assertThat(model.getLowerChannelPrice(t0)).isEqualTo(model.getModelPrice(t0));
    Assertions.assertThat(model.getUpperChannelPrice(t0)).isEqualTo(120);
    Assertions.assertThat(model.getMiddleChannelPrice(t0)).isEqualTo(110);

    try {
      // we can't obtain the channel quantile with time alone, unless we have a BarSeries
      Assertions.assertThat(model.getQuantile(t0));
      Assertions.failBecauseExceptionWasNotThrown(IllegalStateException.class);
    } catch (IllegalStateException ignore) {
    }

    Assertions.assertThat(model.getQuantile(t0, 110)).isEqualTo(50.0);
    Assertions.assertThat(model.getQuantile(t0, 120)).isEqualTo(100.0);
    Assertions.assertThat(model.getQuantile(t0.plusDays(30), 120)).isEqualTo(32);
    Assertions.assertThat(model.getPriceAtQuantile(t0, 50)).isEqualTo(110);
  }
}
