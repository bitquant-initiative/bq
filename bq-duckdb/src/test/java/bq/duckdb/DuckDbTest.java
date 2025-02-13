package bq.duckdb;
import org.junit.jupiter.api.Disabled;
import bq.sql.DbException;
import bq.util.Zones;
import com.google.common.flogger.FluentLogger;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class DuckDbTest extends BaseTest {

  static FluentLogger logger = FluentLogger.forEnclosingClass();

  @Test
  public void testInMemoryShared() throws SQLException {
    DuckDb a = DuckDb.getSharedInMemory();
    DuckDb b = DuckDb.getSharedInMemory();

    Assertions.assertThat(a.getConnection().isSharedInMemory()).isTrue();
    Assertions.assertThat(a).isSameAs(b);
    Assertions.assertThat(a.getConnection()).isSameAs(b.getConnection());

    try {
      a.close();
      Assertions.failBecauseExceptionWasNotThrown(DbException.class);
    } catch (DbException e) {
      // ok
    }
    Assertions.assertThat(a.getConnection().isClosed()).isFalse();

    a.getConnection().close(); // should NOT throw an exception

    Assertions.assertThat(a.getConnection().isClosed()).isFalse();
  }

  @Test
  public void testChangeTable() {
    DuckDb d = DuckDb.createInMemory();
    defer(d);

    Assertions.assertThat(d.table("test").getTableName()).isEqualTo("test");
    Assertions.assertThat(d.table("test").getDb()).isSameAs(d);
  }

  @Test
  public void testId() throws SQLException {

    // Connections are isolated in DuckDB, so these two instances will be different
    // databases
    DuckDb a = DuckDb.createInMemory();
    DuckDb b = DuckDb.createInMemory();

    System.out.println(a.getConnection());
    a.template().execute("create table test (abc int)");

    a.template().log().query("select * from test");
    System.out.println(a.getConnection());
    Assertions.assertThat(a.table("test").exists()).isTrue();

    Assertions.assertThat(b.table("test").exists()).isFalse();

    DuckTable c = b.table("another");

    Assertions.assertThat(c.getDb().getConnection())
        .isSameAs(b.getConnection())
        .isNotSameAs(a.getConnection());
    a.close();
    Assertions.assertThat(a.getConnection().isClosed()).isTrue();
    b.close();
    Assertions.assertThat(b.getConnection().isClosed()).isTrue();
    Assertions.assertThat(c.getDb().getConnection().isClosed()).isTrue();
  }

  @Test
  public void testIt() throws IOException {

    DuckDb d = DuckDb.createInMemory();

    defer(d);
    DuckTable test = d.table("test");

    Assertions.assertThat(test.getTableName()).isEqualTo("test");
    d.template().execute("create table test (name varchar(20), age int)");

    System.out.println(d.getConnection());
    d.table("test")
        .append(
            appender -> {
              appender.beginRow();
              appender.append("steve");
              appender.append("19");
              appender.endRow();

              appender.beginRow();
              appender.append("poppins, mary");
              appender.append("22");
              appender.endRow();
            });

    System.out.println(test.toPrettyString());

    d.table("test")
        .append(
            appender -> {
              appender.beginRow();
              appender.append("vivek");
              appender.append("32");
              appender.endRow();
            });

    d.table(test.getTableName()).update(0, "age", 7);

    System.out.println(d.table("test").toPrettyString());

    File f = Files.createTempFile("test", ".csv").toFile();
    d.table(test.getTableName()).exportCsv(f);
  }

  @Test
  public void testToString() {
    DuckDb db = DuckDb.createInMemory();
    Assertions.assertThat(db.toString()).startsWith("DuckDb{@=").endsWith("url=jdbc:duckdb:}");
    db.close();

    db = DuckDb.getSharedInMemory();
    Assertions.assertThat(db.toString())
        .startsWith("DuckDb{@=")
        .endsWith(", url=jdbc:duckdb:, shared=true}");

    DuckTable table = db.table("mytable");

    System.out.println(table);
    Assertions.assertThat(table.toString())
        .isEqualTo("DuckTable{name=mytable, db=" + db.toString() + "}");
  }

  @Test
  public void testRename() {
    DuckDb db = DuckDb.getSharedInMemory();

    db.table("old_name").drop();
    db.table("student").drop();
    db.template().execute("create table old_name (abc int)");
    Assertions.assertThat(db.table("old_name").exists()).isTrue();
    db.table("old_name").renameTable("student");
    Assertions.assertThat(db.table("old_name").exists()).isFalse();
    Assertions.assertThat(db.table("student").exists()).isTrue();

    db.table("student").renameColumn("abc", "age");

    db.template().update("insert into student (age) values ({{age}})", 22);

    db.template().log().query("select age from student");
  }

  @Test
  public void testZone() {
    DuckDb db = DuckDb.createInMemory();
    Assertions.assertThat(db.operations().getSessionTimeZone()).isEqualTo(Zones.UTC);
    System.out.println(db.operations().getSessionTimeZone());
    db.close();
  }

  @Test
  public void testAddDropRenameColumn() {
    DuckDb db = DuckDb.createInMemory();

    DuckTable table = db.table("cities");
    Assertions.assertThat(table.exists()).isFalse();

    db.table("cities").importCsv("./us-cities.csv");

    Assertions.assertThat(table.exists()).isTrue();

    db.template().log().query("select city,state from cities");

    Assertions.assertThat(table.hasColumn("city")).isTrue();
    Assertions.assertThat(table.hasColumn("cityx")).isFalse();
    db.table("cities").dropColumn("longitude");
    Assertions.assertThat(table.hasColumn("longitude")).isFalse();
    try {
      db.template().log().query("select logitude from cities");
      Assertions.failBecauseExceptionWasNotThrown(DbException.class);
    } catch (DbException e) {
      // expected
    }
    db.table("cities").addColumn("new_col bigint");
    db.template().log().query("select new_col from cities limit 1");

    table.renameColumn("latitude", "lat");
    Assertions.assertThat(table.hasColumn("latitude")).isFalse();
    Assertions.assertThat(table.hasColumn("lat")).isTrue();
    db.template().log().query("select lat from cities limit 1");

    System.out.println(db.table("cities").toPrettyString());
  }

  @Test
  @Disabled
  public void testImportViaHttp() {

    DuckDb db = DuckDb.createInMemory();

    var table = db.table("cities");
    table.importCsv(
        "https://raw.githubusercontent.com/bitquant-initiative/bq-duckdb/refs/heads/main/us-cities.csv");

    db.template().print().query("select * from cities");

    Assertions.assertThat(db.table("cities").getColumnNames())
        .contains("city", "state", "state_capital");

    defer(db);
  }
}
