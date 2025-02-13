package bq.util;

import bq.sql.SqlTemplate;
import com.google.common.flogger.FluentLogger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;

public abstract class BqTest {

  private static final FluentLogger testLogger = FluentLogger.forEnclosingClass();
  private List<java.lang.AutoCloseable> deferredAutoCloseable = new java.util.ArrayList<>();

  protected SqlTemplate newTemplate() {
    try {
      Connection c = DriverManager.getConnection("jdbc:duckdb:");
      defer(c);
      return SqlTemplate.create(c);
    } catch (SQLException e) {
      throw new BqException(e);
    }
  }

  protected void defer(java.lang.AutoCloseable c) {
    deferredAutoCloseable.add(c);
  }

  @AfterEach
  private final void bqTestCleanup() {
    try {
      for (AutoCloseable c : deferredAutoCloseable) {
        try {
          c.close();
        } catch (Exception e) {
          testLogger.atWarning().withCause(e).log("problem closing %s", c);
        }
      }
    } finally {
      this.deferredAutoCloseable.clear();
    }
  }
}
