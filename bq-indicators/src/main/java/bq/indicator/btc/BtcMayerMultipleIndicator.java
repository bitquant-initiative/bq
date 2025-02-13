package bq.indicator.btc;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public class BtcMayerMultipleIndicator extends AbstractIndicator<Num> {

  static final int BAR_COUNT = 200;

  Indicator<Num> base;
  SMAIndicator sma200;

  public BtcMayerMultipleIndicator(BarSeries series) {
    super(series);

    base = new ClosePriceIndicator(series);
    sma200 = new SMAIndicator(base, BAR_COUNT);
  }

  public BtcMayerMultipleIndicator(Indicator<Num> indicator) {
    super(indicator.getBarSeries());
    sma200 = new SMAIndicator(indicator, BAR_COUNT);
    indicator = base;
  }

  @Override
  public Num getValue(int index) {

    Num price = base.getValue(index);
    Num sma = sma200.getValue(index);

    double d = price.doubleValue() / sma.doubleValue();
    return DoubleNum.valueOf(d);
  }

  @Override
  public int getUnstableBars() {

    return 0;
  }
}
