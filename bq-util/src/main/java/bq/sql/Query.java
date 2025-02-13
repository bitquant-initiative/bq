package bq.sql;

import java.util.function.Consumer;

/**
 * Query provides a contract for use-cases in which the response and output
 * is handled downstream.
 */
public interface Query {
  public void query(Consumer<StatementBuilder> c);

  public default void query(String sql) {
    query(c -> c.sql(sql));
  }

  public default void query(String sql, Object p1) {
    query(c -> c.sql(sql).bind(1, p1));
  }

  public default void query(String sql, Object p1, Object p2) {
    query(c -> c.sql(sql).bind(1, p1).bind(2, p2));
  }
}
