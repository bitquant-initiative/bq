package bq.loader;

import bq.duckdb.DuckDb;
import bq.loader.coinbase.CoinbaseDataProvider;
import bq.loader.polygon.PolygonDataProvider;
import bq.util.ta4j.Bars;
import bq.util.ta4j.ImmutableBarSeries;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.flogger.FluentLogger;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

public class LoadTask extends Loader<LoadTask> {

  static FluentLogger logger = FluentLogger.forEnclosingClass();

  public LoadTask(DuckDb db) {
    super(db);
  }

  @Override
  public BarSeries loadAll() {
    // TODO Auto-generated method stub
    return null;
  }

  boolean isFullLoadRequired(BarSeries s3Data) {

    if (s3Data.getBarCount() < 100) {
      return true;
    }
    return false;
  }

  boolean isIncrementalLoadRequired(BarSeries s3Data) {

    Set<LocalDate> missing = getMissingDates(s3Data);
    if (!missing.isEmpty()) {
      logger.atInfo().log("missing dates for %s: %s", getSymbol(), missing);
    }

    if (!missing.isEmpty()) {
      return true;
    }

    return false;
  }

  public BarSeries loadFull(BarSeries s3Data) {
    if (s3Data == null) {
      s3Data = ImmutableBarSeries.empty();
    }
    logger.atInfo().log("performing full load for %s", symbol);
    var allDataFromProvider = getDataProvider().loadAll();

    var newBars = Bars.difference(Bars.toList(allDataFromProvider), Bars.toList(s3Data));

    // filter out data for un-closed days (i.e. intraday)
    LocalDate lastClosedTradingDay = getLastClosedTradingDay();
    newBars =
        newBars.stream().filter(Bars.notAfter(lastClosedTradingDay)).collect(Collectors.toSet());

    List<Bar> mergedBars = Lists.newArrayList();

    mergedBars.addAll(Bars.toList(s3Data));
    mergedBars.addAll(newBars);

    Collections.sort(mergedBars, Bars.ascendingDateOrder());

    BarSeries complete = ImmutableBarSeries.of(mergedBars, "updated " + symbol);

    Preconditions.checkState(complete.getBarCount() >= s3Data.getBarCount());

    return complete;
  }

  public BarSeries loadIncremental(BarSeries s3Data) {
    logger.atInfo().log("performing incremental load for %s", symbol);

    var incrementalBarSeriesFromProvider =
        getDataProvider().fetch(LocalDate.now().minusDays(50), getLastClosedTradingDay());

    var newBars =
        Bars.difference(Bars.toList(incrementalBarSeriesFromProvider), Bars.toList(s3Data));
    // filter out data for un-closed days (i.e. intraday)
    LocalDate lastClosedTradingDay = getLastClosedTradingDay();
    newBars =
        newBars.stream().filter(Bars.notAfter(lastClosedTradingDay)).collect(Collectors.toSet());

    List<Bar> mergedBars = Lists.newArrayList();

    mergedBars.addAll(Bars.toList(s3Data));
    mergedBars.addAll(newBars);

    Collections.sort(mergedBars, Bars.ascendingDateOrder());

    BarSeries complete = ImmutableBarSeries.of(mergedBars, "updated " + symbol);

    Preconditions.checkState(complete.getBarCount() >= s3Data.getBarCount());

    return complete;
  }

  public Loader getDataProvider() {

    if (symbol.isStock() || symbol.isIndex()) {
      PolygonDataProvider polygon = new PolygonDataProvider(getDb()).symbol(getSymbol());
      return polygon;
    } else if (symbol.isCrypto()) {
      CoinbaseDataProvider coinbase = new CoinbaseDataProvider(getDb()).symbol(getSymbol());
      return coinbase;
    }

    throw new UnsupportedOperationException("symbol not supported: " + getSymbol());
  }

  public void execute() {
    logger.atInfo().log("********** %s **********", getSymbol());

    var s3Loader = new S3Loader(getDb()).symbol(getSymbol());

    var s3Data = s3Loader.loadAll();

    logger.atInfo().log("s3 data for %s: %s", getSymbol(), s3Data);

    BarSeries completeBarSeries = ImmutableBarSeries.empty();
    if (isFullLoadRequired(s3Data)) {
      completeBarSeries = loadFull(s3Data);
    } else if (isIncrementalLoadRequired(s3Data)) {
      // completeBarSeries should now contain all data

      completeBarSeries = loadIncremental(s3Data);
    } else {
      logger.atInfo().log("no update for %s is required", symbol);
      return;
    }

    if (completeBarSeries == null || completeBarSeries.isEmpty()) {
      // do not write empty data!
      logger.atWarning().log("BarSeries data for %s is null/empty...will not write to S3", symbol);
      return;
    }

    // now do some sanity checks to make sure that we are not about to clobber
    // existing data
    if (completeBarSeries.getBarCount() < s3Data.getBarCount()) {

      logger.atWarning().log("will not write fewer records than were in S3: %s", completeBarSeries);
      return;
    }

    s3Loader.writeS3(completeBarSeries);
  }

  @Override
  public BarSeries fetch(LocalDate from, LocalDate to) {
    return ImmutableBarSeries.of(List.of(), "empty");
  }
}
