package bq.indicator.btc;

import bq.ducktape.BarSeriesTable;
import bq.indicator.IndicatorTest;
import bq.indicator.btc.BtcPowerLawModel.QuantileModel;
import org.junit.jupiter.api.Test;

public class BtcPowerLawCalculatorTest extends IndicatorTest {

  @Test
  public void testIt() {
    BarSeriesTable t = loadBtcTable();

    QuantileModel m = BtcPowerLawCalculator.generateQuantileModel(t.getBarSeries(), 5.65);
  }
}
