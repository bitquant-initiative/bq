package bq.ducktape.chart;

import bq.util.Json;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class JSUtilTest {

  @Test
  public void testIt() {

    ObjectNode n = Json.createObjectNode();

    n.put("name", "Homer");
    n.put("age", 7);

    ArrayNode arr = Json.createArrayNode();

    n.set("arr", arr);

    arr.add("bark");
    arr.add("bark");

    n.set("activity", arr);

    String expected =
        """
{
    name: "Homer",
    age: 7,
    arr: [ "bark", "bark" ],
    activity: [ "bark", "bark" ],
}\
""";
    Assertions.assertThat(JSUtil.toObject(n)).isEqualTo(expected);
  }
}
