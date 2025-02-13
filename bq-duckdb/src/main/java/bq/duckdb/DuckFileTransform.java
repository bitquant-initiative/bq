package bq.duckdb;

import bq.sql.RowMapper;
import bq.sql.SqlTemplate;
import bq.sql.StatementBuilder;
import bq.util.S;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.flogger.FluentLogger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Utility for codifying a pattern of importing a CSV file, processing it, and
 * then writing it back.
 */
public class DuckFileTransform {

  FluentLogger logger = FluentLogger.forEnclosingClass();
  DuckDb db;

  String source;
  String sourceTable;
  String currentTable;

  String orderBy = null;

  List<String> deferredDrop = Lists.newArrayList();
  List<Consumer<DuckFileTransform>> transforms = Lists.newArrayList();

  String dest;
  boolean writeOnSuccess = false;

  public static DuckFileTransform with(DuckDb db) {
    DuckFileTransform transform = new DuckFileTransform();
    transform.db = db;
    return transform;
  }

  public static DuckFileTransform withSharedInMemory() {
    return with(DuckDb.getSharedInMemory());
  }

  public DuckFileTransform orderBy(String clause) {
    this.orderBy = clause;

    return this;
  }

  public DuckFileTransform table(String table) {
    this.currentTable = table;
    return this;
  }

  public DuckFileTransform source(String src) {
    this.source = src;
    return this;
  }

  public DuckDb getDb() {
    return db();
  }

  public DuckDb db() {
    return db;
  }

  public DuckFileTransform sourceTable(String srcTable) {
    Preconditions.checkState(this.sourceTable == null, "once set sourceTable cannot be changed");
    this.currentTable = srcTable;
    return this;
  }

  public String getTable() {
    return this.currentTable;
  }

  public String table() {
    return getTable();
  }

  public DuckFileTransform execute(String sql) {
    db().template().execute(sql);
    return this;
  }

  public Optional<String> getSource() {
    return S.notBlank(source);
  }

  public SqlTemplate template() {
    return this.db().template();
  }

  public <T> Stream<T> query(Consumer<StatementBuilder> b, RowMapper<T> rsp) {

    return db().template().query(b, rsp);
  }

  public String getSourceTable() {
    return sourceTable;
  }

  public DuckFileTransform deferDrop(String table) {
    this.deferredDrop.add(table);
    return this;
  }

  public DuckFileTransform transform(Consumer<DuckFileTransform> t) {
    this.transforms.add(t);
    return this;
  }

  public DuckFileTransform writeOnSuccess(String dest) {
    this.dest = dest;
    writeOnSuccess = true;
    return this;
  }

  public DuckFileTransform writeOnSuccess() {
    dest = source;
    this.writeOnSuccess = true;
    return this;
  }

  public DuckFileTransform execute() {

    try {
      if (source != null) {
        String tempTable = String.format("tmp_%s", UUID.randomUUID().toString().replace("-", ""));
        deferDrop(tempTable);
        this.sourceTable = tempTable;
        String sql =
            String.format(
                "create table %s as (select * from '%s' %s)",
                tempTable, source, orderBy != null ? ("order by " + orderBy) : "");
        logger.atFine().log("SQL: %s", sql);
        db.template().execute(sql);
        this.currentTable = tempTable;
      }

      for (Consumer<DuckFileTransform> transform : this.transforms) {
        transform.accept(this);
      }

      if (writeOnSuccess) {
        String sql =
            String.format(
                "COPY (SELECT * FROM %s %s) TO '%s' (HEADER, DELIMITER ',')",
                currentTable, orderBy != null ? ("order by " + orderBy) : "", dest);
        db.template().execute(sql);
      } else {
        logger.atInfo().log("will NOT write unless writeOnSuccess() is called");
      }

      return this;

    } finally {
      for (String t : this.deferredDrop) {
        String sql = "drop table if exists " + t;
        logger.atFine().log("SQL: " + sql);
        db.template().execute(sql);
      }
    }
  }
}
