package bq.loader;

import bq.duckdb.DuckDb;
import bq.util.Symbol;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.flogger.FluentLogger;
import java.util.List;

public class MultiLoadTask {

  static FluentLogger logger = FluentLogger.forEnclosingClass();
  DuckDb db;
  List<Symbol> symbols = Lists.newArrayList();
  boolean updateAll = true;

  public MultiLoadTask(DuckDb db) {
    this.db = db;
  }

  public MultiLoadTask symbol(Symbol symbol) {
    this.symbols.add(symbol);
    return this;
  }

  public MultiLoadTask all(boolean all) {
    this.updateAll = all;
    return this;
  }

  public MultiLoadTask symbol(String s) {
    return symbols(s);
  }

  public MultiLoadTask symbols(String symbols) {

    if (symbols == null) {
      return this;
    }
    Splitter.on(CharMatcher.anyOf(" ,\t\n\r"))
        .trimResults()
        .omitEmptyStrings()
        .splitToList(symbols)
        .forEach(
            s -> {
              this.symbols.add(Symbol.parse(s));
            });

    return this;
  }

  public void execute() {

    S3Loader s3Loader = new S3Loader(db);
    if (!s3Loader.isS3Available()) {
      logger.atWarning().log("cannot connect to s3");
      return;
    }

    if (updateAll) {
      s3Loader
          .fetchSymbolsForUpdate()
          .forEach(
              it -> {
                symbol(it);
              });
    }

    List<Symbol> symbols = Lists.newArrayList(this.symbols);

    symbols.stream()
        .forEach(
            symbol -> {
              try {
                new LoadTask(db).symbol(symbol).execute();
              } catch (RuntimeException e) {
                logger.atWarning().withCause(e).log("problem processing %s", symbol);
              }
            });
  }
}
