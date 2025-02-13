package bq.sql;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.flogger.FluentLogger;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatementBuilder {

  static FluentLogger logger = FluentLogger.forEnclosingClass();
  static final Pattern parsePattern =
      Pattern.compile("(.*?)\\{\\{(.*?)\\}\\}(.*)", Pattern.MULTILINE | Pattern.DOTALL);

  static final Pattern textualSubstitutionPattern =
      Pattern.compile("(.*?)(##(.+?)##)(.*)", Pattern.MULTILINE | Pattern.DOTALL);
  List<String> fragments = Lists.newArrayList();
  int index = 0;
  List<String> paramNames = Lists.newArrayList();

  Map<String, Object> bindings = Maps.newHashMap();

  List<Consumer<Statement>> deferredBindings = Lists.newArrayList();

  private StatementBuilder() {}

  public static StatementBuilder create() {
    return new StatementBuilder();
  }

  public StatementBuilder sql(String sql) {

    return sqlWithBindings(sql, List.of());
  }

  public StatementBuilder sql(String sql, Object v1) {

    return sqlWithBindings(sql, Lists.newArrayList(v1));
  }

  public StatementBuilder sql(String sql, Object v1, Object v2) {

    return sqlWithBindings(sql, Lists.newArrayList(v1, v2));
  }

  public StatementBuilder sql(String sql, Object v1, Object v2, Object v3) {

    return sqlWithBindings(sql, Lists.newArrayList(v1, v2, v3));
  }

  public StatementBuilder sql(String sql, Object v1, Object v2, Object v3, Object v4) {

    return sqlWithBindings(sql, Lists.newArrayList(v1, v2, v3, v4));
  }

  public StatementBuilder bind(int pos, Object val) {
    this.bindings.put(toKey(pos), val);
    return this;
  }

  public StatementBuilder bind(String name, Object val) {
    this.bindings.put(name, val);
    return this;
  }

  public StatementBuilder bind(Consumer<Statement> deferredBinding) {
    Preconditions.checkArgument(deferredBinding != null);
    deferredBindings.add(deferredBinding);
    return this;
  }

  StatementBuilder sqlWithBindings(String s, List<Object> bindVals) {

    if (s == null) {
      s = "";
    }

    if (bindVals == null) {
      bindVals = Lists.newArrayList();
    }

    List<Object> newList = Lists.newArrayList();
    newList.addAll(bindVals);
    bindVals = newList;

    int expectedTotalParamCount = paramNames.size() + bindVals.size();

    Matcher m = parsePattern.matcher(s);
    if (m.matches()) {
      String pre = m.group(1);
      String paramName = m.group(2).trim();
      String post = m.group(3);

      sqlWithBindings(pre, null);
      if (bindVals.isEmpty()) {
        addParam(paramName);
      } else {

        addParam(paramName, bindVals.removeFirst());
      }

      sqlWithBindings(post, bindVals);
    } else {
      fragments.add(s);
    }

    if (expectedTotalParamCount != paramNames.size()) {
      if (bindVals.size() == 0) {
        // if no positional parameters were provided, it is not a problem
        // that there are unbound parameters. They may be bound at a later time.
      } else {
        // BUT, if positional parameters were supplied, they need to match
        throw new DbException(
            String.format(
                "expected %d bind values but got %d sql=<%s>",
                expectedTotalParamCount - bindVals.size(), bindVals.size(), s));
      }
    }

    return this;
  }

  private String toKey(int index) {
    return String.format("_%s", index);
  }

  private void addParam(String name) {
    paramNames.add(name);
    fragments.add("?");
  }

  private void addParam(String name, Object orderedVal) {

    addParam(name);

    bindings.put(toKey(paramNames.size()), orderedVal);
  }

  public String getSql() {
    String sql = "";
    for (String fragment : fragments) {
      boolean addBlank = true;

      fragment = fragment.trim();
      if (sql.isBlank() || sql.endsWith(" ") || fragment.startsWith(" ")) {
        addBlank = false;
      } else if (fragment.startsWith("?")) {
        if (sql.endsWith("=") || sql.endsWith(",")) {
          addBlank = false;
        }
      } else if (sql.endsWith("?")) {
        if (fragment.startsWith(" ")) {}
      }

      if (addBlank) {
        sql += " ";
      }
      sql += fragment;
    }
    sql = interpolate(sql);
    sql = sql.trim();

    return sql;
  }

  String interpolate(final String sql) {
    Matcher m = textualSubstitutionPattern.matcher(sql);

    if (m.matches()) {
      // example: select * from ##table## where ##col##=1

      // group 1 will contain the text before the marker: "select * from "
      // group 2 will contain the marker: ##table##
      // group 3 will contain the text inside the marker: table
      // group 4 will contain the text after the marker " where ##col##=1"

      Object subVal = this.bindings.get(m.group(3));
      if (subVal == null) {
        //
        String msg = String.format("unbound token: %s", m.group(2));

        // Do not attach the SQL to the exception since it could have sensitive data
        DbException ex = new DbException(msg);
        // this will probably end up getting logged twice, but it is nice to have the
        // SQL and the
        // call
        // site together
        logger.atWarning().withCause(ex).log("%s - %s", msg, sql);
        throw ex;
      }

      String interpolatedValue = subVal.toString();

      return m.group(1) + interpolatedValue + interpolate(m.group(4));
    }

    return sql;
  }

  public StatementBuilder bind(PreparedStatement ps) throws SQLException {
    for (int i = 0; i < paramNames.size(); i++) {
      int index = i + 1;
      String paramName = paramNames.get(i);

      Object val = null;

      if (bindings.containsKey(paramName)) {
        val = bindings.get(paramName);
      } else {
        val = bindings.get(toKey(index));
      }

      Object convertedVal = SqlUtil.toSqlBindType(val);

      ps.setObject(index, convertedVal);

      // now apply and deferred bindings that might be set
      for (Consumer<Statement> deferred : deferredBindings) {

        deferred.accept(ps);
      }
    }

    return this;
  }
}
