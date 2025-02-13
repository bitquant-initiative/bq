package bq.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Suppliers;
import com.google.common.flogger.FluentLogger;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;

public class Config {

  static final YAMLMapper yamlMapper = new YAMLMapper();
  static FluentLogger logger = FluentLogger.forEnclosingClass();

  static Supplier<JsonNode> configSupplier = Suppliers.memoize(Config::loadConfig);

  public static File getConfigDir() {
    return new File(System.getProperty("user.home"), ".bq");
  }

  public static File getConfigFile() {
    return new File(getConfigDir(), "config.yml");
  }

  public JsonNode getConfig() {
    return loadConfig();
  }

  private static JsonNode loadConfig() {
    File cfg = getConfigFile();
    if (cfg.exists()) {
      try {
        return yamlMapper.readTree(cfg);
      } catch (IOException e) {
        throw new BqException(e);
      }
    }
    return NullNode.getInstance();
  }

  public static Optional<String> get(String key) {

    if (S.isBlank(key)) {
      return Optional.empty();
    }

    String envVal = System.getenv(key);
    if (S.isNotBlank(envVal)) {
      return Optional.of(envVal);
    }

    Optional<String> val = tryGet(key, new File(System.getProperty("user.home"), ".bq/config.yml"));
    if (val.isPresent()) {
      return val;
    }

    return val;
  }

  static Optional<String> tryGet(String key, File f) {
    if (f == null || !f.exists()) {
      return Optional.empty();
    }

    try {

      JsonNode n = yamlMapper.readTree(f);

      String val = n.path(key).asText(null);
      if (val != null) {
        return Optional.of(val);
      }

      return Optional.empty();
    } catch (IOException e) {

      throw new BqException(e);
    }
  }
}
