package bq.sql;

import com.google.common.base.Suppliers;
import com.google.common.flogger.FluentLogger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SqlTemplateTest {

  FluentLogger logger = FluentLogger.forEnclosingClass();

  SqlTemplate template = null;

  @Test
  public void testOmitNullRows() {

    Assertions.assertThat(template.isOmitNullRowsEnabled()).isFalse();

    var list =
        template
            .query(
                "select null as name",
                rs -> {
                  return (String) null;
                })
            .toList();

    Assertions.assertThat(list).hasSize(1);
    Assertions.assertThat(list.getFirst()).isNull();

    list =
        template
            .query(
                "select null as name",
                rs -> {
                  return (String) null;
                })
            .toList();
    Assertions.assertThat(list).hasSize(1);
    Assertions.assertThat(list.getFirst()).isNull();
  }

  @Test
  public void testFoo() {

    template.execute("create table test (name varchar(10), age int)");

    template.update("insert into test (name,age) values ({{name}}, {{age}})", "homer", 7);
    template.update("insert into test (name,age) values ({{name}}, {{age}})", "rosie", 3);

    Assertions.assertThat(
            template.queryInt(c -> c.sql("select age from test where name={{name}}", "homer")))
        .isEqualTo(7);
    Assertions.assertThat(
            template.queryInt(
                c -> c.sql("select age from test where name={{name}}").bind(1, "homer")))
        .isEqualTo(7);
    Assertions.assertThat(
            template.queryInt(
                c -> c.sql("select age from test where name={{name}}").bind("name", "homer")))
        .isEqualTo(7);

    template
        .query(
            c -> c.sql("select age from test"),
            rs -> {
              return rs.getDouble("age").get();
            })
        .forEach(
            it -> {
              // this is performing an important type test...do not delete
              double x = it;
            });
  }

  void checkExpected(Consumer<SqlTemplate> c) {
    try {
      c.accept(template);
      Assertions.failBecauseExceptionWasNotThrown(DbException.class);
    } catch (DbException e) {

    }
  }

  @Test
  public void testPrint() {
    template.execute("create table employee (id bigint, name varchar(50))");
    template.execute("insert into employee (id,name) values (1000,'Jerry')");

    template.log().query("select * from employee");
    template.print().query("select * from employee");
  }

  @Test
  public void testNoRows() {

    template.execute("create table test (name varchar(10), age int)");

    checkExpected(
        t -> {
          template.queryInt(c -> c.sql("select age from test where name={{name}}", "homer"));
        });

    Assertions.assertThat(
            template.query(c -> c.sql("select * from test"), rs -> rs.getString(1)).toList())
        .isEmpty();
  }

  ////////
  ////////
  ////////

  List<Connection> connections = Lists.newArrayList();

  SqlTemplate getTemplate() {

    if (template != null) {
      return template;
    }
    try {
      Connection c = DriverManager.getConnection("jdbc:duckdb:");

      connections.add(c);

      return SqlTemplate.create(Suppliers.ofInstance(c));

    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testExec() {
    String val =
        template.executeContext(
            c -> {
              Assertions.assertThat(c.getConnection()).isNotNull();

              return "something";
            });

    Assertions.assertThat(val).isEqualTo("something");
  }

  @BeforeEach
  private void setup() {
    template = getTemplate();
  }

  @AfterEach
  private void cleanup() {
    template = null;
    for (Connection c : connections) {
      try {
        c.close();
      } catch (SQLException e) {
        logger.atWarning().withCause(e).log("connectin.close() failed");
      }
    }
  }
}
