package bq.ducktape;

public class NoSuchIndicatorException extends IndicatorException {

  public NoSuchIndicatorException(IndicatorExpression expression) {
    super(expression, "no such indicator");
  }
}
