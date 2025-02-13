package bq.duckdb;

import static bq.util.S.isNotBlank;

import bq.sql.DbException;
import bq.sql.TablePrinter;
import bq.util.BqException;
import bq.util.S;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.FluentLogger.Api;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBConnection;

public class DuckTable {

  static FluentLogger logger = FluentLogger.forEnclosingClass();

  DuckDb db;
  String table;

  public DuckTable(DuckDb d, String table) {
    Preconditions.checkNotNull(d);
    Preconditions.checkArgument(isNotBlank(table));
    this.db = d;
    this.table = table;
  }

  public DuckDb getDb() {
    return db;
  }

  public String getName() {
    return table;
  }

  public String getTableName() {
    return table;
  }

  public void append(DuckAppenderConsumer consumer) {
    try (DuckDBAppender appender = this.createAppender()) {
      consumer.accept(appender);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public void drop() {
    db.template().execute(c -> c.sql("drop table if exists ##table##").bind("table", getName()));
  }

  public void renameTable(String newName) {
    db.template()
        .execute(
            c ->
                c.sql("alter table ##old## rename to ##new##")
                    .bind("old", table)
                    .bind("new", newName));
    this.table = newName;
  }

  public void renameColumn(String oldName, String newName) {
    db.template()
        .execute(
            c ->
                c.sql("ALTER TABLE ##table## RENAME ##old## TO ##new##")
                    .bind("table", table)
                    .bind("old", oldName)
                    .bind("new", newName));
  }

  public int update(long rowId, String column, Object val) {

    return db.template()
        .update(
            c -> {
              c.sql("UPDATE ##table## set ##column##={{val}} where rowid={{id}}", val, rowId);
              c.bind("table", table);
              c.bind("column", column);
            });
  }

  public int deleteRow(long rowId) {

    return db.template()
        .update(
            c -> {
              c.sql("DELETE from ##table## where rowid={{id}}", rowId);
              c.bind("table", getTableName());
            });
  }

  /**
   * Convenience to write the table to a CSV file
   *
   * @param f
   */
  public void exportCsv(File f) {

    String sql =
        String.format("COPY %s TO '%s' (HEADER, DELIMITER ',')", table, f.getAbsolutePath());
    db.template().execute(sql);
  }

  public boolean exists() {
    if (S.isBlank(table)) {
      return false;
    }
    return db.template()
        .queryResult(
            c -> c.sql("show tables"),
            rs -> {
              while (rs.next()) {

                if (table.equalsIgnoreCase(rs.getString("name").orElse(""))) {
                  return true;
                }
              }
              return false;
            });
  }

  public void dropColumn(String column) {

    if (hasColumn(column)) {
      db.template()
          .execute(
              c ->
                  c.sql("ALTER TABLE ##table## DROP ##col##")
                      .bind("table", table)
                      .bind("col", column));
    }
  }

  public DuckDBAppender createAppender() {
    try {

      DuckDBConnection x = db.getConnection().getWrappedConnection();

      DuckDBAppender appender = x.createAppender(DuckDBConnection.DEFAULT_SCHEMA, table);

      return appender;
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public void addColumn(String columnSpec) {
    Preconditions.checkNotNull(columnSpec, "columnSpec");
    String sql = String.format("alter table %s add column %s", table, columnSpec);
    db.template().execute(sql);
  }

  public void addPrimaryKey(String column) {

    db.template()
        .execute(
            c ->
                c.sql("ALTER TABLE ##table## ADD PRIMARY KEY (##column##)")
                    .bind("table", table)
                    .bind("column", column));
  }

  public List<String> getTableNames() {
    return db.template()
        .query(
            c -> c.sql("show tables"),
            rs -> {
              return rs.getString("name").get();
            })
        .sorted()
        .toList();
  }

  public String toPrettyString() {
    return getDb()
        .template()
        .queryResult(
            c -> {
              c.sql("select * from " + getTableName());
            },
            rs -> {
              return TablePrinter.create().toString(rs.getResultSet());
            });
  }

  public void importCsv(byte[] data) {

    inportCsv(ByteSource.wrap(data));
  }

  public void inportCsv(ByteSource byteSource) {
    File tempFile = null;
    try (InputStream in = byteSource.openBufferedStream()) {
      tempFile = java.nio.file.Files.createTempFile("table", ".csv").toFile();

      ByteSink sink = Files.asByteSink(tempFile);
      sink.writeFrom(in);
      importCsv(tempFile);
    } catch (IOException e) {
      throw new BqException(e);
    } finally {
      if (tempFile != null && tempFile.exists()) {
        tempFile.delete();
      }
    }
  }

  public List<String> getColumnNames() {
    if (!exists()) {
      throw new DbException("table does not exist: " + getTableName());
    }
    List<String> columnNames =
        db.template()
            .queryResult(
                c -> c.sql("select * from ##table## limit 1").bind("table", getTableName()),
                rs -> {
                  var md = rs.getResultSet().getMetaData();
                  List<String> names = Lists.newArrayList();
                  for (int i = 1; i <= md.getColumnCount(); i++) {
                    String name = md.getColumnName(i);
                    names.add(name);
                  }
                  return names;
                });
    return List.copyOf(columnNames);
  }

  public void retainColumns(String... cols) {
    if (cols == null) {
      retainColumns(Set.of());
    }
    retainColumns(Set.of(cols));
  }

  public void retainColumns(Collection<String> names) {

    if (names == null) {
      names = Set.of();
    }
    Set<String> retain = names.stream().map(t -> t.toLowerCase()).collect(Collectors.toSet());
    Set<String> existing =
        getColumnNames().stream().map(t -> t.toLowerCase()).collect(Collectors.toSet());

    Set<String> drop = Sets.difference(existing, retain);

    dropColumns(drop);
  }

  public void dropColumns(String... cols) {
    if (cols == null) {
      return;
    }
    dropColumns(Set.of(cols));
  }

  public void dropColumns(Collection<String> s) {
    if (s == null) {
      return;
    }
    Set<String> toDrop = s.stream().map(t -> t.toLowerCase()).collect(Collectors.toSet());
    Set<String> existingColumns =
        getColumnNames().stream().map(n -> n.toLowerCase()).collect(Collectors.toSet());

    toDrop = Sets.intersection(existingColumns, Set.copyOf(s));

    toDrop.forEach(
        it -> {
          logger.atFine().log("dropping column: " + it);
          dropColumn(it);
        });
  }

  public void prettyPrint() {
    db.template()
        .print()
        .query(c -> c.sql("select * from ##table##").bind("table", getTableName()));
  }

  public void prettyLog() {
    db.template().log().query(c -> c.sql("select * from ##table##").bind("table", getTableName()));
  }

  public void prettyLog(Api log) {
    db.template()
        .log(log)
        .query(c -> c.sql("select * from ##table##").bind("table", getTableName()));
  }

  public boolean hasColumn(String column) {
    if (!exists()) {
      return false;
    }

    try {
      db.template()
          .query(
              c ->
                  c.sql("select ##col## from ##table## limit 1")
                      .bind("table", table)
                      .bind("col", column),
              rs -> {
                return "";
              });
      return true;
    } catch (BqException e) {
      return false;
    }
  }

  public void importCsv(String source) {
    if (exists()) {
      // user needs to do this manually....to many edge cases
      throw new UnsupportedOperationException(
          "importCsv() not supported for tables that already exist");
    }
    db.template()
        .execute(
            c ->
                c.sql("CREATE TABLE ##table## AS SELECT * FROM '##file##'")
                    .bind("table", this.table)
                    .bind("file", source));
  }

  public void importCsv(File file) {

    importCsv(file.toString());
  }

  public int rowCount() {
    String sql = "select count(*) as cnt from ##table##";
    return db.template().queryInt(c -> c.sql(sql).bind("table", getTableName()));
  }

  public int deleteWhere(String clause) {

    String sql = String.format("delete from %s where %s", getTableName(), clause);
    return db.template()
        .update(
            c -> {
              c.sql(sql);
            });
  }

  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", getName()).add("db", getDb()).toString();
  }
}
