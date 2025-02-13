package bq.util;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class Items {

  public static <T> Stream<Item<T>> stream(Collection<T> input) {
    return stream(input, 0);
  }

  public static <T> Stream<Item<T>> stream(Iterable<T> input, int startIndex) {

    return list(input, startIndex).stream();
  }

  public static <T> List<Item<T>> list(Iterable<T> input) {
    return list(input, 0);
  }

  public static <T> List<Item<T>> list(Iterable<T> input, int startIndex) {
    if (input == null) {
      return List.of();
    }
    List<Item<T>> list = Lists.newArrayList();
    int index = startIndex;
    for (T t : input) {
      list.add(Item.of(t, index));
      index++;
    }
    return List.copyOf(list);
  }
}
