package bq.options;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import java.time.Duration;

public abstract class OptionModel {

  OptionType type;
  Double yearsToExpiry;
  Double stockPrice;
  Double strikePrice;
  Double riskFreeRate;
  Double dividendYield = 0d;
  Double volatility;
  Double optionPrice;

  public abstract OptionModel copy();

  protected void copyAttributes(OptionModel target) {
    target.type = this.type;
    target.yearsToExpiry = this.yearsToExpiry;
    target.stockPrice = this.stockPrice;
    target.strikePrice = this.strikePrice;
    target.riskFreeRate = this.riskFreeRate;
    target.dividendYield = this.dividendYield;
    target.volatility = this.volatility;
    target.optionPrice = this.optionPrice;
  }

  public OptionModel expiration(Duration d) {

    double days = d.toDays();
    this.yearsToExpiry = days / 365d;
    return this;
  }

  public OptionModel optionPrice(double d) {
    this.optionPrice = d;
    return this;
  }

  public Double optionPrice() {
    return this.optionPrice;
  }

  public OptionModel type(OptionType type) {
    Preconditions.checkNotNull(type, "option type not specified");
    this.type = type;
    return this;
  }

  public OptionModel yte(double yte) {
    return expiration(yte);
  }

  public OptionModel expiration(double yearsToExpiry) {
    this.yearsToExpiry = yearsToExpiry;
    return this;
  }

  public OptionModel stockPrice(double d) {
    this.stockPrice = d;
    return this;
  }

  public OptionModel strikePrice(double d) {
    this.strikePrice = d;
    return this;
  }

  public OptionModel rate(double d) {
    this.riskFreeRate = d;
    return this;
  }

  public OptionModel volatility(double d) {
    this.volatility = d;
    return this;
  }

  public OptionModel dividendYield(double d) {
    this.dividendYield = d;
    return this;
  }

  public OptionType type() {
    return type;
  }

  public Double volatility() {
    return volatility;
  }

  public Double dividendYield() {
    return dividendYield;
  }

  public Double rate() {
    return riskFreeRate;
  }

  public Double strikePrice() {
    return strikePrice;
  }

  public Double stockPrice() {
    return stockPrice;
  }

  public Double expiration() {
    return yearsToExpiry;
  }

  public Double dte() {
    return Math.round(yearsToExpiry * 365d * 100d) / 100d;
  }

  public abstract Double calcIV();

  public abstract Double calcOptionPrice();

  public abstract Double delta();

  public abstract Double gamma();

  public abstract Double rho();

  public abstract Double theta();

  public abstract Double vega();

  protected Double roundGreek(double d) {
    if (Double.isFinite(d)) {
      return Math.round(d * 1000d) / 1000d;
    }
    return d;
  }

  protected Double roundPrice(double d) {
    if (Double.isFinite(d)) {
      return Math.round(d * 100d) / 100d;
    }
    return d;
  }

  public String toString() {
    ToStringHelper h = MoreObjects.toStringHelper(this);
    h.add("type", type);
    h.add("dte", dte());
    if (type != null
        && stockPrice != null
        && strikePrice != null
        && riskFreeRate != null
        && this.yearsToExpiry != null
        && this.volatility != null
        && this.dividendYield != null) {
      h.add("optionPrice", roundPrice(calcOptionPrice()));
      h.add("delta", roundGreek(delta()));
      h.add("theta", roundGreek(theta()));
      h.add("gamma", gamma());
    }

    h.add("stock", roundPrice(stockPrice));
    h.add("strike", roundPrice(strikePrice));
    h.add("rate", riskFreeRate);

    h.add("divYield", dividendYield);

    return h.toString();
  }

  protected void checkIVCalcPrams() {
    Preconditions.checkNotNull(type(), "option type must be specified");
    Preconditions.checkNotNull(optionPrice(), "option price must be specified");
    Preconditions.checkNotNull(stockPrice(), "stock price must be specified");
    Preconditions.checkNotNull(strikePrice(), "strike price must be specified");
    Preconditions.checkNotNull(rate(), "risk-free rate must be specified");
    Preconditions.checkNotNull(expiration(), "expiration  must be specified");
    Preconditions.checkNotNull(volatility(), "volatility must be specified");
    Preconditions.checkNotNull(dividendYield(), "volatility must be specified");
  }

  protected void checkPriceCalcParams() {
    Preconditions.checkNotNull(type(), "option type must be specified");
    Preconditions.checkNotNull(stockPrice(), "stock price must be specified");
    Preconditions.checkNotNull(strikePrice(), "strike price must be specified");
    Preconditions.checkNotNull(rate(), "risk-free rate must be specified");
    Preconditions.checkNotNull(expiration(), "expiration  must be specified");
    Preconditions.checkNotNull(dividendYield(), "volatility must be specified");
    Preconditions.checkNotNull(volatility(), "volatility must be specified");
  }
}
