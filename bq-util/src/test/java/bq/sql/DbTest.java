package bq.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;

public class DbTest {
  Connection connection;

  Connection getConnection() {
    try {
      connection = DriverManager.getConnection("jdbc:duckdb:");

      return connection;
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  SqlTemplate newTemplate() {
    return SqlTemplate.create(getConnection());
  }

  @AfterEach
  private final void cleanup() throws SQLException {
    if (connection != null) {
      connection.close();
    }
  }
}
