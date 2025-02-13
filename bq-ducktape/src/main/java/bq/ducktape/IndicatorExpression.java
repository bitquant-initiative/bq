package bq.ducktape;

import bq.util.S;
import com.google.common.base.CharMatcher;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.flogger.FluentLogger;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ta4j.core.Indicator;

public class IndicatorExpression {

  static FluentLogger logger = FluentLogger.forEnclosingClass();
  private static final Pattern INDICATOR_REGEX = Pattern.compile("(\\w[\\w_]*)\\W*\\((.*)\\)(.*)");

  String unparsedExpression;
  String fn;

  List<String> args;

  String outputName;
  BarSeriesTable table;

  public static IndicatorExpression parse(final String input) {

    Matcher m = INDICATOR_REGEX.matcher(input.trim());

    IndicatorExpression expression = new IndicatorExpression();
    expression.unparsedExpression = input;
    if (!m.matches()) {

      throw new InvalidExpressionException(expression, "parse error");
    }
    expression.fn = m.group(1);

    // do NOT use omitEmptyStrings()
    expression.args = Lists.newArrayList(Splitter.on(",").trimResults().splitToList(m.group(2)));

    if (expression.args.size() == 1 && S.isBlank(expression.args.get(0))) {

      expression.args = Lists.newArrayList();
    }
    // now parse the remainder "as" clause
    var remainderParts =
        Splitter.on(CharMatcher.whitespace())
            .omitEmptyStrings()
            .trimResults()
            .splitToList(m.group(3));

    if (remainderParts.size() == 0) {
      // no as clause ... nothing to do
      expression.outputName = null;
    } else if (remainderParts.size() == 2) {
      if (remainderParts.get(0).equalsIgnoreCase("as")) {

        String name = remainderParts.get(1);
        if (isValidTableName(name)) {
          expression.outputName = name;
        } else {
          throw new InvalidExpressionException(
              expression, "invalid 'as' clause: '" + m.group(3) + "'");
        }
      } else {
        throw new InvalidExpressionException(
            expression, "invalid 'as' clause: '" + m.group(3) + "'");
      }
    } else {
      throw new InvalidExpressionException(expression, "invalid 'as' clause: '" + m.group(3) + "'");
    }

    expression.getIndicatorClass();
    return expression;
  }

  static boolean isValidTableName(String name) {
    if (name == null) {
      return false;
    }
    if (name.length() == 0) {
      return false;
    }
    if (!(Character.isAlphabetic(name.charAt(0)) || name.charAt(0) == '_')) {
      return false;
    }

    return name.substring(1)
        .chars()
        .allMatch(c -> Character.isAlphabetic(c) || Character.isDigit(c) || c == '_');
  }

  public Optional<String> getOutputName() {
    return S.notBlank(outputName);
  }

  public String getFunctionName() {
    return fn;
  }

  public Class<? extends Indicator<?>> getIndicatorClass() {
    Class<? extends Indicator<?>> clazz =
        IndicatorRegistry.getRegistry().getIndicatorClass(fn).orElse(null);
    if (clazz == null) {
      throw new InvalidExpressionException(this, "no such function: " + fn);
    }
    return clazz;
  }

  public List<String> getArgs() {
    return this.args;
  }

  static List<String> toShortNames(Class[] z) {
    return Lists.newArrayList(z).stream().map(c -> c.getSimpleName()).toList();
  }

  public String getUnparsedExpression() {
    return this.unparsedExpression;
  }

  public String toString() {
    return MoreObjects.toStringHelper("IndicatorExpression")
        .add("fn", fn)
        .add("args", args)
        .toString();
  }
}
