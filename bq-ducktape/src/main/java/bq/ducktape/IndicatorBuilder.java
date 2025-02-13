package bq.ducktape;

import bq.util.BqException;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.flogger.FluentLogger;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.List;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.pivotpoints.TimeLevel;
import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

@SuppressWarnings("rawtypes")
public class IndicatorBuilder {

  static FluentLogger logger = FluentLogger.forEnclosingClass();

  Class<? extends Indicator> indicatorClass;
  List<String> args = List.of();
  BarSeriesTable table;
  BarSeries barSeries;
  IndicatorExpression expression;
  ClosePriceIndicator closePriceIndicator;

  Constructor constructorUsed = null;

  public static IndicatorBuilder newBuilder() {
    return new IndicatorBuilder();
  }

  public Constructor getConstructorUsed() {
    return constructorUsed;
  }

  public IndicatorBuilder expression(IndicatorExpression expression) {
    this.expression = expression;
    return this;
  }

  public IndicatorBuilder expression(String expression) {
    this.expression = IndicatorExpression.parse(expression);
    return this;
  }

  public IndicatorBuilder indicator(Class<? extends Indicator> x) {
    this.indicatorClass = x;
    return this;
  }

  public IndicatorBuilder args(String... s) {
    if (s == null || (s != null && s.length == 1 && s[0] == null)) {
      args = List.of();
    }

    args = Lists.newArrayList(s).stream().map(t -> t.trim()).toList();

    return this;
  }

  public IndicatorBuilder table(BarSeriesTable table) {
    this.table = table;
    this.barSeries = table.getBarSeries();
    return this;
  }

  /**
   * There are some obscure issues with overloaded constructors with the same
   * arity. For example, FisherIndicator has:
   *
   * FisherIndicator(Indicator, int, double, boolean) FisherIndicator(Indicator,
   * int, double, double)
   *
   * with args specified as: ("close", 10, 20, 30)
   *
   * this will actually match the boolean constructor, which is not what is
   * desired. By sorting the booleans with lower precedence, we work around the
   * issue
   *
   * Another solution may be to make the boolean parsing more strict. The fisher
   * this doesn't seem to be a common issue.
   *
   * @param list
   * @return
   */
  static List<Constructor> sortByPrecdence(List<Constructor> list) {

    Comparator<Constructor> comparator =
        new Comparator<Constructor>() {

          @Override
          public int compare(Constructor o1, Constructor o2) {

            if (o1.getParameterCount() > o2.getParameterCount()) {
              return 1;
            }
            if (o1.getParameterCount() < o2.getParameterCount()) {
              return -1;
            }

            return 0;
          }
        };

    List<Constructor> tmp = list.stream().sorted(comparator).toList();

    List<Constructor> output = Lists.newArrayList();

    output.addAll(
        tmp.stream()
            .filter(
                c ->
                    !Lists.newArrayList(c.getParameterTypes())
                        .toString()
                        .toLowerCase()
                        .contains("boolean"))
            .toList());
    output.addAll(
        tmp.stream()
            .filter(
                c ->
                    Lists.newArrayList(c.getParameterTypes())
                        .toString()
                        .toLowerCase()
                        .contains("boolean"))
            .toList());

    return output;
  }

  <T extends Indicator> T build() {

    RuntimeException lastException = null;

    if (expression != null) {

      indicatorClass = expression.getIndicatorClass();
      args = expression.getArgs();
    }

    List<Constructor> ctors = Lists.newArrayList(indicatorClass.getDeclaredConstructors());

    for (Constructor ctor : ctors) {

      try {
        // arity check will help filter constructors
        if (ctor.getParameterCount() < args.size()) {
          // if we have more parameters on the constructor than we have supplied
          // there is no way that the it can match
        } else if (ctor.getParameterCount() > args.size() + 1) {
          // THis one is tricky. If the arity matches, we need to evaluate.
          // If the supplied parameters is one LESS than the constructor count, we also
          // need to
          // match. This is due to the fact that BarSeries and Indicator can be implicit
          // in some
          // cases.
        } else {

          logger.atFiner().log(
              "trying to build %s using %s with %s", indicatorClass.getSimpleName(), ctor, args);
          Indicator indicator = build(ctor);
          if (indicator != null) {
            return (T) indicator;
          }
        }
      } catch (RuntimeException e) {
        lastException = e;
      }
    }

    if (lastException != null) {
      throw lastException;
    }
    throw new InvalidExpressionException(
        expression,
        "could not build Indicator: " + ((expression != null) ? expression : indicatorClass));
  }

  Indicator build(Constructor<Indicator> ctor) {

    Preconditions.checkNotNull(ctor);

    Class<?>[] ptypes = ctor.getParameterTypes();
    List<Object> ctorArgs = Lists.newArrayList();
    List<String> args = Lists.newArrayList(this.args); // make a mutable copy

    if (ptypes == null || ptypes.length == 0) {

      throw new BqException("no-arg constructors not allowed");
    }
    if (Modifier.isAbstract(ctor.getModifiers())) {
      throw new BqException("abstract constructors not allowed");
    }

    for (int i = 0; ptypes != null && i < ptypes.length; i++) {
      Class<?> type = ptypes[i];
      String arg = args.isEmpty() ? null : args.getFirst();
      if (BarSeries.class.equals(type)) {

        // bar series is always implicit, does not require an arg
        Preconditions.checkState(barSeries != null, "BarSeries not set");
        logger.atFiner().log("#%s: %s", i, barSeries);
        ctorArgs.add(barSeries);

      } else if (Indicator.class.equals(type)) {
        if (arg != null
            && arg.length() > 0
            && (Character.isLetter(arg.charAt(0)) || arg.charAt(0) == '_')) {
          // the arg looks like an indicator column
          Preconditions.checkState(table != null, "table must be set");
          Indicator indicator = table.getIndicator(arg);
          logger.atFiner().log("#%s: %s", i, indicator);
          ctorArgs.add(indicator);
          args.removeFirst(); // we just consumed an argument
        } else {
          // we weren't given a column, but if we're in the first column, we can make an
          // assumption that
          // ClosePriceIndicator is used;
          if (i != 0) {
            throw new BqException("when more than one indicator is required it must be explicit");
          }
          if (closePriceIndicator == null) {
            closePriceIndicator = new ClosePriceIndicator(barSeries);
          }
          logger.atFiner().log("#%s: %s (implicit)", i, closePriceIndicator);
          ctorArgs.add(closePriceIndicator);

          // we did NOT consume an arg
        }
      } else {

        if (args.isEmpty()) {

          throw new InvalidExpressionException(expression, "arg underflow building");
        }
        Object val = cast(arg, type);
        ctorArgs.add(val);
        logger.atFiner().log("#%s: %s", i, val);
        args.removeFirst();
      }
    }

    if (!args.isEmpty()) {

      throw new InvalidExpressionException(expression, "arg underflow");
    }

    if (ctorArgs.size() != ptypes.length) {
      throw new InvalidExpressionException(
          expression, "expected " + ptypes.length + " got " + ctorArgs.size());
    }

    try {
      this.constructorUsed = ctor;
      return ctor.newInstance(ctorArgs.toArray());
    } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
      throw new IndicatorExecutionException(expression, "constructor failed", e);
    }
  }

  Object cast(String input, Class type) {

    try {
      if (input == null) {
        return null;
      } else if (type == int.class) {
        return Integer.parseInt(input);
      } else if (type == double.class || type == Double.class || type == Number.class) {

        return Double.parseDouble(input);
      } else if (type == Boolean.class || type == boolean.class) {
        return Boolean.parseBoolean(input.toLowerCase());
      } else if (type == TimeLevel.class) {
        try {
          return TimeLevel.valueOf(input.toUpperCase());

        } catch (IllegalArgumentException e) {
          throw new InvalidExpressionException(
              expression, "TimeLevel arg must be one of: " + List.of(TimeLevel.values()));
        }
      } else if (type == Num.class) {
        double d = Double.parseDouble(input);
        return DoubleNum.valueOf(d);
      } else {
        throw new InvalidExpressionException(
            expression, "Indicator parameter type not supported: " + type);
      }
    } catch (IndicatorException e) {
      throw e;
    } catch (IllegalArgumentException e) {
      String message = String.format("could not convert '%s' to %s", input, type);
      throw new InvalidExpressionException(expression, message);
    }
  }
}
