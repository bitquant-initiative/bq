package bq.duckdb;

import bq.sql.DbException;
import com.google.common.flogger.FluentLogger;

public class S3Extension {

  FluentLogger logger = FluentLogger.forEnclosingClass();
  DuckDb duck;

  S3Extension(DuckDb db) {
    super();
    this.duck = db;
  }

  S3Extension loadExtension() {
    duck.template().execute("INSTALL aws");
    duck.template().execute("LOAD aws");
    return this;
  }

  public S3Extension useCredentialChain() {

    try {
      String sql =
          """
          CREATE SECRET ducktape_aws_secret (
              TYPE S3,
              PROVIDER CREDENTIAL_CHAIN
          )\
          """;

      duck.template().execute(sql);

    } catch (DbException e) {
      if (e.getMessage().toLowerCase().contains("already exists")) {
        logger.atFiner().log("credential chain already loaded");
        return this;
      }
      throw e;
    }
    return this;
  }
}
