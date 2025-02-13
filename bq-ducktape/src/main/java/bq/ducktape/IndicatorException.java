package bq.ducktape;

import bq.util.BqException;

public abstract class IndicatorException extends BqException {

  IndicatorExpression expression;

  public IndicatorException(IndicatorExpression x, String message) {
    super(String.format("%s - '%s'", message, x != null ? x.getUnparsedExpression() : null));
    expression = x;
  }

  public IndicatorException(IndicatorExpression x, String message, Throwable cause) {
    super(String.format("%s - '%s'", message, x != null ? x.getUnparsedExpression() : null), cause);
    expression = x;
  }

  public String getUnparsedExpression() {
    return expression != null ? expression.getUnparsedExpression() : null;
  }
}
