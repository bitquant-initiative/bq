package bq.loader;

import bq.duckdb.DuckDb;
import bq.ducktape.DuckTape;
import org.junit.jupiter.api.AfterEach;

public abstract class LoaderTest {

  private DuckDb db;
  private DuckTape tape;

  public DuckDb getDb() {
    if (db == null) {
      db = DuckDb.createInMemory();
    }
    return db;
  }

  public DuckTape getTape() {
    if (tape == null) {
      tape = DuckTape.create(getDb());
    }
    return tape;
  }

  @AfterEach
  private final void cleanup() {
    DuckDb x = db;
    db = null;
    tape = null;
    if (x != null) {
      x.close();
    }
  }
}
