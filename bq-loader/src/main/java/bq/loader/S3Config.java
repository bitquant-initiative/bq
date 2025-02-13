package bq.loader;

import bq.util.Config;
import bq.util.RuntimeEnvironment;
import bq.util.S;
import com.google.common.base.Suppliers;
import com.google.common.flogger.FluentLogger;
import java.util.function.Supplier;
import software.amazon.awssdk.services.s3.S3Client;

public class S3Config {

  static FluentLogger logger = FluentLogger.forEnclosingClass();
  static final Supplier<S3Client> supplier = Suppliers.memoize(S3Config::createClient);

  public static S3Client getClient() {
    return supplier.get();
  }

  public static String getBucket() {

    String bucket = Config.get("S3_BUCKET").orElse(null);

    if (S.isBlank(bucket)) {

      if (RuntimeEnvironment.get().isSourceEnvironment()) {
        bucket = "test.bitquant.cloud";
      }
    }
    if (S.isBlank(bucket)) {
      bucket = "data.bitquant.cloud";
    }

    return bucket;
  }

  private static S3Client createClient() {
    return S3Client.create();
  }
}
