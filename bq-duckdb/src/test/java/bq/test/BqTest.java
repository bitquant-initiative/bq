package bq.test;

import com.google.common.flogger.FluentLogger;

import bq.duckdb.DuckDb;

import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;


public abstract class BqTest {

  private static final FluentLogger testLogger = FluentLogger.forEnclosingClass();
  private List<java.lang.AutoCloseable> deferredAutoCloseable = new java.util.ArrayList<>();
  private DuckDb db;
  
  
  public final DuckDb getDb() {
    if (db!=null) {
      return db;
    }
    DuckDb db = DuckDb.createInMemory();
    AutoCloseable c = new AutoCloseable() {

      @Override
      public void close() throws Exception {
        db.close();
      }
    };
    defer(c);
    return db;
  }

  protected final void defer(java.lang.AutoCloseable c) {
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
      db=null;
    }
  }
}
