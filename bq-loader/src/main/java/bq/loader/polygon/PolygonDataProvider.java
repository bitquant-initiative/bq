package bq.loader.polygon;

import bq.duckdb.DuckDb;
import bq.loader.Loader;
import bq.loader.Throttle;
import bq.util.Config;
import bq.util.Zones;
import bq.util.ta4j.Bars;
import bq.util.ta4j.ImmutableBar;
import bq.util.ta4j.ImmutableBarSeries;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.flogger.FluentLogger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;

public class PolygonDataProvider extends Loader<PolygonDataProvider> {

  Throttle throttle = Throttle.rpm(10);
  FluentLogger logger = FluentLogger.forEnclosingClass();

  public PolygonDataProvider(DuckDb db) {
    super(db);
  }

  Optional<Double> asDouble(JsonNode n, String f) {
    String s = n.path(f).asText(null);
    if (s == null) {
      return Optional.empty();
    }
    try {
      BigDecimal bd = new BigDecimal(s);
      bd = bd.setScale(2, RoundingMode.HALF_UP);
      return Optional.of(bd.doubleValue());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  Bar toBar(JsonNode record) {

    ZonedDateTime dt =
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(record.path("t").asLong()), Zones.UTC);
    LocalDate d = dt.toLocalDate();

    return ImmutableBar.create(
        d,
        asDouble(record, "o").orElse(null),
        asDouble(record, "h").orElse(null),
        asDouble(record, "l").orElse(null),
        asDouble(record, "c").orElse(null),
        asDouble(record, "v").orElse(null));
  }

  List<Bar> toBars(JsonNode response) {

    List<Bar> bars = Lists.newArrayList();
    response
        .path("results")
        .forEach(
            n -> {
              Bar b = toBar(n);
              bars.add(b);
            });
    return bars;
  }

  public BarSeries fetch(LocalDate from, LocalDate to) {
    if (to == null) {
      to = LocalDate.now();
    }
    if (from == null) {
      from = LocalDate.now().minus(30, ChronoUnit.DAYS);
    }
    String fixedSymbol = getSymbol().toString();
    if (fixedSymbol.equals("I:DJIA")) {
      fixedSymbol = "DJIA";
    }
    if (fixedSymbol.startsWith("S:")) {
      fixedSymbol = fixedSymbol.replace("S:", "");
    }
    throttle.acquire();
    String url =
        String.format(
            "https://api.polygon.io/v2/aggs/ticker/%s/range/1/day/%s/%s?sort=asc&adjusted=true",
            fixedSymbol, from.toString(), to.toString());
    logger.atInfo().log("GET %s", url);
    HttpResponse<JsonNode> response =
        Unirest.get(url)
            .header("Authorization", "Bearer " + Config.get("POLYGON_API_KEY").orElse("misisng"))
            .asObject(JsonNode.class);

    if (response.isSuccess()) {
      return ImmutableBarSeries.of(toBars(response.getBody()));
    } else {
      if (response.getStatus() == 403) {
        String message = response.getBody().path("message").asText();
        if (message.toLowerCase().contains("timeframe")) {
          ImmutableBarSeries.of(List.of());
        }
      }

      throw new RuntimeException("status=" + response.getStatus());
    }
  }

  public BarSeries loadAll() {
    List<Bar> allResults = Lists.newArrayList();
    LocalDate t1 = LocalDate.now();
    LocalDate t0 = t1.minus(1000, ChronoUnit.DAYS);
    List<Bar> results = List.of();
    do {

      BarSeries page = fetch(t0, t1);

      allResults.addAll(Bars.toList(page));
      t1 = t0;
      t0 = t1.minus(1000, ChronoUnit.DAYS);

    } while (!results.isEmpty());

    return ImmutableBarSeries.of(allResults.stream().sorted().toList(), this.getSymbol().getName());
  }
}
