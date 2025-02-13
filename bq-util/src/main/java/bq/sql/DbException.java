package bq.sql;

import bq.util.BqException;

public class DbException extends BqException {

  public DbException(String message, Throwable cause) {
    super(message, cause);
    // TODO Auto-generated constructor stub
  }

  public DbException(String message) {
    super(message);
    // TODO Auto-generated constructor stub
  }

  public DbException(Throwable cause) {
    super(cause);
    // TODO Auto-generated constructor stub
  }
}
