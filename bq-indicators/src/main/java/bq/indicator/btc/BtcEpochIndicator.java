package bq.indicator.btc;

import java.time.LocalDate;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class BtcEpochIndicator extends AbstractBtcEpochIndicator {

  public BtcEpochIndicator(BarSeries series) {
    super(series);
  }

  @Override
  public Num getValue(int index) {
    Bar bar = getBarSeries().getBar(index);
    if (bar == null) {
      return null;
    }
    LocalDate d = bar.getBeginTime().toLocalDate();

    long epoch = BtcUtil.getEpoch(d);
    return DoubleNum.valueOf(epoch);
  }
}
