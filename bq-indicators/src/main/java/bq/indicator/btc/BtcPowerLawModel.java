package bq.indicator.btc;

import static bq.indicator.btc.BtcPowerLawCalculator.calculateDate;

import bq.util.ClasspathResources;
import bq.util.Json;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.flogger.FluentLogger;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Supplier;
import org.ta4j.core.BarSeries;

public class BtcPowerLawModel {

  public static int DEFAULT_QUANTILE = 50;
  static FluentLogger logger = FluentLogger.forEnclosingClass();

  static Supplier<QuantileModel> supplier = Suppliers.memoize(BtcPowerLawModel::loadDefaultModel);

  QuantileModel quantileModel = null;
  int quantile = DEFAULT_QUANTILE;

  Double explicitC = null;
  Double explicitA = null;

  public static QuantileModel getDefaultModel() {
    return supplier.get();
  }

  public int getDaysToPrice(double price, int quantile) {
    checkQuantileRange(quantile);
    return getDaysToPrice(price, LocalDate.now(), quantile);
  }

  private void checkQuantileRange(int q) {
    Preconditions.checkArgument(q >= 0, "quantile (%s) must be >=0", q);
    Preconditions.checkArgument(q < 100, "quantile (%s) must be <100", q);
  }

  public int getDaysToPrice(double price, LocalDate refrenceDate, int quantile) {
    Preconditions.checkNotNull(refrenceDate);

    LocalDate dateAtPrice = getDate(price, quantile);

    return (int) ChronoUnit.DAYS.between(refrenceDate, dateAtPrice);
  }

  public int getDaysToPrice(double price) {

    return getDaysToPrice(price, LocalDate.now());
  }

  public int getDaysToPrice(double price, LocalDate d) {
    Preconditions.checkNotNull(d, "date cannot be null");

    LocalDate dateAtPrice = null;
    if (this.quantileModel != null) {
      dateAtPrice = getDate(price, getQuantile());

    } else {
      requireExplicitParameters();
      dateAtPrice = BtcPowerLawCalculator.calculateDate(price, explicitA, explicitC);
    }

    return (int) ChronoUnit.DAYS.between(d, dateAtPrice);
  }

  public LocalDate getDate(double price) {
    if (this.quantileModel != null) {
      return getDate(price, this.getQuantile());
    } else {
      requireExplicitParameters();
      return calculateDate(price, explicitA, explicitC);
    }
  }

  public LocalDate getDate(double price, int q) {
    requireQuantileModel();
    checkQuantileRange(q);
    double c = this.quantileModel.quantiles.get(q);
    return calculateDate(price, a(), c);
  }

  public BtcPowerLawModel quantile(int q) {
    checkQuantileRange(q);

    Preconditions.checkState(
        this.quantileModel != null, "cannot use quantiles if a quantile model is not set");
    this.quantile = q;
    return this;
  }

  private BtcPowerLawModel() {}

  public static BtcPowerLawModel create(double a, double c) {

    BtcPowerLawModel x = new BtcPowerLawModel();
    x.quantileModel = null;
    x.quantile = -1;

    x.explicitA = a;
    x.explicitC = c;

    return x;
  }

  public static BtcPowerLawModel create(BarSeries calibrationData, double a) {
    QuantileModel quantileModel = BtcPowerLawCalculator.generateQuantileModel(calibrationData, a);

    return create(quantileModel);
  }

  public static BtcPowerLawModel create(QuantileModel qm) {
    BtcPowerLawModel m = new BtcPowerLawModel();
    m.quantileModel = qm;
    return m;
  }

  public static BtcPowerLawModel create() {
    return create(supplier.get());
  }

  public double a() {

    if (quantileModel != null) {
      return quantileModel.a();
    } else {
      Preconditions.checkState(explicitA != null);
      return explicitA;
    }
  }

  public double c() {
    if (quantileModel != null) {
      return quantileModel.c(quantile);
    } else {
      Preconditions.checkState(explicitC != null);
      return explicitC;
    }
  }

  public static class QuantileModel {

    double a;
    List<Double> quantiles;

    public double a() {
      return a;
    }

    public QuantileModel(double a) {
      this.a = a;

      this.quantiles = Lists.newArrayList();

      for (int i = 0; i < 100; i++) {
        quantiles.add(null);
      }
      Preconditions.checkState(quantiles.size() == 100);
    }

    public double c(int q) {
      Preconditions.checkArgument(q >= 0, "q>=0; was %s", q);
      Preconditions.checkArgument(q <= 100, "q>=0; was %s", q);
      return quantiles.get(q);
    }

    public List<Double> getQuantiles() {
      return List.copyOf(this.quantiles);
    }

    public JsonNode toJson() {

      ObjectNode n = Json.createObjectNode();
      n.put("a", a());
      ArrayNode arr = Json.createArrayNode();
      n.set("quantiles", arr);
      for (int i = 0; i < this.quantiles.size(); i++) {
        ObjectNode qn = Json.createObjectNode();
        qn.put("q", i);
        qn.put("a", a());
        qn.put("c", quantiles.get(i));
        arr.add(qn);
      }

      return n;
    }

    public String toString() {
      return MoreObjects.toStringHelper("QuantileModel")
          .add("a", a())
          .add("q5", c(5))
          .add("q50", c(50))
          .add("q95", c(95))
          .toString();
    }
  }

  static QuantileModel toQuantileModel(JsonNode n) {

    Preconditions.checkArgument(n.has("a") && n.path("a").isNumber());
    Preconditions.checkArgument(n.has("quantiles") && n.path("quantiles").isArray());
    Preconditions.checkArgument(
        n.path("quantiles").size() == 100,
        "quantile size should be 100 was %s",
        n.path("quantiles").size());
    QuantileModel m = new QuantileModel(n.path("a").asDouble());

    List<Double> quantiles = Lists.newArrayList();
    n.path("quantiles")
        .forEach(
            it -> {
              double c = it.path("c").asDouble();
              Preconditions.checkArgument(c > 1);
              quantiles.add(c);
            });

    m.quantiles = List.copyOf(quantiles);

    return m;
  }

  private static QuantileModel loadDefaultModel() {

    JsonNode n = ClasspathResources.asJsonNode("/btc-power-law-model.json");

    return toQuantileModel(n);
  }

  public double getQuantile(LocalDate d, double price) {

    for (int q = 0; q < 100; q++) {
      double modelPrice = getPrice(d, q);

      if (modelPrice == price) {
        return q;
      }
      if (modelPrice > price) {

        if (q > 1) {

          double diff0 = Math.abs(price - getPrice(d, q - 1));
          double diff1 = Math.abs(price - modelPrice);
          if (diff0 < diff1) {
            return q - 1;
          }
        }
        return q;
      }
    }

    return quantileModel.getQuantiles().size() - 1;
  }

  public double getPrice(LocalDate d) {
    if (this.quantileModel != null) {
      return getPrice(d, this.quantile);
    } else {
      requireExplicitParameters();
      return BtcPowerLawCalculator.calculatePrice(d, explicitA, explicitC);
    }
  }

  private void requireExplicitParameters() {
    Preconditions.checkState(explicitA != null);
    Preconditions.checkState(explicitC != null);
    Preconditions.checkState(quantileModel == null);
  }

  private void requireQuantileModel() {
    Preconditions.checkState(this.quantileModel != null, "quantile model not set");
    Preconditions.checkState(explicitA == null);
    Preconditions.checkState(explicitC == null);
  }

  double c(int q) {
    requireQuantileModel();
    return this.quantileModel.getQuantiles().get(q);
  }

  public double getPrice(LocalDate d, int q) {
    double c = c(q);
    return BtcPowerLawCalculator.calculatePrice(BtcUtil.getDaysSinceGenesis(d), a(), c);
  }

  public int getQuantile() {
    return quantile;
  }

  public QuantileModel getModel() {
    requireQuantileModel();
    return this.quantileModel;
  }

  public String toString() {
    return MoreObjects.toStringHelper(this).add("model", this.quantileModel).toString();
  }
}
