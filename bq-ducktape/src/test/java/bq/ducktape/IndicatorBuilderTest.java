package bq.ducktape;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.flogger.FluentLogger;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.ChopIndicator;
import org.ta4j.core.indicators.FisherIndicator;
import org.ta4j.core.indicators.KAMAIndicator;
import org.ta4j.core.indicators.KSTIndicator;
import org.ta4j.core.indicators.KalmanFilterIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.PVOIndicator;
import org.ta4j.core.indicators.ParabolicSarIndicator;
import org.ta4j.core.indicators.RAVIIndicator;
import org.ta4j.core.indicators.RecentSwingLowIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.SqueezeProIndicator;
import org.ta4j.core.indicators.bollinger.PercentBIndicator;
import org.ta4j.core.indicators.candles.DojiIndicator;
import org.ta4j.core.indicators.candles.HammerIndicator;
import org.ta4j.core.indicators.candles.ThreeWhiteSoldiersIndicator;
import org.ta4j.core.indicators.helpers.CrossIndicator;
import org.ta4j.core.indicators.helpers.DifferencePercentageIndicator;
import org.ta4j.core.indicators.keltner.KeltnerChannelMiddleIndicator;
import org.ta4j.core.indicators.pivotpoints.DeMarkPivotPointIndicator;
import org.ta4j.core.indicators.statistics.CovarianceIndicator;
import org.ta4j.core.indicators.supertrend.SuperTrendIndicator;

@TestInstance(Lifecycle.PER_CLASS)
@SuppressWarnings("rawtypes")
public class IndicatorBuilderTest {

  static DuckTape tape;
  static BarSeriesTable btc;

  static FluentLogger logger = FluentLogger.forEnclosingClass();

  Map<String, Set<Class>> signatureMap = Maps.newHashMap();

  Set<Class> indicatorsWithStandardArgs = Sets.newHashSet();
  Set<Class> indicatorsWithNonStandardArgs = Sets.newHashSet();
  Set<Class> indicatorsWithOnlyNonStandardArgs = Sets.newHashSet();

  Set<Class> successfullyTested = Sets.newHashSet();
  Set<Constructor> successfullyTestedConstructors = Sets.newHashSet();
  Set<String> successfullyTestedSignature = Sets.newHashSet();
  Set<String> unsupportedSignatures = Sets.newHashSet();

  AtomicInteger checkCount = new AtomicInteger();

  @Test
  public void testIt() {
    check(SMAIndicator.class, "20");
    check(MACDIndicator.class);
    check(MACDIndicator.class, "close");

    // Constructor is overloaded between Num and Number, which prevents one
    // of the two from being used. Not a problem, but ends up being a false
    // positive in the report.
    check(DifferencePercentageIndicator.class, "12.3");
    check(DifferencePercentageIndicator.class, "open", "12.3");
    check(CovarianceIndicator.class, "col_a", "col_b", "12");

    check(PercentBIndicator.class, "1", "2.1");
    check(PercentBIndicator.class, "col", "1", "2.1");
    check(RAVIIndicator.class, "1", "2");
    check(RAVIIndicator.class, "col", "1", "2");

    check(KAMAIndicator.class, "col", "1", "2", "3");
    check(KAMAIndicator.class, "1", "2", "3");

    check(KSTIndicator.class);
    check(KSTIndicator.class, "col");
    check(KSTIndicator.class, "col", "1", "2", "3", "4", "5", "6", "7", "8");
    check(KSTIndicator.class, "1", "2", "3", "4", "5", "6", "7", "8");

    check(CrossIndicator.class, "foo", "bar");

    // KalmanFilter is strange in that some logic is in the constructor, so the
    // column needs to exist
    check(KalmanFilterIndicator.class, "close", "1.1", "1.2");
    check(KalmanFilterIndicator.class, "1.1", "1.2");
    check(FisherIndicator.class, "10", "true");
    Indicator t = check(FisherIndicator.class, "10", "12.3", "13.4");

    check(FisherIndicator.class, "col", "10", "12.3", "13.4");

    check(KeltnerChannelMiddleIndicator.class, "3");
    check(DifferencePercentageIndicator.class, "col", "12.3");
    check(DifferencePercentageIndicator.class, "12.3");

    check(SqueezeProIndicator.class, "1", "2.1", "2.2", "3.3", "3.4");
    check(PVOIndicator.class, "1", "2", "3");
    check(ThreeWhiteSoldiersIndicator.class, "1", "2.2");
    check(SuperTrendIndicator.class, "1", "2.2");
    // BarSeries
    check(RecentSwingLowIndicator.class, "12");
    check(HammerIndicator.class);
    check(ChopIndicator.class, "1", "2");
    check(DojiIndicator.class, "1", "2.2");
    check(HammerIndicator.class, "1", "2.2");

    check(ParabolicSarIndicator.class, "1", "2.2");
    check(ParabolicSarIndicator.class, "1", "2.2", "3");

    check(DeMarkPivotPointIndicator.class, "DAY");

    check(FisherIndicator.class, "col", "1", "2.1", "2.2", "2.3", "2.4");

    // [interface org.ta4j.core.Indicator, int, double, double, double, double,
    // double, boolean]
    check(FisherIndicator.class, "1", "2.2", "2.3", "2.4", "2.5", "2.6", "true");
    checkFailure(CovarianceIndicator.class, "col", "10");

    // check(DistanceFromMAIndicator.class, "test"); // this is broken as
    // implemented need an alternate
  }

  @Test
  public void testSma() {
    SMAIndicator sma = IndicatorBuilder.newBuilder().expression("sma(12)").table(btc).build();
  }

  @Test
  public void testFisher() {

    List<Constructor> ctors = Lists.newArrayList(FisherIndicator.class.getDeclaredConstructors());

    ctors = IndicatorBuilder.sortByPrecdence(ctors);

    boolean seenBoolean = false;
    for (Constructor c : ctors) {
      boolean isBoolean =
          Lists.newArrayList(c.getParameters()).toString().toLowerCase().contains("boolean");
      if (isBoolean) {
        seenBoolean = true;
      }
      if (seenBoolean) {
        Assertions.assertThat(isBoolean).isTrue();
      }
    }
  }

  void checkFailure(Class indicatorClass, String... args) {
    try {
      IndicatorBuilder.newBuilder().indicator(indicatorClass).table(btc).args(args).build();
      Assertions.failBecauseExceptionWasNotThrown(RuntimeException.class);
    } catch (RuntimeException e) {

    }
  }

  Indicator check(Class indicatorClass, String... args) {

    if (args == null || (args.length == 1 && args[0] == null)) {
      args = new String[0];
    }

    return check(null, indicatorClass, args);
  }

  static String toSignature(Constructor c) {
    return Lists.newArrayList(c.getParameterTypes()).toString();
  }

  Indicator check(String expectedToString, Class indicatorClass, String... args) {
    checkCount.incrementAndGet();

    logger.atInfo().log("checking " + indicatorClass.getSimpleName());

    IndicatorBuilder b =
        IndicatorBuilder.newBuilder().table(btc).indicator(indicatorClass).args(args);

    Indicator c = b.build();
    Assertions.assertThat(c != null);
    Assertions.assertThat(indicatorClass.equals(c.getClass()));

    Constructor usedConstructor = b.getConstructorUsed();
    this.successfullyTestedConstructors.add(usedConstructor);
    this.successfullyTestedSignature.add(toSignature(usedConstructor));
    if (expectedToString != null) {
      Assertions.assertThat(c.toString()).isEqualTo(expectedToString);
    }
    successfullyTested.add(indicatorClass);

    return c;
  }

  @BeforeAll
  public void inventoryAll() {

    IndicatorRegistry registry = IndicatorRegistry.getRegistry();

    registry
        .getAvailableIndicators()
        .values()
        .forEach(
            c -> {
              for (Constructor ctor : c.getDeclaredConstructors()) {
                if (Modifier.isPublic(ctor.getModifiers())) {
                  List<Class<? extends Indicator>> sig =
                      Lists.newArrayList(ctor.getParameterTypes());
                  String signature = sig.toString();

                  Set<Class> classes = signatureMap.get(signature);
                  if (classes == null) {
                    classes = Sets.newHashSet();
                    signatureMap.put(signature, classes);
                  }
                  classes.add(c);
                }
              }
            });

    signatureMap.keySet().stream()
        .sorted()
        .forEach(
            signature -> {

              //    System.out.println(signatureMap.get(signature).size() + " " +
              // signature + " " + signatureMap.get(signature));
            });

    registry.getAllIndicators().values().stream()
        .filter(IndicatorRegistry::hasStandardArgs)
        .forEach(
            c -> {
              indicatorsWithStandardArgs.add(c);
            });

    registry.getAllIndicators().values().stream()
        .filter(IndicatorRegistry::hasNonStandardArgs)
        .forEach(
            c -> {
              indicatorsWithNonStandardArgs.add(c);
            });

    indicatorsWithOnlyNonStandardArgs =
        com.google.common.collect.Sets.difference(
            indicatorsWithNonStandardArgs, indicatorsWithStandardArgs);

    indicatorsWithOnlyNonStandardArgs.forEach(
        it -> {
          // logger.atInfo().log("non-standard: " + it);
        });

    unsupportedSignatures.add(
        "[interface org.ta4j.core.Indicator, interface java.util.function.Predicate]");
    unsupportedSignatures.add(
        "[class org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator, class"
            + " org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator, class"
            + " org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator]");
    unsupportedSignatures.add(
        "[interface org.ta4j.core.BarSeries, class org.ta4j.core.indicators.ATRIndicator,"
            + " class java.lang.Double]");
    unsupportedSignatures.add(
        "[interface org.ta4j.core.Indicator, interface org.ta4j.core.Indicator, interface"
            + " java.util.function.BinaryOperator]");
    unsupportedSignatures.add(
        "[interface org.ta4j.core.BarSeries, class"
            + " org.ta4j.core.indicators.ichimoku.IchimokuTenkanSenIndicator, class"
            + " org.ta4j.core.indicators.ichimoku.IchimokuKijunSenIndicator, int]");
    unsupportedSignatures.add("[class org.ta4j.core.indicators.RSIIndicator, int]");
    unsupportedSignatures.add(
        "[class org.ta4j.core.indicators.pivotpoints.PivotPointIndicator, class"
            + " org.ta4j.core.indicators.pivotpoints.FibonacciReversalIndicator$FibonacciFactor,"
            + " class"
            + " org.ta4j.core.indicators.pivotpoints.FibonacciReversalIndicator$FibReversalTyp]");
    unsupportedSignatures.add(
        "[interface org.ta4j.core.Indicator, int, class"
            + " org.ta4j.core.indicators.statistics.SimpleLinearRegressionIndicator$SimpleLinearRegressionType]");
    unsupportedSignatures.add("[interface org.ta4j.core.BarSeries, class [Ljava.lang.Boolean;]");
    unsupportedSignatures.add(
        "[interface org.ta4j.core.BarSeries, interface java.util.function.Function]");
    unsupportedSignatures.add(
        "[interface org.ta4j.core.Indicator, interface java.util.function.UnaryOperator]");
    unsupportedSignatures.add(
        "[interface org.ta4j.core.Indicator, int, class"
            + " org.ta4j.core.indicators.helpers.HighPriceIndicator, class"
            + " org.ta4j.core.indicators.helpers.LowPriceIndicator]");
    unsupportedSignatures.add("[class org.ta4j.core.indicators.helpers.TRIndicator, int]");
    unsupportedSignatures.add(
        "[class org.ta4j.core.indicators.keltner.KeltnerChannelMiddleIndicator, class"
            + " org.ta4j.core.indicators.ATRIndicator, double]");
    unsupportedSignatures.add("[interface org.ta4j.core.Indicator, class java.lang.Number]");
    unsupportedSignatures.add(
        "[class org.ta4j.core.indicators.keltner.KeltnerChannelMiddleIndicator, double," + " int]");
    unsupportedSignatures.add(
        "[class org.ta4j.core.indicators.helpers.ClosePriceIndicator, int, class"
            + " org.ta4j.core.indicators.helpers.HighPriceIndicator, class"
            + " org.ta4j.core.indicators.helpers.LowPriceIndicator]");
    unsupportedSignatures.add(
        "[interface org.ta4j.core.Indicator, interface org.ta4j.core.Indicator, int, class"
            + " org.ta4j.core.indicators.helpers.ConvergenceDivergenceIndicator$ConvergenceDivergenceType,"
            + " double, double]");
    unsupportedSignatures.add("[class [Lorg.ta4j.core.Indicator;]");
    unsupportedSignatures.add(
        "[class org.ta4j.core.indicators.pivotpoints.PivotPointIndicator, double, class"
            + " org.ta4j.core.indicators.pivotpoints.FibonacciReversalIndicator$FibReversalTyp]");
    unsupportedSignatures.add("[class org.ta4j.core.indicators.StochasticOscillatorKIndicator]");
    unsupportedSignatures.add(
        "[class org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator, interface"
            + " org.ta4j.core.Indicator]");
    unsupportedSignatures.add(
        "[class org.ta4j.core.indicators.pivotpoints.PivotPointIndicator, class"
            + " org.ta4j.core.indicators.pivotpoints.PivotLevel]");
    unsupportedSignatures.add("[interface org.ta4j.core.Indicator, int, double, double]");
    unsupportedSignatures.add("[interface org.ta4j.core.BarSeries, class [Ljava.lang.String;]");
    unsupportedSignatures.add("[interface org.ta4j.core.BarSeries, class java.lang.Object]");
    unsupportedSignatures.add(
        "[interface org.ta4j.core.BarSeries, interface org.ta4j.core.Indicator]");
    unsupportedSignatures.add(
        "[class org.ta4j.core.indicators.pivotpoints.DeMarkPivotPointIndicator, class"
            + " org.ta4j.core.indicators.pivotpoints.DeMarkReversalIndicator$DeMarkPivotLevel]");
    unsupportedSignatures.add(
        "[interface org.ta4j.core.Indicator, interface org.ta4j.core.Indicator, int, class"
            + " org.ta4j.core.indicators.helpers.ConvergenceDivergenceIndicator$ConvergenceDivergenceType]");
    unsupportedSignatures.add("[interface org.ta4j.core.BarSeries, class [Ljava.lang.Object;]");
    unsupportedSignatures.add(
        "[class org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator, interface"
            + " org.ta4j.core.Indicator, interface org.ta4j.core.num.Num]");
    unsupportedSignatures.add("[interface org.ta4j.core.BarSeries, class [D]");
    unsupportedSignatures.add("[class org.ta4j.core.indicators.volume.VWAPIndicator, int]");
    unsupportedSignatures.add(
        "[interface org.ta4j.core.Indicator, interface org.ta4j.core.Indicator, int, class"
            + " org.ta4j.core.indicators.helpers.ConvergenceDivergenceIndicator$ConvergenceDivergenceStrictType]");

    logger.atInfo().log(
        "indicators w/o standard constructors: %s", indicatorsWithOnlyNonStandardArgs.size());
    logger.atInfo().log(
        "total indicators: %s", IndicatorRegistry.getRegistry().getAllIndicators().size());
  }

  @AfterAll
  private void report() {

    logger.atInfo().log("successfully tested indicators: " + successfullyTested.size());
    logger.atInfo().log("successfully tested signatures: " + successfullyTestedSignature.size());
    logger.atInfo().log(
        "successfully tested constructors: " + successfullyTestedConstructors.size());

    // only run and enforce the checks if enough have been run
    if (checkCount.get() < 35) {
      return;
    }

    Set<String> untestedSignatures =
        com.google.common.collect.Sets.difference(
            signatureMap.keySet(), successfullyTestedSignature);

    Assertions.assertThat(
            com.google.common.collect.Sets.difference(untestedSignatures, unsupportedSignatures))
        .isEmpty();
  }

  @BeforeAll
  private static void setup() {
    tape = DuckTape.createInMemory();
    btc = tape.importTable("BTC", new File("./src/test/resources/data/btc.csv"));
  }

  @AfterAll
  private void cleanup() {
    if (tape != null) {
      tape.close();
    }
  }
}
