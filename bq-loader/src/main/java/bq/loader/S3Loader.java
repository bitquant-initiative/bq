package bq.loader;

import bq.duckdb.DuckDb;
import bq.ducktape.BarSeriesTable;
import bq.ducktape.DuckTape;
import bq.util.ProjectConfig;
import bq.util.S;
import bq.util.Symbol;
import bq.util.ta4j.ImmutableBarSeries;
import com.google.common.base.Splitter;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.flogger.FluentLogger;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.ta4j.core.BarSeries;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectAttributesResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectAttributes;
import software.amazon.awssdk.services.s3.model.S3Object;

public class S3Loader extends Loader<S3Loader> {

  FluentLogger logger = FluentLogger.forEnclosingClass();
  static Map<String, Boolean> s3AvailabilityMap = Maps.newHashMap();

  String bucket;

  static final Supplier<S3Client> supplier = Suppliers.memoize(S3Loader::createClient);

  public S3Loader(DuckDb d) {
    super(d);
  }

  @Override
  public BarSeries loadAll() {

    String tempTableName = "temp_" + System.currentTimeMillis();
    try {
      db.s3Extension().useCredentialChain();

      String s3Url = getS3UrlForSymbol();
      logger.atInfo().log("loading %s from %s", getSymbol(), s3Url);

      if (!s3FileExists()) {
        logger.atInfo().log("not found: %s", getS3UrlForSymbol());

        return ImmutableBarSeries.of(List.of(), getSymbol().getName());
      }

      String sql = String.format("CREATE TABLE %s AS SELECT * FROM '%s'", tempTableName, s3Url);

      logger.atFine().log("%s", sql);
      getDb().template().execute(sql);

      BarSeries bs = getDuckTape().getTable(tempTableName, symbol.getName()).getBarSeries();

      return bs;
    } finally {
      if (S.isNotBlank(tempTableName)) {
        getDb().template().execute("drop table if exists " + tempTableName);
      }
    }
  }

  public String getBucket() {
    if (S.isNotBlank(bucket)) {
      return bucket;
    }
    return ProjectConfig.get().getS3Bucket();
  }

  String getS3UrlForSymbol() {
    return String.format("s3://%s/%s", getBucket(), s3Key(getSymbol()));
  }

  boolean s3FileExists() {
    try {
      GetObjectAttributesResponse r =
          getClient()
              .getObjectAttributes(
                  c -> {
                    c.bucket(getBucket());
                    c.key(s3Key(symbol));
                    c.objectAttributes(ObjectAttributes.OBJECT_SIZE);
                  });
      return true;
    } catch (NoSuchKeyException e) {
      return false;
    }
  }

  public void writeS3(BarSeries data) {

    // for consistency, we're going to write the bar series to DuckDB in a temp table,
    // then have DuckDb send it to S3
    BarSeriesTable t = null;

    try {
      DuckTape tape = getDuckTape();
      String tempTable = String.format("temp_%s", System.currentTimeMillis());

      t = tape.createOHLCVTable(tempTable);
      tape.appendAll(t, data);

      getDb().template().log().query("select * from " + tempTable);
      String s3Url = getS3UrlForSymbol();

      db.s3Extension().useCredentialChain();

      String sql =
          String.format(
              "COPY (select date,open,high,low,close,volume from %s order by date asc) to '%s'",
              tempTable, getS3UrlForSymbol());

      logger.atInfo().log("%s", sql);
      getDb().template().execute(sql);
    } finally {
      if (t != null) {
        logger.atInfo().log("dropping temp table: %s", t);
        getDb().template().execute("drop table if exists " + t.getTableName());
      }
    }
  }

  public boolean isS3Available() {
    Boolean b = s3AvailabilityMap.get(getBucket());
    if (b != null) {
      return b;
    }

    Symbol checkSymbol = Symbol.parse("Q:CHECK.S3." + System.currentTimeMillis());
    String url = String.format("s3://%s/%s", getBucket(), s3Key(checkSymbol));

    try {
      logger.atInfo().log("checking S3 write access to %s", url);
      getClient();

    } catch (RuntimeException e) {
      logger.atInfo().log("could not access s3: " + e.toString());
      s3AvailabilityMap.put(getBucket(), false);
      return false;
    }
    try {
      getClient()
          .putObject(
              x -> {
                x.bucket(getBucket());
                x.key(s3Key(checkSymbol));
              },
              RequestBody.fromBytes("date,open,high,low,close,volume\n".getBytes()));

    } catch (RuntimeException e) {
      logger.atInfo().log("WRITE failed to %s", url);
      s3AvailabilityMap.put(getBucket(), false);
      return false;
    }

    try {
      getClient()
          .getObject(
              x -> {
                x.bucket(getBucket());
                x.key(s3Key(checkSymbol));
              });
    } catch (RuntimeException e) {
      logger.atInfo().log("READ failed to %s", url);
      s3AvailabilityMap.put(getBucket(), false);
      return false;
    }

    try {
      getClient()
          .deleteObject(
              x -> {
                x.bucket(getBucket());
                x.key(s3Key(checkSymbol));
              });
    } catch (RuntimeException e) {
      logger.atInfo().log("READ failed to %s", url);
      s3AvailabilityMap.put(getBucket(), false);
      return false;
    }

    return true;
  }

  @Override
  public BarSeries fetch(LocalDate from, LocalDate to) {
    return ImmutableBarSeries.empty();
  }

  List<Symbol> fetchSymbolsForUpdate() {

    var s3 = getClient();

    List<S3Object> list = Lists.newArrayList();
    ListObjectsV2Response response = null;

    AtomicReference<String> token = new AtomicReference<String>();
    do {
      response =
          s3.listObjectsV2(
              c -> {
                c.bucket(ProjectConfig.get().getS3Bucket());
                c.continuationToken(token.get());
              });
      list.addAll(response.contents());
      token.set(response.nextContinuationToken());
    } while (response.isTruncated());

    return list.stream()
        .filter(S3Loader::filterObjects)
        .sorted(byLastModified())
        .flatMap(m -> extractSymbol(m.key()).stream())
        .toList();
  }

  public static boolean filterObjects(S3Object obj) {
    String key = obj.key();
    if (key == null) {
      return false;
    }
    if (!key.endsWith(".csv")) {
      return false;
    }
    if (!key.contains("/1d/")) {
      return false;
    }

    boolean hasPrefix =
        key.startsWith("stocks/") || key.startsWith("indexes/") || key.startsWith("crypto/");
    if (!hasPrefix) {
      return false;
    }
    return true;
  }

  public static Optional<Symbol> extractSymbol(String path) {
    if (path == null) {
      return Optional.empty();
    }
    if (path.endsWith(".csv")) {
      path = path.substring(0, path.length() - 4);
    } else {
      return Optional.empty();
    }

    List<String> parts = Splitter.on("/").splitToList(path);

    if (parts.size() != 3) {
      return Optional.empty();
    }

    if (!parts.get(1).equals("1d")) {
      return Optional.empty();
    }

    String prefix = null;
    if (parts.get(0).equals("stocks")) {
      prefix = "S";
    }
    if (parts.get(0).equals("indices")) {
      prefix = "I";
    }
    if (parts.get(0).equals("crypto")) {
      prefix = "X";
    }
    if (prefix == null) {
      return Optional.empty();
    }
    String combined = String.format("%s:%s", prefix, parts.get(2));

    try {
      Symbol s = Symbol.parse(combined);
      return Optional.of(s);
    } catch (RuntimeException e) {

    }
    return Optional.empty();
  }

  static String s3Key(Symbol symbol) {
    Map<String, String> map =
        Map.of("X", "crypto", "S", "stocks", "I", "indices", "Q", "indicators");
    String type = map.get(symbol.getQualifier().orElse("S"));
    return String.format("%s/1d/%s.csv", type, symbol.getTicker());
  }

  static Comparator<S3Object> byLastModified() {
    Comparator<S3Object> x =
        new Comparator<S3Object>() {

          @Override
          public int compare(S3Object o1, S3Object o2) {
            return o1.lastModified().compareTo(o2.lastModified());
          }
        };
    return x;
  }

  public static S3Client getClient() {
    return supplier.get();
  }

  private static S3Client createClient() {

    return S3Client.builder().region(Region.of(ProjectConfig.get().getS3BucketRegion())).build();
  }
}
