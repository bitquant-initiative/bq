package bq.util;

public class BqException extends RuntimeException {

  public BqException(String message, Throwable cause) {
    super(message, cause);
  }

  public BqException(String message) {
    super(message);
  }

  public BqException(Throwable cause) {
    super(cause);
  }
}
