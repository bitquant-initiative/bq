package bq.sql;

import java.sql.SQLException;

public interface RowMapper<T> {
  public T map(Results rs) throws SQLException;
}
