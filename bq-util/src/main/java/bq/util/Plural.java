package bq.util;

import java.util.Map;

public class Plural {

  static Map<String, String> pluralMap =
      Map.of(
          "man", "men", "woman", "women", "person", "people", "crypto", "crypto", "index",
          "indices");

  public static String toPlural(String singular) {
    String pluralName = pluralMap.get(singular.toLowerCase());
    if (pluralName != null) {
      return pluralName;
    }
    return String.format("%ss", singular);
  }

  public static String toCount(long count, String singular) {

    if (count == 1) {
      return String.format("%s %s", count, singular);
    }
    return String.format("%s %s", count, toPlural(singular));
  }
}
