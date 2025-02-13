package bq.duckdb;

import com.google.common.flogger.FluentLogger;
import java.util.List;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class BaseTest {

  FluentLogger logger = FluentLogger.forEnclosingClass();
  List<DuckDb> cleanup = Lists.newArrayList();
  DuckDb db;

  public DuckDb getDb() {
    if (db == null) {
      db = DuckDb.createInMemory();
      defer(db);
    }
    return db;
  }

  void defer(DuckDb db) {
    if (db != null) {
      cleanup.add(db);
    }
  }

  @BeforeEach
  private final void setup() {
    getDb();
  }

  @AfterEach
  private final void cleanup() {
    cleanup.forEach(
        d -> {
          try {
            logger.atFiner().log("closing %s", d);
            d.close();
          } catch (RuntimeException e) {
            logger.atWarning().withCause(e).log("cleanup problem");
          }
        });
    cleanup.clear();
  }
}
