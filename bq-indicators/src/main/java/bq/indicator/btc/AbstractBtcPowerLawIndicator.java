package bq.indicator.btc;

import java.time.LocalDate;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.Num;

abstract class AbstractBtcPowerLawIndicator extends AbstractIndicator<Num> {

  BtcPowerLawModel powerLaw;

  public AbstractBtcPowerLawIndicator(BarSeries series) {
    this(series, BtcPowerLawModel.create());
  }

  public AbstractBtcPowerLawIndicator(BarSeries series, BtcPowerLawModel power) {
    super(series);
    this.powerLaw = power;
  }

  public AbstractBtcPowerLawIndicator(BarSeries series, int q) {

    this(series, BtcPowerLawModel.create().quantile(q));
  }

  public LocalDate getDate(int i) {
    return getBarSeries().getBar(i).getBeginTime().toLocalDate();
  }

  @Override
  public int getUnstableBars() {
    return 0;
  }
}
