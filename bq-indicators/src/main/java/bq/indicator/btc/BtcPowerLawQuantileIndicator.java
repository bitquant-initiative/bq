package bq.indicator.btc;

import java.time.LocalDate;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class BtcPowerLawQuantileIndicator extends AbstractBtcPowerLawIndicator {

  public BtcPowerLawQuantileIndicator(BarSeries series) {
    super(series);
  }

  public BtcPowerLawQuantileIndicator(BarSeries series, BtcPowerLawModel power) {
    super(series, power);
  }

  public BtcPowerLawQuantileIndicator(BarSeries series, int q) {
    super(series, q);
  }

  @Override
  public Num getValue(int index) {

    Num num = getBarSeries().getBar(index).getClosePrice();
    if (num == null) {
      return null;
    }
    LocalDate d = getDate(index);
    double q = this.powerLaw.getQuantile(d, num.doubleValue());

    return DoubleNum.valueOf(q);
  }
}
