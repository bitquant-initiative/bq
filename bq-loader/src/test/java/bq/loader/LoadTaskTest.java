package bq.loader;

import org.junit.jupiter.api.Test;

public class LoadTaskTest extends LoaderTest {

  @Test
  public void testIt() {

    LoadTask task = new LoadTask(getDb()).symbol("S:MSTR");

    task.execute();
  }
}
