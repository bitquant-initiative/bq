package bq.util;

import java.util.Set;

public class Mask {

  public static final String mask(String input) {

    if (input == null) {
      return "";
    }
    if (input.equalsIgnoreCase("")) {
      return "";
    }
    if (input.length() > 10) {
      return input.substring(0, 2) + "*****" + input.substring(input.length() - 2);
    }

    return "*****";
  }

  static final Set<String> sensitiveStrings =
      Set.of(
          "key",
          "passwd",
          "toKen",
          "password",
          "passphrase",
          "secret",
          "bearer",
          "clientid",
          "appid",
          "do_pat");

  public static String mask(String key, String val) {
    if (isSensitiveKey(key)) {
      return mask(val);
    }
    return val;
  }

  public static boolean isSensitiveKey(String k) {
    if (k == null) {
      return false;
    }

    return sensitiveStrings.stream()
        .map(s -> s.toLowerCase().trim().replace("-", "").replace("_", ""))
        .anyMatch(
            p ->
                S.notBlank(k)
                    .orElse("")
                    .toLowerCase()
                    .trim()
                    .replace("-", "")
                    .replace("_", "")
                    .contains(p));
  }
}
