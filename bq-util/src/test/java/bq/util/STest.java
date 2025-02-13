package bq.util;

import com.google.common.collect.Lists;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class STest {

  @Test
  public void testNot() {

    Lists.newArrayList("hello", "", "  ", "\t", null)
        .forEach(
            it -> {
              Assertions.assertThat(S.isNotBlank(it)).isEqualTo(!S.isBlank(it));
              Assertions.assertThat(S.isNotEmpty(it)).isEqualTo(!S.isEmpty(it));
              Assertions.assertThat(S.isNotNull(it)).isEqualTo(!S.isNull(it));
            });
  }

  @Test
  public void testNotEmpty() {

    Assertions.assertThat(S.notEmpty("hello")).hasValue("hello");
    Assertions.assertThat(S.notEmpty("")).isEmpty();
    Assertions.assertThat(S.notEmpty(" ")).hasValue(" ");
    Assertions.assertThat(S.notEmpty(null)).isEmpty();

    Assertions.assertThat(S.isEmpty("x")).isFalse();
    Assertions.assertThat(S.isEmpty("")).isTrue();
    Assertions.assertThat(S.isEmpty(" ")).isFalse();
  }

  @Test
  public void testNotBlank() {

    Assertions.assertThat(S.notEmpty("hello")).hasValue("hello");
    Assertions.assertThat(S.notBlank("")).isEmpty();
    Assertions.assertThat(S.notBlank(" ")).isEmpty();
    Assertions.assertThat(S.notBlank(null)).isEmpty();

    Assertions.assertThat(S.isBlank("x")).isFalse();
    Assertions.assertThat(S.isBlank("")).isTrue();
    Assertions.assertThat(S.isBlank(" ")).isTrue();
  }

  @Test
  public void testNotNull() {

    Assertions.assertThat(S.notNull("hello")).hasValue("hello");
    Assertions.assertThat(S.notNull("")).hasValue("");
    Assertions.assertThat(S.notNull(" ")).hasValue(" ");
    Assertions.assertThat(S.notNull(null)).isEmpty();

    Assertions.assertThat(S.isNull("x")).isFalse();
    Assertions.assertThat(S.isNull("")).isFalse();
    Assertions.assertThat(S.isNull(" ")).isFalse();
  }

  @SuppressWarnings("unused")
  private void checkThrown(Class clazz, Runnable r) {

    try {
      r.run();
      Assertions.failBecauseExceptionWasNotThrown(clazz);
    } catch (Exception e) {
      Assertions.assertThat(e.getClass().isAssignableFrom(clazz)).isTrue();
    }
  }

  @Test
  public void testlpad() {
    Assertions.assertThat(S.lpad("foo", 0)).isEqualTo("foo");
    Assertions.assertThat(S.lpad("foo", 3)).isEqualTo("foo");
    Assertions.assertThat(S.lpad("foo", 5)).isEqualTo("  foo");
    Assertions.assertThat(S.lpad("foo", 7, "x")).isEqualTo("xxxxfoo");
    Assertions.assertThat(S.lpad("foo", 7, "1234567")).isEqualTo("1234foo");

    checkThrown(
        IllegalArgumentException.class,
        () -> {
          S.lpad("foo", 7, "");
        });
  }

  @Test
  public void testRPad() {
    Assertions.assertThat(S.rpad("foo", 0)).isEqualTo("foo");
    Assertions.assertThat(S.rpad("foo", 3)).isEqualTo("foo");
    Assertions.assertThat(S.rpad("foo", 5)).isEqualTo("foo  ");
    Assertions.assertThat(S.rpad("foo", 7, "x")).isEqualTo("fooxxxx");
    Assertions.assertThat(S.rpad("foo", 7, "1234567")).isEqualTo("foo1234");
  }

  @Test
  public void testRepat() {

    checkThrown(
        IllegalArgumentException.class,
        () -> {
          S.repeat(null, 4);
        });

    Assertions.assertThat(S.repeat("", 4)).isEqualTo("");
  }
}
