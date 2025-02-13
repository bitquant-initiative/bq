package bq.duckdb;

import bq.sql.DbException;
import bq.sql.SqlCloser;
import bq.sql.SqlTemplate;
import bq.util.BqException;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Suppliers;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Supplier;
import org.duckdb.DuckDBConnection;

/**
 * DuckTable is a convenient way to pass around tabular data.
 */
public class DuckDb implements AutoCloseable {

  static Supplier<DuckDb> supplier = Suppliers.memoize(DuckDb::createInMemory);
  Supplier<DuckConnection> connectionSupplier;

  String metadataUrl; // only used to report on toString
  boolean metadataReadOnly; // only used to report on toString
  boolean metadataShared; // only used to report on toString

  S3Extension s3;

  public static DuckDb getSharedInMemory() {
    DuckDb db = supplier.get();
    db.metadataShared = true;
    return db;
  }

  public static DuckDb createInMemory() {
    return create("jdbc:duckdb:");
  }

  private DuckDb initSession() {

    try (SqlCloser closer = SqlCloser.create()) {
      Connection c = getConnection();
      this.metadataUrl = c.getMetaData().getURL();
      this.metadataReadOnly = c.getMetaData().isReadOnly();
    } catch (SQLException e) {
      throw new BqException(e);
    }

    template().execute("set TimeZone='UTC'");

    return this;
  }

  public static DuckDb create(File f) {
    try {
      Connection c = DriverManager.getConnection("jdbc:duckdb:" + f.getCanonicalPath());
      DuckDb d = create(c).initSession();

      return d;
    } catch (SQLException | IOException e) {
      throw new DbException(e);
    }
  }

  public static DuckDb create(Supplier<DuckConnection> supplier) {

    if (supplier == null) {
      throw new DbException("connection supplier cannot be null or empty");
    }
    DuckDb t = new DuckDb();

    t.connectionSupplier = supplier;

    t.initSession();
    return t;
  }

  public static DuckDb create(String url) {
    try {
      Connection c = DriverManager.getConnection(url);
      return create(c).initSession();
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public static DuckDb create(DuckConnection c) {
    return create(Suppliers.ofInstance(c));
  }

  public static DuckDb create(Connection c) {

    if (c == null) {
      throw new DbException("connection cannot be null or empty");
    }
    try {
      if (c instanceof DuckDBConnection) {
        DuckDb d = create(DuckConnection.wrap((DuckDBConnection) c));
        return d;
      }
      if (c instanceof DuckConnection) {
        return create((DuckConnection) c);
      }
      if (c.isWrapperFor(DuckDBConnection.class)) {
        DuckDb d = create(c.unwrap(DuckDBConnection.class));
        return d;
      }
    } catch (SQLException e) {
      throw new DbException(e);
    }
    throw new DbException("unsupported connection: " + c);
  }

  public DuckConnection getConnection() {
    return connectionSupplier.get();
  }

  /**
   * Obtain a DuckTable instance, which is simply a reference to a table that may
   * or may not exist.
   *
   * @param table
   * @return
   */
  public DuckTable table(String table) {

    return new DuckTable(this, table);
  }

  public Operations operations() {
    return new Operations(this);
  }

  public List<String> getTableNames() {
    return table("dummy").getTableNames();
  }

  public S3Extension s3Extension() {
    if (s3 == null) {

      S3Extension x = new S3Extension(this);
      x.loadExtension();
      this.s3 = x;
    }
    return s3;
  }

  public SqlTemplate template() {
    return SqlTemplate.create(Suppliers.ofInstance(getConnection()));
  }

  public void close() {
    this.getConnection().dispose();
  }

  public String toString() {

    ToStringHelper h =
        MoreObjects.toStringHelper(this)
            .add("@", Integer.toHexString(hashCode()))
            .add("url", metadataUrl);
    if (metadataShared == true) {
      h.add("shared", metadataShared);
    }
    if (metadataReadOnly) {
      h.add("readOnly", metadataReadOnly);
    }
    return h.toString();
  }
}
