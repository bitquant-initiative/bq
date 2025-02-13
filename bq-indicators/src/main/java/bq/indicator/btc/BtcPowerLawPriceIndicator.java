package bq.indicator.btc;

import java.time.LocalDate;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class BtcPowerLawPriceIndicator extends AbstractBtcPowerLawIndicator {

  public BtcPowerLawPriceIndicator(BarSeries series) {
    super(series);
  }

  public BtcPowerLawPriceIndicator(BarSeries series, BtcPowerLawModel power) {
    super(series, power);
  }

  public BtcPowerLawPriceIndicator(BarSeries series, int q) {
    super(series, q);
  }

  @Override
  public Num getValue(int index) {

    LocalDate d = getDate(index);

    double val = this.powerLaw.getPrice(d);

    return DoubleNum.valueOf(val);
  }
}
