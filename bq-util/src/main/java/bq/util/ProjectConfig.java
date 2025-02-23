package bq.util;

public class ProjectConfig {

  static ProjectConfig instance = new ProjectConfig();

  private static final String DEFAULT_BQ_S3_BUCKET_NAME = "test.bitquant.cloud";
  private static final String DEFAULT_BQ_S3_BUCKET_REGION = "us-west-2";

  private ProjectConfig() {}

  public static ProjectConfig get() {
    return instance;
  }

  public String getS3Bucket() {

    return Config.get("BQ_S3_BUCKET").orElse(DEFAULT_BQ_S3_BUCKET_NAME);
  }

  public String getS3BucketRegion() {
    return Config.get("BQ_S3_BUCKET_REGION").orElse(DEFAULT_BQ_S3_BUCKET_REGION);
  }
}
