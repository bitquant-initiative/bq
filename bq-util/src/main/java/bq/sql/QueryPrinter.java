package bq.sql;

import com.google.common.flogger.FluentLogger.Api;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.function.Consumer;

public class QueryPrinter implements Query {

  SqlTemplate template;

  PrintStream streamOutput;
  Api logOutput;

  String message;

  QueryPrinter(SqlTemplate template, OutputStream output) {
    this.template = template;
    this.streamOutput = new PrintStream(output);

    logOutput = null;
  }

  QueryPrinter(SqlTemplate template, Api logOutput) {
    this.template = template;
    this.streamOutput = null;
    this.logOutput = logOutput;
  }

  public QueryPrinter message(String msg) {
    this.message = msg;
    return this;
  }

  public void query(Consumer<StatementBuilder> b) {
    ResultSetProcessor<String> p =
        new ResultSetProcessor<String>() {

          @Override
          public String process(Results rs) throws SQLException {
            String s = new ResultSetTextFormatter().process(rs);
            print(s);
            return s;
          }
        };

    template.queryResult(b, p);
  }

  private void print(String output) {
    if (streamOutput != null) {
      streamOutput.println(output);
      streamOutput.flush();
    }

    if (logOutput != null) {
      logOutput.log("%s\n%s", message != null ? message : "", output);
    }
  }
}
