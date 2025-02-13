package bq.indicator.btc;

import com.google.common.flogger.FluentLogger;
import java.time.LocalDate;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class BtcUtilTest {

  static FluentLogger logger = FluentLogger.forEnclosingClass();

  @Test
  public void testXX() {
    Assertions.assertThat(BtcUtil.getStartOfEpoch(1)).isEqualTo(LocalDate.of(2009, 1, 3));
  }

  @Test
  public void testGetDaysSinceGenesis() {
    Assertions.assertThat(BtcUtil.getDaysSinceGenesis(BtcUtil.GENESIS_DATE)).isEqualTo(0);
    Assertions.assertThat(BtcUtil.getDaysSinceGenesis(BtcUtil.GENESIS_DATE.minusDays(1)))
        .isEqualTo(-1);
    Assertions.assertThat(BtcUtil.getDaysSinceGenesis(BtcUtil.GENESIS_DATE.plusDays(1)))
        .isEqualTo(1);

    for (int i = 0; i < 6000; i++) {
      LocalDate d = BtcUtil.GENESIS_DATE.plusDays(i);
      Assertions.assertThat(BtcUtil.getDaysSinceGenesis(d)).isEqualTo(i);
    }
    Assertions.assertThat(BtcUtil.getDaysSinceGenesis(LocalDate.of(2024, 1, 1))).isEqualTo(5476);
  }

  @Test
  public void testGetEpochDay() {
    for (int epoch = 1; epoch < 10; epoch++) {

      LocalDate startOfEpoch = BtcUtil.getStartOfEpoch(epoch);
      logger.atFiner().log("testing epoch=%s start=%s", epoch, startOfEpoch);

      Assertions.assertThat(BtcUtil.getDayOfEpoch(BtcUtil.GENESIS_DATE)).isEqualTo(0);
      for (int i = 0; i < 2000; i++) {
        Assertions.assertThat(BtcUtil.getDayOfEpoch(BtcUtil.GENESIS_DATE.minusDays(i)))
            .isEqualTo(0);
      }

      if (epoch > 1) {
        Assertions.assertThat(BtcUtil.getDayOfEpoch(startOfEpoch.minusDays(1))).isGreaterThan(1300);
      }
      Assertions.assertThat(BtcUtil.getDayOfEpoch(startOfEpoch)).isEqualTo(0);
      Assertions.assertThat(BtcUtil.getDayOfEpoch(startOfEpoch.plusDays(1))).isEqualTo(1);
      for (int i = 0; i < 1200; i++) {
        Assertions.assertThat(BtcUtil.getDayOfEpoch(startOfEpoch.plusDays(i))).isEqualTo(i);
      }
      Assertions.assertThat(BtcUtil.getDayOfEpoch(startOfEpoch.plusDays(1))).isEqualTo(1);
    }
  }

  @Test
  public void testStartOfEpoch() {

    Assertions.assertThat(BtcUtil.getStartOfEpoch(1)).isEqualTo(LocalDate.of(2009, 1, 3));
    Assertions.assertThat(BtcUtil.getStartOfEpoch(2)).isEqualTo(LocalDate.of(2012, 11, 28));

    for (int epoch = 1; epoch < BtcUtil.getEpochStartDates().size(); epoch++) {

      LocalDate d = BtcUtil.getStartOfEpoch(epoch);

      Assertions.assertThat(BtcUtil.getEpoch(d)).isEqualTo(epoch);
    }

    Assertions.assertThat(BtcUtil.getStartOfEpoch(-1)).isEqualTo(LocalDate.EPOCH);
    Assertions.assertThat(BtcUtil.getStartOfEpoch(0)).isEqualTo(LocalDate.EPOCH);
  }

  @Test
  public void testMethods() {

    // thre is no 0 epoch
    Assertions.assertThat(BtcUtil.getEpoch(LocalDate.of(2009, 1, 2))).isEqualTo(0);

    Assertions.assertThat(BtcUtil.getEpoch(LocalDate.of(2009, 1, 3))).isEqualTo(1);
    Assertions.assertThat(BtcUtil.getEpoch(LocalDate.of(2010, 1, 5))).isEqualTo(1);
    Assertions.assertThat(BtcUtil.getEpoch(LocalDate.of(2009, 1, 3))).isEqualTo(1);

    Assertions.assertThat(BtcUtil.getEpoch(LocalDate.of(2012, 11, 27))).isEqualTo(1);

    Assertions.assertThat(BtcUtil.getEpoch(LocalDate.of(2012, 11, 28))).isEqualTo(2);
    Assertions.assertThat(BtcUtil.getEpoch(LocalDate.of(2012, 11, 29))).isEqualTo(2);

    for (int i = 0; i < BtcUtil.getEpochStartDates().size(); i++) {
      int epoch = i + 1;
      Assertions.assertThat(BtcUtil.getEpoch(BtcUtil.getEpochStartDates().get(i))).isEqualTo(epoch);
      if (epoch > 1) {
        Assertions.assertThat(BtcUtil.getEpoch(BtcUtil.getEpochStartDates().get(i - 1)))
            .isEqualTo(epoch - 1);
      }
    }
  }

  @Test
  public void testEpochStartDate() {
    Assertions.assertThat(BtcUtil.getHalvingDates()).doesNotContain(BtcUtil.GENESIS_DATE);
    Assertions.assertThat(BtcUtil.getEpochStartDates()).contains(BtcUtil.GENESIS_DATE);
  }

  @Test
  public void testBlockReward() {

    Assertions.assertThat(BtcUtil.getBlockReward(0)).isEqualByComparingTo("0");
    Assertions.assertThat(BtcUtil.getBlockReward(1)).isEqualByComparingTo("50");
    Assertions.assertThat(BtcUtil.getBlockReward(2)).isEqualByComparingTo("25");
    Assertions.assertThat(BtcUtil.getBlockReward(3)).isEqualByComparingTo("12.5");
    Assertions.assertThat(BtcUtil.getBlockReward(4)).isEqualByComparingTo("6.25");
    Assertions.assertThat(BtcUtil.getBlockReward(5)).isEqualByComparingTo("3.125");
    Assertions.assertThat(BtcUtil.getBlockReward(6)).isEqualByComparingTo("1.56250");
    Assertions.assertThat(BtcUtil.getBlockReward(7)).isEqualByComparingTo("0.781250");
    Assertions.assertThat(BtcUtil.getBlockReward(-1)).isEqualByComparingTo("0");

    for (int i = 1; i < 15; i++) {
      logger.atInfo().log("epoch=%s reward=%s", i, BtcUtil.getBlockReward(i));
    }

    Assertions.assertThat(BtcUtil.getBlockReward(LocalDate.now())).isEqualByComparingTo("3.125");
    Assertions.assertThat(BtcUtil.getBlockReward(LocalDate.of(2009, 1, 3)))
        .isEqualByComparingTo("50");
    Assertions.assertThat(BtcUtil.getBlockReward(LocalDate.of(2009, 1, 2)))
        .isEqualByComparingTo("0");
    Assertions.assertThat(BtcUtil.getBlockReward(LocalDate.of(2000, 1, 2)))
        .isEqualByComparingTo("0");
  }
}
