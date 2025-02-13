package bq.ducktape;

import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.flogger.FluentLogger;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Indicator;
import org.ta4j.core.indicators.helpers.BooleanTransformIndicator;
import org.ta4j.core.indicators.helpers.CombineIndicator;
import org.ta4j.core.indicators.helpers.ConstantIndicator;
import org.ta4j.core.indicators.helpers.FixedBooleanIndicator;
import org.ta4j.core.indicators.helpers.FixedDecimalIndicator;
import org.ta4j.core.indicators.helpers.FixedIndicator;
import org.ta4j.core.indicators.helpers.NumIndicator;
import org.ta4j.core.indicators.helpers.SumIndicator;
import org.ta4j.core.indicators.helpers.TransformIndicator;
import org.ta4j.core.indicators.pivotpoints.TimeLevel;
import org.ta4j.core.num.Num;

public class IndicatorRegistry {

  static FluentLogger logger = FluentLogger.forEnclosingClass();
  static IndicatorRegistry singleton;

  private AtomicReference<Map<String, Class<? extends Indicator<?>>>> allIndicators =
      new AtomicReference<>(Map.of());

  private Supplier<Map<String, Class<? extends Indicator<?>>>> availableSupplier =
      Suppliers.memoize(this::filterAvailableIndicators);

  private Set<Class> blacklist = buildBlacklist();

  public Map<String, Class<? extends Indicator<?>>> getAvailableIndicators() {
    return availableSupplier.get();
  }

  private Map<String, Class<? extends Indicator<?>>> filterAvailableIndicators() {

    Map<String, Class<? extends Indicator<?>>> filtered = Maps.newHashMap();

    allIndicators
        .get()
        .forEach(
            (k, v) -> {
              if (isAvailable(v)) {
                filtered.put(k, v);
              }
            });

    return Map.copyOf(filtered);
  }

  public static synchronized IndicatorRegistry getRegistry() {
    if (singleton == null) {
      singleton = new IndicatorRegistry();
      singleton.reload();
    }
    return singleton;
  }

  private void reload() {
    ScanResult result =
        new ClassGraph()
            .verbose(false)
            .enableAllInfo()
            .acceptPackages("bq", "org.ta4j.core.indicators")
            .scan();

    Map<String, Class<? extends Indicator<?>>> map = Maps.newHashMap();
    result
        .getClassesImplementing(Indicator.class)
        .forEach(
            classInfo -> {
              try {
                String className = classInfo.getName();
                Class x = (Class<? extends Indicator<?>>) Class.forName(className);

                if (x.isInterface()
                    || Modifier.isAbstract(x.getModifiers())
                    || x.getName().contains("$")
                    || !hasPublicConstructor(x)) {
                  logger.atFiner().log("ignoring interface/abstract/inner class: %s", x);
                } else {

                  ;
                  map.put(toFunctionName(x), x);
                }

              } catch (RuntimeException | ClassNotFoundException e) {
                logger.atFine().withCause(e).log();
              }
            });

    this.allIndicators.set(Map.copyOf(map));
  }

  public Map<String, Class<? extends Indicator<?>>> getAllIndicators() {
    return this.allIndicators.get();
  }

  public String toFunctionName(Class x) {
    String simple = x.getSimpleName();

    simple = simple.replace("Indicator", "");

    char[] array = simple.toCharArray();
    StringBuffer sb = new StringBuffer();

    for (int i = 0; i < array.length; i++) {
      char c = array[i];
      if (i == 0) {
        sb.append(Character.toLowerCase(c));
      } else {
        char prev = array[i - 1];
        if (Character.isLowerCase(prev) && Character.isUpperCase(c)) {
          sb.append("_");
        }
        sb.append(Character.toLowerCase(c));
      }
    }
    return sb.toString();
  }

  public Optional<Class<? extends Indicator<?>>> getIndicatorClass(String name) {
    Class<? extends Indicator<?>> x = this.getAvailableIndicators().get(name);
    return Optional.ofNullable(x);
  }

  private boolean hasPublicConstructor(Class c) {

    for (Constructor ctor : c.getDeclaredConstructors()) {

      if (Modifier.isPublic(ctor.getModifiers())) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private void scanClassInfo(io.github.classgraph.ClassInfo info) {}

  static boolean hasStandardArgs(Class clazz) {
    for (Constructor ctor : clazz.getDeclaredConstructors()) {
      if (hasStandardArgs(ctor) == true) {
        return true;
      }
    }
    return false;
  }

  static boolean hasNonStandardArgs(Class clazz) {
    for (Constructor ctor : clazz.getDeclaredConstructors()) {
      if (hasStandardArgs(ctor) == false) {
        return true;
      }
    }
    return false;
  }

  boolean isAvailable(Class<? extends Indicator<?>> indicator) {
    if (!hasStandardArgs(indicator)) {
      return false;
    }
    if (blacklist.contains(indicator)) {
      return false;
    }

    return true;
  }

  static boolean hasStandardArgs(Constructor ctor) {
    Class[] ptypes = ctor.getParameterTypes();

    Set<Class> standardTypes =
        Set.of(
            BarSeries.class,
            int.class,
            double.class,
            boolean.class,
            Number.class,
            TimeLevel.class,
            Double.class,
            Boolean.class,
            Num.class,
            Indicator.class);
    for (int i = 0; i < ptypes.length; i++) {
      if (!standardTypes.contains(ptypes[i])) {

        return false;
      }
    }
    return true;
  }

  private Set<Class> buildBlacklist() {
    Set<Class> blacklist = Sets.newHashSet();

    // There is nothing wrong with these indicators, but they can only be used programmatically.
    blacklist.add(FixedDecimalIndicator.class);
    blacklist.add(TransformIndicator.class);
    blacklist.add(FixedIndicator.class);
    blacklist.add(FixedDecimalIndicator.class);
    blacklist.add(CombineIndicator.class);
    blacklist.add(NumIndicator.class);
    blacklist.add(ConstantIndicator.class);
    blacklist.add(SumIndicator.class);
    blacklist.add(FixedBooleanIndicator.class);
    blacklist.add(BooleanTransformIndicator.class);
    return Set.copyOf(blacklist);
  }
}
