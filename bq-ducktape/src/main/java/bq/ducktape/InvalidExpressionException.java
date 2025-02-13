package bq.ducktape;

public class InvalidExpressionException extends IndicatorException {

  public InvalidExpressionException(IndicatorExpression x, String message) {
    super(x, message);
  }
}
