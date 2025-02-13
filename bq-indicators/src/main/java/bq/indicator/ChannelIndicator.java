package bq.indicator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.ta4j.core.BarSeries;

public class ChannelIndicator extends DateFunctionIndicator {

  public ChannelIndicator(BarSeries series, String type, LocalDate d0, LocalDate d1, LocalDate d2) {
    super(series);

    LinearTrendModel model = LinearTrendModel.from(series, d0, d1);
    model.channel(d2);

    if (type.equalsIgnoreCase("upper")) {
      setFunction(model.quantilePriceFunction(100d));
      return;
    } else if (type.equalsIgnoreCase("middle")) {
      setFunction(model.quantilePriceFunction(50d));
      return;

    } else if (type.equalsIgnoreCase("lower")) {
      setFunction(model.quantilePriceFunction(0d));
      return;
    } else if (type.equalsIgnoreCase("quantile")) {
      setFunction(model.quantileFunction());
      return;
    }

    try {
      double q = Double.parseDouble(type);
      setFunction(model.quantilePriceFunction(q));
      return;
    } catch (RuntimeException ignore) {

    }

    throw new IllegalArgumentException(
        "channel indicator type must be one of: [upper, middle, lower, quantile, #] was ("
            + type
            + ")");
  }

  public ChannelIndicator(BarSeries series, String type, String d0, String d1, String d2) {
    this(series, type, toLocalDate(d0), toLocalDate(d1), toLocalDate(d2));
  }

  static LocalDate toLocalDate(String s) {
    return LocalDate.parse(s, DateTimeFormatter.ISO_DATE);
  }
}
