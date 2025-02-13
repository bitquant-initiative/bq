package bq.indicator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.ta4j.core.BarSeries;

public class TrendlineIndicator extends DateFunctionIndicator {

  public TrendlineIndicator(BarSeries series, LocalDate d0, LocalDate d1) {
    super(series);
    LinearTrendModel model = LinearTrendModel.from(series, d0, d1);
    setFunction(model.trendValueFunction());
  }

  public TrendlineIndicator(BarSeries series, String d0, String d1) {
    super(series);

    LinearTrendModel model = LinearTrendModel.from(series, d0, d1);
    setFunction(model.trendValueFunction());
  }

  static LocalDate toLocalDate(String s) {
    return LocalDate.parse(s, DateTimeFormatter.ISO_DATE);
  }

  @Override
  public int getUnstableBars() {

    return 0;
  }
}
