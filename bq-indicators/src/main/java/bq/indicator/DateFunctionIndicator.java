package bq.indicator;

import com.google.common.base.Preconditions;
import java.time.ZonedDateTime;
import java.util.function.Function;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.indicators.AbstractIndicator;
import org.ta4j.core.num.Num;

public abstract class DateFunctionIndicator extends AbstractIndicator<Num> {

  Function<ZonedDateTime, Num> function;

  public DateFunctionIndicator(BarSeries bs, Function<ZonedDateTime, Num> function) {
    super(bs);
    this.function = function;
    Preconditions.checkArgument(function != null);
  }

  public DateFunctionIndicator(BarSeries bs) {
    super(bs);
  }

  public <T extends DateFunctionIndicator> T setFunction(Function<ZonedDateTime, Num> f) {
    this.function = f;
    return (T) this;
  }

  @Override
  public Num getValue(int index) {
    Bar b = getBarSeries().getBar(index);
    return function.apply(b.getBeginTime());
  }

  @Override
  public int getUnstableBars() {
    return 0;
  }
}
