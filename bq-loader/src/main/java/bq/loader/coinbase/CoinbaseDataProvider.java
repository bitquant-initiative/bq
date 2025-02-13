package bq.loader.coinbase;

import bq.duckdb.DuckDb;
import bq.loader.Loader;
import bq.util.BqException;
import bq.util.Zones;
import bq.util.ta4j.Bars;
import bq.util.ta4j.ImmutableBar;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.RateLimiter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

public class CoinbaseDataProvider extends Loader<CoinbaseDataProvider> {

  FluentLogger logger = FluentLogger.forEnclosingClass();

  RateLimiter limiter = RateLimiter.create(5d);

  static final int MAX_CANDLES_PER_REQUEST = 250;

  public CoinbaseDataProvider(DuckDb db) {
    super(db);
  }

  String getCoinbaseSymbol() {

    String asset = getSymbol().getTicker();
    asset = asset.toUpperCase().trim();

    if (!asset.endsWith("USD")) {
      asset = asset + "USD";
    }

    if (asset.endsWith("USD") && !asset.endsWith("-USD")) {
      asset = asset.replace("USD", "-USD");
    }

    return asset;
  }

  public List<Bar> fetchAll() {
    List<Bar> list = Lists.newArrayList();

    LocalDate to = LocalDate.now();

    LocalDate from = to.minus(250, ChronoUnit.DAYS);
    List<Bar> batch = Lists.newArrayList();
    do {
      batch = getBars(from, to);
      list.addAll(batch);
      to = from;
      from = to.minusDays(250);

    } while (!batch.isEmpty());

    Collections.sort(list, Bars.ascendingDateOrder());

    return list;
  }

  public List<Bar> getBars(LocalDate fromDate, LocalDate toDate) {

    if (toDate == null) {
      toDate = LocalDate.now();
    }
    logger.atInfo().log("getBars(%s, %s)", fromDate, toDate);
    ZonedDateTime from = fromDate.atStartOfDay(Zones.UTC);
    ZonedDateTime to = toDate.atStartOfDay(Zones.UTC);

    // t0 and t1 will be the start and end period of our request window
    ZonedDateTime t1 = to;
    ZonedDateTime t0 = from;
    int requestCount = 0;
    List<Bar> records = Lists.newArrayList();
    boolean hasMore = true;
    do {
      // coinbase has a limite of 250 bars per request
      ZonedDateTime t0Limit = t1.minusDays(MAX_CANDLES_PER_REQUEST);
      if (t0Limit.isAfter(t0)) {
        t0 = t0Limit;
      }
      requestCount++;
      String url =
          String.format(
              "https://api.coinbase.com/api/v3/brokerage/market/products/%s/candles?granularity=ONE_DAY&start=%s&end=%s",
              getCoinbaseSymbol(), t0.toEpochSecond(), t1.toEpochSecond());
      limiter.acquire();
      int responseCandleCount = 0; // # of candles in the response
      HttpResponse<JsonNode> response = null;
      try {
        response = Unirest.get(url).asObject(JsonNode.class);
        if (response.isSuccess()) {
          JsonNode n = response.getBody();
          responseCandleCount = n.path("candles").size();

          n.path("candles")
              .forEach(
                  candle -> {
                    long ts = candle.path("start").asLong();
                    Instant instant = Instant.ofEpochSecond(ts);
                    LocalDate date = ZonedDateTime.ofInstant(instant, Zones.UTC).toLocalDate();
                    Double open = candle.path("open").asDouble();
                    Double high = candle.path("high").asDouble();
                    Double low = candle.path("low").asDouble();
                    Double close = candle.path("close").asDouble();
                    Double volume = candle.path("volume").asDouble();

                    Bar bar = ImmutableBar.create(date, open, high, low, close, volume);

                    records.add(bar);
                  });
        } else {
          throw new BqException(
              "Coinbase rc=" + response.getStatus() + " message=" + response.getStatusText());
        }
      } finally {
        logger.atInfo().log(
            "GET %s %s to %s %s status=%s",
            getSymbol(),
            t0.toLocalDate(),
            t1.toLocalDate(),
            url,
            response != null ? response.getStatus() : "");
      }

      if (responseCandleCount == 0 || (!t0.isAfter(from))) {
        hasMore = false;
      } else {
        t1 = t0.minusDays(1);
        t0 = t0.minusDays(250);
        if (t0.isBefore(from)) {
          t0 = from;
        }
      }

      // add some sanity limits to make sure we don't end up in an infinite loop
      if (toDate.isBefore(LocalDate.of(2009, 1, 3)) || requestCount++ > 30) {
        hasMore = false;
      }
    } while (hasMore && requestCount < 30);

    Collections.sort(records, Bars.ascendingDateOrder());

    for (int i = 0; i < records.size(); i++) {
      if (i > 0) {
        Preconditions.checkArgument(
            records.get(i - 1).getBeginTime().plusDays(1).isEqual(records.get(i).getBeginTime()),
            "bars should be contiguous");
      }
    }

    return records;
  }

  @Override
  public BarSeries loadAll() {
    List<Bar> bars = getBars(LocalDate.of(2012, 1, 1), LocalDate.now());
    return Bars.toBarSeries(bars, String.format("%s from Coinbase", getSymbol()));
  }

  @Override
  public BarSeries fetch(LocalDate from, LocalDate to) {
    List<Bar> bars = getBars(from, to);

    return Bars.toBarSeries(bars, String.format("%s from Coinbase", getSymbol()));
  }
}
