package bq.sql;

import java.sql.SQLException;

public interface ResultSetProcessor<T> {

  public T process(Results rs) throws SQLException;
}
