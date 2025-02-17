package bq.duckdb;

import bq.util.ProjectConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class S3ExtensionTest {

  @Test
  @Disabled
  public void testIt() {

    DuckDb d = DuckDb.createInMemory();

    S3Extension s3 = d.s3Extension().useCredentialChain();
    S3Extension s3x = d.s3Extension().useCredentialChain();
    s3.useCredentialChain();
    s3.useCredentialChain();

    d.template().execute("create table test (name varchar(20), age int)");

    DuckTable table = d.table("test");

    d.table("test")
        .append(
            c -> {
              c.beginRow();
              c.append("jim");
              c.append(63);
              c.endRow();

              c.beginRow();
              c.append("kate");
              c.append(49);
              c.endRow();
            });

    d.template()
        .execute(
            "COPY test TO 's3://"
                + ProjectConfig.get().getS3Bucket()
                + "/test/output.csv' (HEADER, DELIMITER ',')");

    var x =
        d.template()
            .query(
                cb -> {
                  cb.sql(
                      "select * from 's3://data.bitquant.cloud/data/daily/S_IREN.csv'"
                          + " where close=3");
                },
                rs -> {
                  return "";
                });
  }
}
