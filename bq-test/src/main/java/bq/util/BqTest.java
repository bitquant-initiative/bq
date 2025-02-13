package bq.util;

import bq.duckdb.DuckDb;
import com.google.common.flogger.FluentLogger;
import java.util.List;
import org.junit.jupiter.api.AfterEach;

/**
 * This should be usable for any module that uses bq-ducdb.
 */
public abstract class BqTest {

  private static final FluentLogger testLogger = FluentLogger.forEnclosingClass();
  private List<java.lang.AutoCloseable> deferredAutoCloseable = new java.util.ArrayList<>();

  private DuckDb db = null;

  public final DuckDb db() {
    return getDb();
  }

  public final DuckDb getDb() {
    if (db == null) {
      this.db = DuckDb.createInMemory();
      defer(db);
    }
    return db;
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
          testLogger.atInfo().withCause(e).log("problem closing %s", c);
        }
      }
    } finally {
      this.deferredAutoCloseable.clear();
      this.db = null;
    }
  }
}
