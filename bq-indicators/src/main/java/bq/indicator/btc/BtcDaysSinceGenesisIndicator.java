package bq.indicator.btc;

import java.time.LocalDate;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class BtcDaysSinceGenesisIndicator extends AbstractBtcEpochIndicator {

  public BtcDaysSinceGenesisIndicator(BarSeries series) {
    super(series);
  }

  @Override
  public Num getValue(int index) {
    Bar b = getBarSeries().getBar(index);
    if (b == null) {
      return null;
    }
    LocalDate d = b.getBeginTime().toLocalDate();
    int day = BtcUtil.getDaysSinceGenesis(d);

    return DoubleNum.valueOf(day);
  }
}
