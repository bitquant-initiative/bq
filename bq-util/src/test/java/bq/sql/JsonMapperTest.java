package bq.sql;

import bq.sql.mapper.Mappers;
import bq.util.BqTest;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class JsonMapperTest extends BqTest {

  @Test
  public void testObjectNode() {

    var template = newTemplate();

    ObjectNode n =
        template
            .queryJson(
                c ->
                    c.sql(
                        "select 'Rosie' as name, 3 as age, true as likes_to_bark,"
                            + " now()::timestamptz as now "))
            .findFirst()
            .get();

    System.out.println(n);
    Assertions.assertThat(n.get("name").isTextual()).isTrue();
    Assertions.assertThat(n.get("name").asText()).isEqualTo("Rosie");
    Assertions.assertThat(n.get("age").getNodeType()).isEqualTo(JsonNodeType.NUMBER);
    Assertions.assertThat(n.get("age").asDouble()).isEqualTo(3);
    Assertions.assertThat(n.get("likes_to_bark").getNodeType()).isEqualTo(JsonNodeType.BOOLEAN);
    Assertions.assertThat(n.get("likes_to_bark").asBoolean()).isEqualTo(true);
  }

  @Test
  public void testArrayNode() {

    var template = newTemplate();

    ArrayNode n =
        template
            .query(
                c -> c.sql("select 'Rosie' as name, 3 as age, true as likes_to_bark "),
                Mappers.jsonArrayMapper())
            .findFirst()
            .get();

    Assertions.assertThat(n.get(0).isTextual()).isTrue();
    Assertions.assertThat(n.get(0).asText()).isEqualTo("Rosie");
    Assertions.assertThat(n.get(1).getNodeType()).isEqualTo(JsonNodeType.NUMBER);
    Assertions.assertThat(n.get(1).asDouble()).isEqualTo(3);
    Assertions.assertThat(n.get(2).getNodeType()).isEqualTo(JsonNodeType.BOOLEAN);
    Assertions.assertThat(n.get(2).asBoolean()).isEqualTo(true);
  }
}
