package bq.util;

import com.google.common.base.MoreObjects;
import java.util.function.Supplier;

public class Item<T> implements Supplier<T> {

  long index;
  T item;

  protected Item(T t, long index) {
    this.item = t;
    this.index = index;
  }

  @Override
  public T get() {

    return item;
  }

  public long indexLong() {
    return index;
  }

  public int index() {
    if (index > Integer.MAX_VALUE || index < Integer.MIN_VALUE) {
      throw new IndexOutOfBoundsException("use indexLong()");
    }
    return (int) index;
  }

  public static <T> Item<T> of(T t, long index) {
    return new Item(t, index);
  }

  public String toString() {
    return MoreObjects.toStringHelper(this).add("index", index).add("item", item).toString();
  }
}
