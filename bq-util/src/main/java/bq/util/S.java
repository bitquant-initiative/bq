package bq.util;

import com.google.common.base.Preconditions;
import java.util.Optional;

public class S {

  public static boolean isNull(String input) {
    return notNull(input).isEmpty();
  }

  public static boolean isNotNull(String input) {
    return !isNull(input);
  }

  public static boolean isBlank(String input) {
    return notBlank(input).isEmpty();
  }

  public static boolean isNotBlank(String input) {
    return !isBlank(input);
  }

  public static boolean isEmpty(String input) {
    return notEmpty(input).isEmpty();
  }

  public static boolean isNotEmpty(String input) {
    return !isEmpty(input);
  }

  public static Optional<String> notNull(String input) {

    return input != null ? Optional.of(input) : Optional.empty();
  }

  public static String repeat(String s, int count) {
    StringBuffer sb = new StringBuffer();
    if (s == null) {
      throw new IllegalArgumentException();
    }
    for (int i = 0; i < count; i++) {
      sb.append(s);
    }
    return sb.toString();
  }

  public static String lpad(String input, int len, String pad) {
    Preconditions.checkArgument(
        pad != null && pad.length() > 0, "padding must not be null or empty");
    String val = input;
    if (val == null) {
      val = "";
    }

    int padLen = len - val.length();
    if (padLen > 0) {
      String padding = "";
      while (padding.length() < padLen) {
        padding = padding + pad;
      }
      padding = padding.substring(0, padLen);
      val = padding + val;
    }
    return val;
  }

  public static String rpad(String input, int len, String pad) {
    Preconditions.checkArgument(
        pad != null && pad.length() > 0, "padding must not be null or empty");
    String val = input;
    if (val == null) {
      val = "";
    }

    int padLen = len - val.length();
    if (padLen > 0) {
      String padding = "";
      while (padding.length() < padLen) {
        padding = padding + pad;
      }
      padding = padding.substring(0, padLen);
      val = val + padding;
    }
    return val;
  }

  public static String repeat(char s, int count) {
    StringBuffer sb = new StringBuffer();

    for (int i = 0; i < count; i++) {
      sb.append(s);
    }
    return sb.toString();
  }

  public static String lpad(String input, int total) {
    return lpad(input, total, " ");
  }

  public static String rpad(String input, int total) {
    return rpad(input, total, " ");
  }

  public static Optional<String> notEmpty(String input) {

    if (input == null) {
      return Optional.empty();
    }
    if (input.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(input);
  }

  public static Optional<String> notBlank(String input) {

    if (input == null) {
      return Optional.empty();
    }
    if (input.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(input);
  }
}
