package bq.indicator.btc;

import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.Num;

abstract class AbstractBtcEpochIndicator extends AbstractIndicator<Num> {

  protected AbstractBtcEpochIndicator(BarSeries series) {
    super(series);
  }

  @Override
  public int getUnstableBars() {

    return 0;
  }
}
