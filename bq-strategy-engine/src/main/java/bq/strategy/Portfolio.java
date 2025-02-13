package bq.strategy;

import bq.duckdb.DuckDb;
import bq.util.BqException;
import bq.util.S;
import bq.util.Symbol;
import bq.util.Zones;
import bq.util.ta4j.ImmutableBar;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.flogger.FluentLogger;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.ta4j.core.Bar;

/**
 * Portfolio is a simple single-asset portfolio which can contain an asset and cash.  It is NOT intended to model
 * a portfolio with multiple assets.  Multiple assets are out of scope.
 *
 */
public class Portfolio {

  static FluentLogger logger = FluentLogger.forEnclosingClass();
  // totally OK with double precision loss...this is not a ledger or payments
  // platform

  DuckDb duck;
  StrategyEngine engine;

  private long rowId = -1;
  private double cash = 0;
  private Double initialValue = null;
  private double assetQty = 0;
  private double assetPrice = 0;
  private double assetUnitCostBasis = 0d;

  String assetSymbol = null;

  double txnQty = 0;
  double txnPrice = 0;

  LocalDate date = LocalDate.now(Zones.UTC);

  JsonNode data;

  public void createTable(String tableName) {

    String sql =
        """
        create table {{table}} (
          date date primary key,
          symbol varchar(12),
          cash double NOT NULL default(0),

        )
        """;
  }

  public static Portfolio create(DuckDb d) {
    Portfolio p = new Portfolio();
    p.duck = d;
    return p;
  }

  public Portfolio recordInitialValue() {
    this.initialValue = getPortfolioValue();
    return this;
  }

  public Portfolio snapshot() {
    Portfolio p = new Portfolio();
    p.cash = cash;
    p.assetQty = assetQty;
    p.assetPrice = assetPrice;
    p.assetSymbol = assetSymbol;
    p.assetUnitCostBasis = assetUnitCostBasis;
    p.initialValue = initialValue;
    p.date = date;

    p.txnPrice = txnPrice;
    p.txnQty = txnQty;

    return p;
  }

  public double getCash() {
    return cash;
  }

  public LocalDate getDate() {
    return date;
  }

  public String getAssetSymbol() {
    return assetSymbol;
  }

  public double getAssetQty() {
    return assetQty;
  }

  public double getAssetPrice() {
    return this.assetPrice;
  }

  public double getAssetValue() {
    return getAssetQty() * getAssetPrice();
  }

  public long getInputRowId() {
    return rowId;
  }

  public double getPortfolioValue() {
    return getAssetValue() + getCash();
  }

  public double getPortfolioInitialValue() {
    return this.initialValue;
  }

  public void buy(final double requestedQty) {

    double beforePurchaseQty = this.assetQty;
    double qty = requestedQty;
    double cashValue = qty * getAssetPrice();
    if (!fractionalSharesAllowed()) {
      qty = Math.floor(qty);
    }
    if (cashValue - getCash() > .01) {

      throw new BqException(
          String.format("insufficient buying power (%s > %s", cashValue, getCash()));
    }
    if (qty < 0) {
      throw new BqException("qty cannot be negative");
    }

    this.cash = this.cash - cashValue;
    this.assetQty += qty;

    this.txnQty = qty;
    this.txnPrice = assetPrice;

    if (Double.isNaN(this.assetQty) || Double.isInfinite(this.assetQty)) {
      this.assetUnitCostBasis = 0d;

    } else {
      this.assetUnitCostBasis =
          ((beforePurchaseQty * getAssetCostBasis()) + (Math.abs(requestedQty) * this.assetPrice))
              / this.assetQty;
    }
  }

  public void sell(double requestedQty) {
    double qty = requestedQty;
    double cashValue = qty * getAssetPrice();

    if (qty < 0) {
      throw new BqException("invalid qty to sell: " + qty);
    }
    if (!fractionalSharesAllowed()) {
      qty = Math.floor(qty);
    }
    if (qty > getAssetQty()) {
      throw new BqException("requested amount " + requestedQty + " exceeds portfolio");
    }
    if (qty < 0) {
      throw new BqException("qty cannot be negative");
    }

    this.assetQty -= Math.abs(requestedQty);

    this.txnQty -= Math.abs(requestedQty);
    this.txnPrice = assetPrice;

    this.cash += Math.abs(cashValue);
  }

  public boolean fractionalSharesAllowed() {
    if (assetSymbol != null && assetSymbol.startsWith("X:")) {
      return true;
    }
    return false;
  }

  public double getAssetAllocation() {

    double v = 0;
    if (getPortfolioValue() < 1.0) {
      getAssetValue();
    }
    v = (getAssetValue() / Math.max(getPortfolioValue(), 1.d)) * 100;

    return v;
  }

  public void rebalance(double targetAllocation) {
    if (assetPrice <= 0) {
      return;
    }

    Preconditions.checkArgument(
        targetAllocation >= 0 && targetAllocation <= 100,
        "target allocation must be between 0-100");

    double targetAssetValue = getPortfolioValue() * (targetAllocation / 100);
    double targetQty = targetAssetValue / getAssetPrice();
    if (!fractionalSharesAllowed()) {
      targetQty = Math.floor(targetQty);
    }

    double rebalancePct = targetAllocation - getAssetAllocation();

    double qtyToTransact = Math.abs(targetQty - getAssetQty());

    if (Math.abs(rebalancePct) < 1.0) {
      logger.atFiner().log("rebalance (%s%%) is too small", round(rebalancePct, 1, 0));
      return;
    }

    // double txnCashValue = Math.abs(qtyToTransact * getAssetPrice());

    if (targetQty > getAssetQty()) {

      buy(Math.abs(qtyToTransact));
    } else if (targetQty < getAssetQty()) {

      sell(Math.abs(qtyToTransact));
    }
  }

  public Portfolio assetSymbol(String symbol) {

    this.assetSymbol = symbol;
    return this;
  }

  public Portfolio cash(double d) {
    this.cash = d;
    return this;
  }

  public double getAssetCostBasis() {

    return this.assetUnitCostBasis;
  }

  public Portfolio assetPrice(double d) {
    this.assetPrice = d;
    return this;
  }

  public Portfolio assetQty(double d) {
    this.assetQty = d;
    return this;
  }

  public String getUnitName() {
    if (S.isBlank(assetSymbol)) {
      return "shares";
    }
    return Symbol.parse(assetSymbol).getTicker();
  }

  Portfolio assetUnitCostBasis(double d) {
    this.assetUnitCostBasis = d;
    return this;
  }

  String roundCurrency(double d) {
    return round(d, 2, 2);
  }

  String roundQty(double d) {
    if (assetSymbol == null) {
      return Double.toString(d);
    } else if (assetSymbol.startsWith("X:")) {
      return round(d, 8, 0);

    } else {
      return round(d, 2, 0);
    }
  }

  public double getPortfolioProfit() {
    return getPortfolioValue() - getPortfolioInitialValue();
  }

  String roundUnitPrice(double d) {
    if (assetSymbol == null) {
      return round(d, 2, 2);
    }
    if (assetSymbol.startsWith("X:")) {
      return round(d, 8, 2);
    }
    return round(d, 2, 2);
  }

  String round(double d, int scale, int min) {

    String val = new BigDecimal(d).setScale(scale, RoundingMode.HALF_UP).toPlainString();

    List<String> parts = Splitter.on(".").splitToList(val);
    String wholePart = parts.get(0);
    String decimalPart = "";
    if (parts.size() == 2) {
      decimalPart = parts.get(1);
    }

    while (decimalPart.endsWith("0") && decimalPart.length() > min) {
      decimalPart = decimalPart.substring(0, decimalPart.length() - 1);
    }
    while (decimalPart.length() < min) {
      decimalPart = decimalPart + "0";
    }
    return String.format("%s%s%s", wholePart, decimalPart.length() > 0 ? "." : "", decimalPart);
  }

  boolean isFractionalAllowed() {
    if (this.assetSymbol == null) {
      return false;
    }
    if (this.assetSymbol.startsWith("X:")) {
      return true;
    }
    return false;
  }

  public Portfolio assetCostBasis(double d) {
    this.assetUnitCostBasis = d;
    return this;
  }

  public String toString() {

    ToStringHelper h = MoreObjects.toStringHelper("Portfolio");

    h.add("date", date);
    h.add("value", roundCurrency(getPortfolioValue()));
    h.add("cash", roundCurrency(getCash()));
    h.add("symbol", getAssetSymbol());
    h.add("assetQty", roundQty(getAssetQty()));
    h.add("assetPrice", roundUnitPrice(getAssetPrice()));
    h.add("assetCostBasis", roundUnitPrice(getAssetCostBasis()));
    h.add("assetValue", roundCurrency(getAssetValue()));
    h.add("assetAllocation", round(getAssetAllocation(), 1, 1));
    return h.toString();
  }

  public JsonNode getData() {
    return data;
  }

  public Optional<Double> getDouble(String name) {
    if (!data.has(name)) {
      return Optional.empty();
    }
    return Optional.of(data.path(name).asDouble());
  }

  public Portfolio next(Bar bar, JsonNode n) {

    ImmutableBar ib = (ImmutableBar) bar;
    txnPrice = 0;
    txnQty = 0;
    this.rowId = ib.getId().get();
    this.date = bar.getBeginTime().toLocalDate();
    this.assetPrice = bar.getClosePrice().doubleValue();
    this.data = n;
    if (initialValue == null) {
      this.initialValue = getPortfolioValue();
    }
    return this;
  }
}
