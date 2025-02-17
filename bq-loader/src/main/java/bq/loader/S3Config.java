package bq.loader;

import bq.util.ProjectConfig;
import com.google.common.base.Suppliers;
import com.google.common.flogger.FluentLogger;
import java.util.function.Supplier;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class S3Config {

  static FluentLogger logger = FluentLogger.forEnclosingClass();
  static final Supplier<S3Client> supplier = Suppliers.memoize(S3Config::createClient);

  public static S3Client getClientx() {
    return supplier.get();
  }

  private static S3Client createClient() {

    return S3Client.builder().region(Region.of(ProjectConfig.get().getS3BucketRegion())).build();
  }
}
