package bq.util;

import java.util.List;

import org.junit.jupiter.api.AfterEach;

import com.google.common.flogger.FluentLogger;



public abstract class BaseTest {

  private static final FluentLogger testLogger = FluentLogger.forEnclosingClass();
  private List<java.lang.AutoCloseable> deferredAutoCloseable = new java.util.ArrayList<>();


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
