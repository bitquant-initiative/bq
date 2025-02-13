package bq.duckdb;

import java.sql.SQLException;
import org.duckdb.DuckDBAppender;

public interface DuckAppenderConsumer {

  public void accept(DuckDBAppender appender) throws SQLException;
}
