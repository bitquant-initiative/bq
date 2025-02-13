package bq.duckdb;

import bq.sql.DbException;
import com.google.common.base.MoreObjects;
import com.google.common.flogger.FluentLogger;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.ShardingKey;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBConnection;

/**
 * DuckConnection is a wrapper for a DuckDBConnection that protects against
 * accidental Connection closure.
 *
 */
public class DuckConnection implements Connection {

  FluentLogger logger = FluentLogger.forEnclosingClass();

  boolean protectedFromClose = true;

  DuckDBConnection duck;

  public static DuckConnection wrap(DuckDBConnection c) {
    DuckConnection wrapper = new DuckConnection();
    wrapper.duck = c;
    wrapper.protectedFromClose = true;
    return wrapper;
  }

  public DuckDBAppender createAppender(String schema, String table) throws SQLException {
    return duck.createAppender(schema, table);
  }

  public void dispose() {

    try {
      if (isSharedInMemory()) {
        throw new DbException("shared in-memory connections cannot be closed");
      }
      duck.close();
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public <T> T unwrap(Class<T> iface) throws SQLException {
    return duck.unwrap(iface);
  }

  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    if (iface == null) {
      return false;
    }
    if (iface.isInstance(duck)) {
      return true;
    }
    return duck.isWrapperFor(iface);
  }

  public Statement createStatement() throws SQLException {
    return duck.createStatement();
  }

  public PreparedStatement prepareStatement(String sql) throws SQLException {
    return duck.prepareStatement(sql);
  }

  public CallableStatement prepareCall(String sql) throws SQLException {
    return duck.prepareCall(sql);
  }

  public String nativeSQL(String sql) throws SQLException {
    return duck.nativeSQL(sql);
  }

  public void setAutoCommit(boolean autoCommit) throws SQLException {
    duck.setAutoCommit(autoCommit);
  }

  public boolean getAutoCommit() throws SQLException {
    return duck.getAutoCommit();
  }

  public void commit() throws SQLException {
    duck.commit();
  }

  public void rollback() throws SQLException {
    duck.rollback();
  }

  public boolean isSharedInMemory() {
    return DuckDb.getSharedInMemory().getConnection() == this;
  }

  public void close() throws SQLException {
    if (protectedFromClose || isSharedInMemory()) {

      logger.atFiner().log("%s is protected from closure", duck);
      return;
    } else {
      logger.atFiner().log("closing %s", duck);
      duck.close();
    }
  }

  public DuckDBConnection getWrappedConnection() {
    return duck;
  }

  public boolean isClosed() throws SQLException {
    return duck.isClosed();
  }

  public DatabaseMetaData getMetaData() throws SQLException {
    return duck.getMetaData();
  }

  public void setReadOnly(boolean readOnly) throws SQLException {
    duck.setReadOnly(readOnly);
  }

  public boolean isReadOnly() throws SQLException {
    return duck.isReadOnly();
  }

  public void setCatalog(String catalog) throws SQLException {
    duck.setCatalog(catalog);
  }

  public String getCatalog() throws SQLException {
    return duck.getCatalog();
  }

  public void setTransactionIsolation(int level) throws SQLException {
    duck.setTransactionIsolation(level);
  }

  public int getTransactionIsolation() throws SQLException {
    return duck.getTransactionIsolation();
  }

  public SQLWarning getWarnings() throws SQLException {
    return duck.getWarnings();
  }

  public void clearWarnings() throws SQLException {
    duck.clearWarnings();
  }

  public Statement createStatement(int resultSetType, int resultSetConcurrency)
      throws SQLException {
    return duck.createStatement(resultSetType, resultSetConcurrency);
  }

  public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
      throws SQLException {
    return duck.prepareStatement(sql, resultSetType, resultSetConcurrency);
  }

  public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
      throws SQLException {
    return duck.prepareCall(sql, resultSetType, resultSetConcurrency);
  }

  public Map<String, Class<?>> getTypeMap() throws SQLException {
    return duck.getTypeMap();
  }

  public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
    duck.setTypeMap(map);
  }

  public void setHoldability(int holdability) throws SQLException {
    duck.setHoldability(holdability);
  }

  public int getHoldability() throws SQLException {
    return duck.getHoldability();
  }

  public Savepoint setSavepoint() throws SQLException {
    return duck.setSavepoint();
  }

  public Savepoint setSavepoint(String name) throws SQLException {
    return duck.setSavepoint(name);
  }

  public void rollback(Savepoint savepoint) throws SQLException {
    duck.rollback(savepoint);
  }

  public void releaseSavepoint(Savepoint savepoint) throws SQLException {
    duck.releaseSavepoint(savepoint);
  }

  public Statement createStatement(
      int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
    return duck.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
  }

  public PreparedStatement prepareStatement(
      String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLException {
    return duck.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
  }

  public CallableStatement prepareCall(
      String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
      throws SQLException {
    return duck.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
  }

  public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
    return duck.prepareStatement(sql, autoGeneratedKeys);
  }

  public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
    return duck.prepareStatement(sql, columnIndexes);
  }

  public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
    return duck.prepareStatement(sql, columnNames);
  }

  public Clob createClob() throws SQLException {
    return duck.createClob();
  }

  public Blob createBlob() throws SQLException {
    return duck.createBlob();
  }

  public NClob createNClob() throws SQLException {
    return duck.createNClob();
  }

  public SQLXML createSQLXML() throws SQLException {
    return duck.createSQLXML();
  }

  public boolean isValid(int timeout) throws SQLException {
    return duck.isValid(timeout);
  }

  public void setClientInfo(String name, String value) throws SQLClientInfoException {
    duck.setClientInfo(name, value);
  }

  public void setClientInfo(Properties properties) throws SQLClientInfoException {
    duck.setClientInfo(properties);
  }

  public String getClientInfo(String name) throws SQLException {
    return duck.getClientInfo(name);
  }

  public Properties getClientInfo() throws SQLException {
    return duck.getClientInfo();
  }

  public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
    return duck.createArrayOf(typeName, elements);
  }

  public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
    return duck.createStruct(typeName, attributes);
  }

  public void setSchema(String schema) throws SQLException {
    duck.setSchema(schema);
  }

  public String getSchema() throws SQLException {
    return duck.getSchema();
  }

  public void abort(Executor executor) throws SQLException {
    duck.abort(executor);
  }

  public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
    duck.setNetworkTimeout(executor, milliseconds);
  }

  public int getNetworkTimeout() throws SQLException {
    return duck.getNetworkTimeout();
  }

  public void beginRequest() throws SQLException {
    duck.beginRequest();
  }

  public void endRequest() throws SQLException {
    duck.endRequest();
  }

  public boolean setShardingKeyIfValid(
      ShardingKey shardingKey, ShardingKey superShardingKey, int timeout) throws SQLException {
    return duck.setShardingKeyIfValid(shardingKey, superShardingKey, timeout);
  }

  public boolean setShardingKeyIfValid(ShardingKey shardingKey, int timeout) throws SQLException {
    return duck.setShardingKeyIfValid(shardingKey, timeout);
  }

  public void setShardingKey(ShardingKey shardingKey, ShardingKey superShardingKey)
      throws SQLException {
    duck.setShardingKey(shardingKey, superShardingKey);
  }

  public void setShardingKey(ShardingKey shardingKey) throws SQLException {
    duck.setShardingKey(shardingKey);
  }

  public String toString() {

    boolean closed = false;
    try {
      closed = isClosed();
    } catch (SQLException e) {
      closed = true;
    }

    return MoreObjects.toStringHelper("DuckConnection")
        .add("wrapped", duck)
        .add("protected", protectedFromClose)
        .add("closed", closed)
        .toString();
  }
}
