package bq.sql;

import com.google.common.collect.Lists;
import com.google.common.flogger.FluentLogger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class SqlCloser implements AutoCloseable {

  static FluentLogger logger = FluentLogger.forEnclosingClass();

  List<Connection> connections = Lists.newArrayList();
  List<Statement> statements = Lists.newArrayList();
  List<ResultSet> resultSets = Lists.newArrayList();
  List<Throwable> exceptions = Lists.newArrayList();

  public static SqlCloser create() {
    return new SqlCloser();
  }

  @Override
  public void close() {

    for (ResultSet rs : resultSets.reversed()) {
      try {
        logger.atFinest().log("closing %s", rs);
        rs.close();
      } catch (RuntimeException | SQLException e) {
        exceptions.add(e);
      }
    }

    for (Statement st : statements.reversed()) {
      try {
        logger.atFinest().log("closing %s", st);
        st.close();
      } catch (RuntimeException | SQLException e) {
        exceptions.add(e);
      }
    }

    for (Connection c : connections.reversed()) {
      try {

        if (c.getClass().getName().contains("duckdb")) {
          // special case...DuckDB connections don't want to be closed
        } else {

          logger.atFinest().log("closing %s", c);
          c.close();
        }

      } catch (RuntimeException | SQLException e) {
        exceptions.add(e);
      }
    }

    exceptions.forEach(
        t -> {
          logger.atWarning().withCause(t).log("problem closing resource");
        });

    if (!exceptions.isEmpty()) {
      Throwable t = exceptions.get(0);
      if (t instanceof RuntimeException) {
        RuntimeException e = ((RuntimeException) t);
        throw e;
      } else {
        throw new DbException(t);
      }
    }
  }

  public void register(Results rs) {
    register(rs.getResultSet());
  }

  public void register(Connection c) {
    connections.add(c);
  }

  public void register(Statement st) {
    statements.add(st);
  }

  public void register(ResultSet rs) {
    resultSets.add(rs);
  }
}
