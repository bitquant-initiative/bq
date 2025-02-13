package bq.ducktape;

public class IndicatorExecutionException extends IndicatorException {

  public IndicatorExecutionException(IndicatorExpression x, String message) {
    super(x, message);
  }

  public IndicatorExecutionException(IndicatorExpression x, String message, Throwable t) {
    super(x, message, t);
  }
}
