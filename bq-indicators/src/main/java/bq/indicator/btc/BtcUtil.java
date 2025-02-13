package bq.indicator.btc;

import com.google.common.collect.Lists;
import com.google.common.flogger.FluentLogger;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class BtcUtil {
  static FluentLogger logger = FluentLogger.forEnclosingClass();
  static final List<LocalDate> epochStartDates;

  public static final LocalDate GENESIS_DATE = LocalDate.of(2009, 1, 3);

  static {
    List<LocalDate> tmp = Lists.newArrayList();

    tmp.add(GENESIS_DATE);
    tmp.add(LocalDate.of(2012, 11, 28));
    tmp.add(LocalDate.of(2016, 7, 9));
    tmp.add(LocalDate.of(2020, 5, 11));
    tmp.add(LocalDate.of(2024, 4, 19));

    LocalDate lastHalving = tmp.getLast();
    for (int i = 1; i < 20; i++) {

      tmp.add(lastHalving.plusDays(i * 365 * 4));
    }
    epochStartDates = List.copyOf(tmp);
  }

  public static List<LocalDate> getEpochStartDates() {
    return epochStartDates;
  }

  public static List<LocalDate> getHalvingDates() {
    return epochStartDates.subList(1, epochStartDates.size() - 1);
  }

  public static LocalDate getGenesisDate() {
    return GENESIS_DATE;
  }

  public static int getDaysSinceGenesis(LocalDate d) {
    if (d.isBefore(GENESIS_DATE)) {
      return (int) ChronoUnit.DAYS.between(d, GENESIS_DATE) * -1;
    }
    return (int) ChronoUnit.DAYS.between(GENESIS_DATE, d);
  }

  public static BigDecimal getBlockReward(int epoch) {
    if (epoch < 1) {
      return BigDecimal.ZERO;
    }

    // block reward in first epoch (immediately after genesis) was 50
    BigDecimal firstEpochBlockReward = new BigDecimal(50);

    return firstEpochBlockReward.multiply(
        new BigDecimal(2).pow((epoch - 1) * -1, MathContext.DECIMAL64));
  }

  public static LocalDate getStartOfEpoch(int i) {
    if (i < 1) {
      return LocalDate.EPOCH; // need to return something
    }
    return getEpochStartDates().get(i - 1);
  }

  public static BigDecimal getBlockReward(LocalDate d) {
    if (d == null) {
      return BigDecimal.ZERO;
    }
    return getBlockReward(getEpoch(d));
  }

  public static int getDayOfEpoch(LocalDate d) {
    if (d.isBefore(GENESIS_DATE)) {
      return 0;
    }
    int epoch = getEpoch(d); // there is no epoch 0!!!

    LocalDate startOfEpoch = getStartOfEpoch(epoch);

    return (int) ChronoUnit.DAYS.between(startOfEpoch, d);
  }

  public static int getEpoch(LocalDate d) {

    for (int i = 0; i < epochStartDates.size(); i++) {

      LocalDate nextEpochStartDate = epochStartDates.get(i);

      if (d.isBefore(nextEpochStartDate)) {
        return i;
      }
    }
    return epochStartDates.size();
  }
}
