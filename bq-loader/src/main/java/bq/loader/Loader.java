package bq.loader;

import bq.duckdb.DuckDb;
import bq.ducktape.BarSeriesTable;
import bq.ducktape.DuckTape;
import bq.util.MarketCalendar;
import bq.util.Symbol;
import bq.util.Zones;
import bq.util.ta4j.Bars;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;
import org.ta4j.core.BarSeries;

public abstract class Loader<T extends Loader> {

  Symbol symbol;
  BarSeriesTable table;
  DuckDb db;
  DuckTape tape;

  public Loader(DuckDb db) {
    this.db = db;
  }

  public DuckDb getDb() {
    return db;
  }

  public DuckTape getDuckTape() {
    if (tape == null) {
      tape = DuckTape.create(getDb());
    }
    return tape;
  }

  public T symbol(Symbol symbol) {
    this.symbol = symbol;
    return (T) this;
  }

  public T symbol(String symbol) {
    this.symbol = Symbol.parse(symbol);
    return (T) this;
  }

  public Symbol getSymbol() {
    return symbol;
  }

  public T dropTable() {
    if (table != null) {
      db.template().execute("dropp table if exists " + table.getTableName());
    }

    return (T) this;
  }

  public abstract BarSeries loadAll();

  public abstract BarSeries fetch(LocalDate from, LocalDate to);

  public BarSeriesTable getBarSeriesTable() {
    return table;
  }

  LocalDate getLastClosedTradingDay() {
    if (symbol.isCrypto()) {
      return ZonedDateTime.now(Zones.UTC).truncatedTo(ChronoUnit.DAYS).minusDays(1).toLocalDate();

    } else {
      return ZonedDateTime.now(Zones.NYC).truncatedTo(ChronoUnit.DAYS).minusDays(1).toLocalDate();
    }
  }

  Set<LocalDate> getBarDates(BarSeries bs) {
    return Bars.toStream(bs).map(b -> b.getBeginTime().toLocalDate()).collect(Collectors.toSet());
  }

  Set<LocalDate> getExpectedRecentDates(int count) {

    Set<LocalDate> expectedDates = com.google.common.collect.Sets.newHashSet();
    LocalDate lastClosedTradingDay = getLastClosedTradingDay();

    if (symbol.isCrypto()) {
      lastClosedTradingDay =
          ZonedDateTime.now(Zones.UTC).truncatedTo(ChronoUnit.DAYS).minusDays(1).toLocalDate();
    }

    LocalDate fromDate = lastClosedTradingDay.minusDays(count);
    LocalDate d = lastClosedTradingDay;
    for (int i = 0; d.isAfter(fromDate); i++) {

      if (symbol.isCrypto() || MarketCalendar.isTradingDay(d)) {
        expectedDates.add(d);
      }
      d = d.minusDays(1);
    }
    if (!symbol.isCrypto()) {
      expectedDates.remove(LocalDate.of(2025, 1, 9)); // jimmy carter
    }
    return expectedDates;
  }

  public Set<LocalDate> getMissingDates(BarSeries bs) {
    var expected = getExpectedRecentDates(50);
    var actual = getBarDates(bs);

    return com.google.common.collect.Sets.difference(expected, actual);
  }
}
