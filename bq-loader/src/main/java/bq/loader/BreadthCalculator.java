package bq.loader;

import bq.duckdb.DuckDb;
import bq.util.Config;
import bq.util.Json;
import bq.util.S;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.flogger.FluentLogger;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;

public class BreadthCalculator {

  static FluentLogger logger = FluentLogger.forEnclosingClass();
  DuckDb db;
  Set<String> symbols = Sets.newHashSet();
  LocalDate fromDate;
  LocalDate toDate;
  String table = "breadth";

  public BreadthCalculator from(LocalDate d) {
    this.fromDate = d;
    return this;
  }

  public BreadthCalculator to(LocalDate d) {
    this.toDate = d;
    return this;
  }

  public BreadthCalculator db(DuckDb db) {
    this.db = db;
    return this;
  }

  public BreadthCalculator includedSymbols(Collection<String> includedSymbols) {
    this.symbols = Set.copyOf(includedSymbols);
    return this;
  }

  public BreadthCalculator createTable() {
    String sql =
        """
        create table if not exists {{table}} (
          date date primary key,
          adv_count bigint,
          dec_count bigint,
          total_count bigint,
          adv_volume bigint,
          dec_volume bigint,
          total_volume bigint,
          ad bigint,
          cumulative_ad bigint
        )
        """;
    sql = sql.replace("{{table}}", table);
    db.template().execute(sql);
    return this;
  }

  public BreadthCalculator forNYSE() {
    return this.includedSymbols(getIndexComponents("NYSE"));
  }

  public List<String> getIndexComponents(String name) {
    String url =
        "https://s3.us-west-2.amazonaws.com/data.bitquant.cloud/metadata/index-components/%s.csv";
    url = String.format(url, name);

    String sql = String.format("select symbol from '%s'", url);

    return DuckDb.getSharedInMemory()
        .template()
        .query(
            c -> c.sql(sql),
            rs -> {
              return rs.getString(1).orElse(null);
            })
        .filter(s -> S.isNotBlank(s))
        .sorted()
        .toList();
  }

  JsonNode getAllStocks(LocalDate d) {

    String url =
        String.format(
            "https://api.polygon.io/v2/aggs/grouped/locale/us/market/stocks/%s?adjusted=true",
            d.toString());

    HttpResponse<byte[]> r =
        Unirest.get(url)
            .header("Authorization", "Bearer " + Config.get("POLYGON_API_KEY").orElse(""))
            .asBytes();

    return Json.readTree(r.getBody());
  }

  public BreadthCalculator fetchData() {

    Preconditions.checkNotNull(db, "db");
    Map<String, Double> priceMap = Maps.newHashMap();
    Map<String, Long> volumeMap = Maps.newHashMap();

    Map<String, Double> priorPriceMap = Maps.newHashMap();
    Map<String, Long> priorVolumeMap = Maps.newHashMap();

    LocalDate d = fromDate;
    while (!d.isAfter(toDate)) {

      var json = getAllStocks(d);

      if (json.path("results").size() > 100) {
        priorPriceMap.clear();
        priorPriceMap.putAll(priceMap);
        priorVolumeMap.clear();
        priorVolumeMap.putAll(volumeMap);

        priceMap.clear();
        volumeMap.clear();

        json.path("results")
            .forEach(
                it -> {
                  String symbol = it.path("T").asText();
                  if (this.symbols.contains(symbol)) {

                    double price = it.path("c").asDouble(-1);
                    long volume = it.path("v").asLong(-1);
                    if (price >= 0 && volume >= 0) {
                      priceMap.put(symbol, price);
                      volumeMap.put(symbol, volume);
                    }
                  }
                });

        int advancers = 0;
        int decliners = 0;
        int total = 0;
        long advancingVolume = 0;
        long decliningVolume = 0;
        long totalVolume = 0;
        for (String symbol : priceMap.keySet()) {
          Double price = priceMap.get(symbol);
          Double priorPrice = priorPriceMap.get(symbol);
          Long volume = volumeMap.get(symbol);
          if (price != null && priorPrice != null) {
            if (price > priorPrice) {
              advancers++;
              advancingVolume += volume;
            } else if (price < priorPrice) {
              decliners++;
              decliningVolume += volume;
            }
            total++;
            totalVolume += volume;
          }
        }
        logger.atInfo().log(
            "date=%s advancers=%s decliners=%s total=%s advancingVolume=%s decliningVolume=%s"
                + " totalVolume=%s",
            d, advancers, decliners, total, advancingVolume, decliningVolume, totalVolume);

        String sql =
            """
insert into ##table## (date,adv_count,dec_count,total_count,
adv_volume, dec_volume, total_volume
)
values ({{date}},{{adv_count}},{{dec_count}},{{total_count}},{{adv_volume}},{{dec_volume}},
{{total_volume}})
""";

        String fsql = sql;
        final LocalDate fd = d;
        final long fAdvancers = advancers;
        final long fDecliners = decliners;
        final long fTotal = total;
        final long fAdvancingVolume = advancingVolume;
        final long fDecliningVolume = decliningVolume;
        final long fTotalVolume = totalVolume;

        db.template()
            .update(
                c -> {
                  c.sql(fsql);
                  c.bind("table", table);
                  c.bind("date", fd);
                  c.bind("adv_count", fAdvancers);
                  c.bind("dec_count", fDecliners);
                  c.bind("total_count", fTotal);
                  c.bind("adv_volume", fAdvancingVolume);
                  c.bind("dec_volume", fDecliningVolume);
                  c.bind("total_volume", fTotalVolume);
                });
        // appender.beginRow();
        // appender.appendLocalDateTime(d.atStartOfDay());
        // appender.append(advancers);
        // appender.append(decliners);
        // appender.append(total);
        // appender.append(advancingVolume);
        // appender.append(decliningVolume);
        // appender.append(totalVolume);
        // appender.endRow();
      }
      d = d.plusDays(1);
    }
    recalculateAd();

    return this;
  }

  private BreadthCalculator recalculateAd() {
    db.template().execute("update " + table + " set ad=adv_count-dec_count");

    AtomicLong cumulativeAd = new AtomicLong();
    Map<Long, Long> vals = Maps.newHashMap();
    db.template()
        .query(
            c -> c.sql("select rowid,date,ad from " + table + " order by date"),
            rs -> {
              long ad = rs.getLong("ad").orElse(0l);
              long rowId = rs.getLong("rowid").get();
              long cumulative = cumulativeAd.addAndGet(ad);

              vals.put(rowId, cumulative);
              return "";
            });

    vals.forEach(
        (rowId, val) -> {
          db.table(table).update(rowId, "cumulative_ad", val);
        });

    return this;
  }

  public BreadthCalculator updateADLine() {

    db.template()
        .query(
            c -> c.sql("select date,ad from " + table + " order by date"),
            rs -> {
              System.out.println(rs.getLong("ad").get());
              return "";
            });

    return this;
  }
}
