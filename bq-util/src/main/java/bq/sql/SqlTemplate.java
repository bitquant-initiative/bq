package bq.sql;

import bq.sql.mapper.Mappers;
import bq.util.BqException;
import bq.util.SqlExecutionFunction;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.FluentLogger.Api;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.sql.DataSource;

public class SqlTemplate {

  FluentLogger logger = FluentLogger.forEnclosingClass();
  Supplier<Connection> connectionSupplier = null;

  static Supplier<Connection> globalSupplier = null;

  private boolean omitNullRows = false;

  static boolean globalDataaSouceDisabled = false;

  public static void disableGlobalDataSource() {
    globalDataaSouceDisabled = true;
  }

  public static SqlTemplate create(Supplier<Connection> cs) {
    Preconditions.checkNotNull(cs);
    SqlTemplate t = new SqlTemplate();
    t.connectionSupplier = cs;
    return t;
  }

  public static SqlTemplate create(Connection c) {
    Preconditions.checkNotNull(c);
    return create(Suppliers.ofInstance(c));
  }

  public static SqlTemplate create(DataSource ds) {
    Preconditions.checkNotNull(ds);

    Supplier<Connection> cs =
        new Supplier<Connection>() {

          @Override
          public Connection get() {
            try {
              return ds.getConnection();
            } catch (SQLException e) {
              throw new DbException(e);
            }
          }
        };
    return create(cs);
  }

  private static Supplier<Connection> supplier(DataSource ds) {
    Supplier<Connection> cs =
        new Supplier<Connection>() {

          @Override
          public Connection get() {
            try {
              return ds.getConnection();
            } catch (SQLException e) {
              throw new DbException(e);
            }
          }
        };
    return cs;
  }

  public static void setGlobalDataSource(DataSource ds) {
    Preconditions.checkState(globalSupplier == null);
    globalSupplier = supplier(ds);
  }

  public static void setGlobalDataSource(Supplier<Connection> ds) {
    Preconditions.checkState(globalSupplier == null);
    globalSupplier = ds;
  }

  public static SqlTemplate create() {
    Preconditions.checkState(
        globalDataaSouceDisabled == false, "global DataSource has been disabled");
    Preconditions.checkState(globalSupplier != null, "global DataSource not set");
    return SqlTemplate.create(globalSupplier);
  }

  private Connection getConnection() {
    return this.connectionSupplier.get();
  }

  public QueryPrinter print() {
    return new QueryPrinter(this, System.out);
  }

  public QueryPrinter log() {
    return new QueryPrinter(this, logger.atFine());
  }

  public QueryPrinter log(Api log) {
    return new QueryPrinter(this, log);
  }

  public String queryString(Consumer<StatementBuilder> builder) {
    Optional<String> r =
        query(
                builder,
                m -> {
                  Optional<String> v = m.getString(1);
                  if (v.isEmpty()) {
                    return null;
                  }
                  return v.get();
                })
            .findFirst();
    if (r.isEmpty()) {
      throw new DbException("queryString expected a result");
    }
    return r.get();
  }

  public int queryInt(Consumer<StatementBuilder> builder) {
    Optional<Integer> r =
        query(
                builder,
                m -> {
                  Optional<Integer> v = m.getInt(1);
                  if (v.isEmpty()) {
                    return 0;
                  }
                  return (int) v.get();
                })
            .findFirst();
    if (r.isEmpty()) {
      throw new DbException("queryInt expected a result");
    }
    return r.get();
  }

  public <T> T queryResult(Consumer<StatementBuilder> builder, ResultSetProcessor<T> processor) {
    Preconditions.checkNotNull(builder, "builder cannot be null");
    Preconditions.checkNotNull(processor, "ResultSetProcessor cannot be null");
    StatementBuilder b = StatementBuilder.create();

    builder.accept(b);

    String sql = b.getSql();

    try (SqlCloser closer = SqlCloser.create()) {
      Connection c = getConnection();
      closer.register(c);

      PreparedStatement ps = c.prepareStatement(sql);
      closer.register(ps);

      b.bind(ps);

      ResultSet rs = ps.executeQuery();
      Results rsx = Results.create(rs);
      return (T) processor.process(rsx);
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public Stream<ObjectNode> queryJson(Consumer<StatementBuilder> builder) {
    return query(builder, Mappers.jsonObjectMapper());
  }

  public <T> Stream<T> query(String sql, RowMapper<T> mapper) {
    return query(c -> c.sql(sql), mapper);
  }

  public <T> Stream<T> query(String sql, Object o1, RowMapper<T> mapper) {
    return query(c -> c.sql(sql).bind(1, o1), mapper);
  }

  public <T> Stream<T> query(String sql, Object o1, Object o2, RowMapper<T> mapper) {
    return query(c -> c.sql(sql).bind(1, o1).bind(2, o2), mapper);
  }

  public <T> Stream<T> query(Consumer<StatementBuilder> builder, RowMapper<T> mapper) {

    Preconditions.checkNotNull(builder, "builder cannot be null");
    Preconditions.checkNotNull(mapper, "mapper cannot be null");

    StatementBuilder b = StatementBuilder.create();

    builder.accept(b);

    String sql = b.getSql();

    try (SqlCloser closer = SqlCloser.create()) {
      Connection c = getConnection();
      closer.register(c);
      List<T> results = Lists.newArrayList();
      PreparedStatement ps = c.prepareStatement(sql);
      closer.register(ps);

      b.bind(ps);

      ResultSet rs = ps.executeQuery();
      closer.register(rs);
      Results r = Results.create(rs);
      while (r.next()) {
        T t = (T) mapper.map(r);
        if (omitNullRows && t == null) {
          // do not add
        } else {
          results.add(t);
        }
        ;
      }
      return results.stream();
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public boolean execute(Consumer<StatementBuilder> builder) {
    Preconditions.checkNotNull(builder);
    StatementBuilder b = StatementBuilder.create();

    builder.accept(b);

    String sql = b.getSql();

    try (SqlCloser closer = SqlCloser.create()) {
      Connection c = getConnection();
      closer.register(c);

      PreparedStatement ps = c.prepareStatement(sql);
      closer.register(ps);

      b.bind(ps);

      return ps.execute();
    } catch (SQLException e) {
      throw new DbException(e);
    }
  }

  public int update(Consumer<StatementBuilder> builder) {
    Preconditions.checkNotNull(builder);
    StatementBuilder b = StatementBuilder.create();

    builder.accept(b);

    String sql = b.getSql();

    return executeContext(
        f -> {
          PreparedStatement ps = f.getConnection().prepareStatement(sql);
          f.register(ps);
          b.bind(ps);

          return ps.executeUpdate();
        });
  }

  ////////
  ////////
  ////////
  ///
  ///

  public boolean execute(String sql) {
    return execute(
        c -> {
          c.sql(sql);
        });
  }

  public boolean execute(String sql, Object v0) {
    return execute(
        c -> {
          c.sql(sql);
          c.bind(1, v0);
        });
  }

  public boolean execute(String sql, Object v1, Object v2) {
    return execute(
        c -> {
          c.sql(sql);
          c.bind(1, v1);
          c.bind(2, v2);
        });
  }

  public int update(String sql) {

    return update(
        b -> {
          b.sql(sql);
        });
  }

  public int update(String sql, Object v1) {

    return update(
        b -> {
          b.sql(sql);
          b.bind(1, v1);
        });
  }

  public int update(String sql, Object v1, Object v2) {

    return update(
        b -> {
          b.sql(sql);
          b.bind(1, v1);
          b.bind(2, v2);
        });
  }

  boolean isOmitNullRowsEnabled() {
    return this.omitNullRows;
  }

  public SqlTemplate omitNullRows() {
    this.omitNullRows = true;
    return this;
  }

  public <T> T executeContext(SqlExecutionFunction<T> c) {

    try (SqlExecutionContext ctx = new SqlExecutionContext(getConnection())) {

      return (T) c.apply(ctx);

    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new BqException(e);
    }
  }
}
