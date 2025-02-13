package bq.util;

import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class JsonTest {

  @Test
  public void testArrayStream() {

    var arr = Json.createArrayNode();

    for (int i = 0; i < 100; i++) {
      arr.add(i);
    }

    AtomicInteger last = new AtomicInteger(-1);
    Json.stream(arr)
        .forEach(
            it -> {
              Assertions.assertThat(it.asInt()).isGreaterThan(last.intValue());
              last.set(it.asInt());
            });
  }

  @Test
  public void testObjectStream() {

    ObjectNode n = Json.createObjectNode();
    n.put("a", 1);
    n.put("b", 2);

    Assertions.assertThat(Json.stream(n).map(t -> t.intValue()).toList()).containsExactly(1, 2);
  }

  @Test
  public void testEmptyStreams() {
    Assertions.assertThat(Json.stream(Json.readTree("\"test\"")).count()).isEqualTo(0);
    Assertions.assertThat(Json.stream(Json.readTree("1")).count()).isEqualTo(0);
    Assertions.assertThat(Json.stream(NullNode.instance).count()).isEqualTo(0);
    Assertions.assertThat(Json.stream(MissingNode.getInstance()).count()).isEqualTo(0);
    Assertions.assertThat(Json.stream(null).count()).isEqualTo(0);
  }
}
