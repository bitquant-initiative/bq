package bq.util;

import bq.sql.SqlExecutionContext;
import java.sql.SQLException;

public interface SqlExecutionFunction<T> {

  public T apply(SqlExecutionContext ctx) throws SQLException;
}
