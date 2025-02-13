package bq.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Symbol {

  String qualifier;
  String ticker;

  static Set<String> validQualifiers = Set.of("S", "I", "Q", "X");

  public String getTicker() {
    return ticker;
  }

  public Optional<String> getQualifier() {
    return S.notBlank(qualifier);
  }

  public boolean isIndex() {
    return getQualifier().orElse("").equals("I");
  }

  public boolean isStock() {
    return getQualifier().orElse("").equals("S");
  }

  public boolean isCrypto() {
    return getQualifier().orElse("").equals("X");
  }

  public boolean isIndicator() {
    return getQualifier().orElse("").equals("Q");
  }

  static Symbol parseTableName(final String input) {
    String s = input;
    if (S.isBlank(s)) {
      throw new IllegalArgumentException("unable to parse symbol: " + input);
    }
    if (s.toLowerCase().endsWith(".csv")) {
      s = s.substring(0, s.length() - 4);
    }

    String qualifier = null;
    List<String> parts = Lists.newArrayList(Splitter.on("_").splitToList(s.toUpperCase()));
    if (parts.size() > 1) {
      qualifier = parts.removeFirst();
      if (!validQualifiers.contains(qualifier)) {
        throw new IllegalArgumentException("invalid qualifier: " + input);
      }
    }

    Symbol symbol = new Symbol();
    symbol.qualifier = qualifier;
    symbol.ticker = Joiner.on(".").join(parts);
    return symbol;
  }

  public static Symbol parse(String input) {
    if (S.isBlank(input)) {
      throw new IllegalArgumentException("invalid symbol: " + input);
    }

    Symbol s = new Symbol();
    List<String> parts = Splitter.on(":").trimResults().splitToList(input);
    if (parts.size() == 1) {
      s.ticker = parts.get(0).toUpperCase();
      return s;
    } else if (parts.size() == 2) {

      s.qualifier = parts.get(0).toUpperCase();
      if (!validQualifiers.contains(s.qualifier)) {
        throw new IllegalArgumentException("invalid qualfiier: " + s.qualifier);
      }
      s.ticker = parts.get(1).toUpperCase();
      if (S.isBlank(s.qualifier)) {
        throw new IllegalArgumentException("invalid symbol: " + input);
      }
      if (S.isBlank(s.ticker)) {
        throw new IllegalArgumentException("invalid symbol: " + input);
      }
      return s;
    }

    throw new IllegalArgumentException("invalid symbol: " + input);
  }

  public String getPathName() {
    if (getQualifier().isEmpty()) {
      return getTicker().toUpperCase();
    }
    return String.format("%s_%s", getQualifier().get().toUpperCase(), getTicker().toUpperCase());
  }

  private String getTypeName() {
    if (isCrypto()) {
      return "crypto";
    }
    if (isStock()) {
      return "stocks";
    }
    if (isIndicator()) {
      return "indicators";
    }
    if (isIndex()) {
      return "indices";
    }
    throw new IllegalStateException("unknown symbol type");
  }

  public String getName() {
    if (S.isBlank(qualifier)) {
      return ticker;
    } else {
      return String.format("%s:%s", qualifier, ticker);
    }
  }

  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  @Override
  public boolean equals(Object obj) {

    if (obj != null && obj instanceof Symbol && ((Symbol) obj).getName().equals(getName())) {
      return true;
    }
    return false;
  }

  public String toString() {
    return getName();
  }
}
