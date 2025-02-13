package bq.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * SqlExecutionContext provides a fluent way to execute low-level JDBC.
 * It can be used in place of a try/catch/finally block that closes resources.
 */
public class SqlExecutionContext implements AutoCloseable {

  SqlCloser closer = new SqlCloser();
  Connection connection;

  public SqlExecutionContext(Connection c) {
    this.connection = c;
    closer.register(c);
  }

  public Connection getConnection() {
    return connection;
  }

  public void register(ResultSet rs) {
    closer.register(rs);
  }

  public void register(Statement st) {
    closer.register(st);
  }

  @Override
  public void close() throws Exception {
    closer.close();
  }
}
