package bq.indicator;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class DateDoubleMap {

  Map<LocalDate, Map<String, Double>> data = Maps.newHashMap();

  public void put(LocalDate d, String name, Double v) {
    Preconditions.checkArgument(d != null);
    Map<String, Double> kv = data.get(d);
    if (kv == null) {
      kv = Maps.newHashMap();
      data.put(d, kv);
    }
    kv.put(name, v);
  }

  public List<LocalDate> dates() {
    return data.keySet().stream().sorted().toList();
  }

  public void forEach(BiConsumer<LocalDate, Map<String, Double>> c) {
    data.keySet().stream()
        .sorted()
        .forEach(
            d -> {
              c.accept(d, data.get(d));
            });
  }

  public Map<String, Double> get(LocalDate d) {
    System.out.println(data);
    return data.get(d);
  }

  public Optional<Double> get(LocalDate d, String key) {
    Map<String, Double> vals = data.get(d);
    if (vals == null) {
      return Optional.empty();
    }

    return Optional.ofNullable(vals.get(key));
  }
}
